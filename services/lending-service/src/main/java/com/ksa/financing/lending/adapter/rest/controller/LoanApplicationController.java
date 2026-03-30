package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.adapter.rest.request.*;
import com.ksa.financing.lending.adapter.rest.response.ApplicationTrackerResponse;
import com.ksa.financing.lending.adapter.rest.response.BankAccountInfoResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanApplicationResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanApplicationStepInfo;
import com.ksa.financing.lending.adapter.rest.response.StepSignalResponse;
import com.ksa.financing.lending.application.mapper.LoanApplicationMapper;
import com.ksa.financing.lending.application.usecase.BankAccountLookupService;
import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.port.in.ManageLoanApplicationUseCase;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for loan applications.
 * Initiates Temporal workflows and sends signals at each UI stepper step.
 * Signal/query endpoints use customerId as path variable and resolve workflowId from DB.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/loan-applications")
@Tag(name = "Loan Applications", description = "Loan application workflow endpoints (signal-driven)")
public class LoanApplicationController {

    private final ManageLoanApplicationUseCase useCase;
    private final com.ksa.financing.lending.domain.port.in.ManageLoanUseCase loanUseCase;
    private final LoanApplicationMapper mapper;
    private final WorkflowClient workflowClient;
    private final BankAccountLookupService bankAccountLookupService;
    private final com.ksa.financing.lending.domain.port.out.ProductConfigPort productConfigPort;

    @Value("${temporal.task-queue:loan-application-queue}")
    private String taskQueue;

    public LoanApplicationController(ManageLoanApplicationUseCase useCase,
                                      com.ksa.financing.lending.domain.port.in.ManageLoanUseCase loanUseCase,
                                      LoanApplicationMapper mapper,
                                      WorkflowClient workflowClient,
                                      BankAccountLookupService bankAccountLookupService,
                                      com.ksa.financing.lending.domain.port.out.ProductConfigPort productConfigPort) {
        this.useCase = useCase;
        this.loanUseCase = loanUseCase;
        this.mapper = mapper;
        this.workflowClient = workflowClient;
        this.bankAccountLookupService = bankAccountLookupService;
        this.productConfigPort = productConfigPort;
    }

    // ══════════════════════════════════════════════════════════════
    // INITIATE WORKFLOW (two modes: APPLY and ELIGIBILITY_CHECK)
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loan-applications", act = "create")
    @PostMapping("/initiate")
    @Operation(summary = "Initiate loan application (APPLY) or check eligibility (ELIGIBILITY_CHECK)")
    public ResponseEntity<InitiateApplicationResponse> initiateApplication(
            @Valid @RequestBody InitiateLoanApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);
        var answers = request.eligibilityAnswers() != null ? request.eligibilityAnswers() : Map.<String, String>of();

        // Validate required fields for APPLY mode
        if (request.requestedAmount() == null || request.requestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Requested amount is required");
        }
        if (request.requestedTenureMonths() <= 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Requested tenure is required");
        }

        // Validate against product + Fineract limits before starting workflow
        try {
            var config = productConfigPort.fetchProductConfig(
                    tenantId, request.productId(), request.requestedAmount(), request.requestedTenureMonths());
            var errors = new java.util.ArrayList<String>();
            if (!config.fineractLinked()) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST,
                        "Product '" + config.productName() + "' is not linked to any Fineract loan product. Please configure the product in Fineract first.");
            }
            if (config.minAmount() != null && request.requestedAmount().compareTo(config.minAmount()) < 0) {
                errors.add("Amount " + request.requestedAmount() + " SAR is below minimum " + config.minAmount() + " SAR");
            }
            if (config.maxAmount() != null && request.requestedAmount().compareTo(config.maxAmount()) > 0) {
                errors.add("Amount " + request.requestedAmount() + " SAR exceeds maximum " + config.maxAmount() + " SAR");
            }
            if (config.minTenureMonths() > 0 && request.requestedTenureMonths() < config.minTenureMonths()) {
                errors.add("Tenure " + request.requestedTenureMonths() + " months is below minimum " + config.minTenureMonths() + " months");
            }
            if (config.maxTenureMonths() > 0 && request.requestedTenureMonths() > config.maxTenureMonths()) {
                errors.add("Tenure " + request.requestedTenureMonths() + " months exceeds maximum " + config.maxTenureMonths() + " months");
            }
            if (!errors.isEmpty()) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST, String.join("; ", errors));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Product/Fineract validation unavailable, proceeding: {}", e.getMessage());
        }

        // Start Temporal workflow — all inter-service calls (eligibility, credit check,
        // product validation, etc.) happen inside workflow activities, NOT direct REST calls.
        var workflowId = "loan-app-" + tenantId + "-" + UUID.randomUUID();
        log.info("Initiating loan application workflow: {}, mode=APPLY", workflowId);

        var workflow = workflowClient.newWorkflowStub(
                LoanApplicationWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(taskQueue)
                        .setWorkflowExecutionTimeout(Duration.ofDays(7))
                        .build()
        );

        WorkflowClient.start(workflow::execute, new InitiateRequest(
                tenantId.toString(),
                request.customerId(),
                request.nationalId(),
                request.mobileNumber(),
                userId.toString(),
                request.productId(),
                request.requestedAmount(),
                request.requestedTenureMonths(),
                request.purposeOfFinance(),
                request.purposeOfFinanceOther(),
                // Individual expense categories (BRD Section 3.3)
                request.foodGroceries(),
                request.utilities(),
                request.healthcare(),
                request.communication(),
                request.housingRent(),
                request.clothingEssentials(),
                request.education(),
                request.transportation(),
                answers
        ));

        log.info("Loan application workflow started: {}", workflowId);

        // Calculate offer/installment preview for the response
        Map<String, Object> preQualification = null;
        try {
            // Fetch product profit rate for calculation
            BigDecimal profitRate = new BigDecimal("0.0385"); // default (decimal form)
            try {
                var config = productConfigPort.fetchProductConfig(
                        tenantId, request.productId(), request.requestedAmount(), request.requestedTenureMonths());
                if (config.profitRate() != null) {
                    // baseProfitRate from product-service: 2.5 means 2.5%, convert to decimal 0.025
                    // config.profitRate() from parseProductConfig: if from API = 2.5, if default = 0.0385
                    BigDecimal rate = config.profitRate();
                    profitRate = rate.compareTo(BigDecimal.ONE) > 0 ? rate.movePointLeft(2) : rate;
                }
            } catch (Exception ignored) {}

            var calcResult = com.ksa.financing.lending.domain.service.FinanceCalculationService.calculate(
                    request.requestedAmount(),
                    profitRate,
                    null,
                    request.requestedTenureMonths(),
                    null, null
            );
            if (!calcResult.hasErrors()) {
                preQualification = new java.util.LinkedHashMap<>();
                preQualification.put("requestedAmount", request.requestedAmount());
                preQualification.put("tenureMonths", request.requestedTenureMonths());
                preQualification.put("monthlyInstallment", calcResult.monthlyInstallment());
                preQualification.put("totalPayable", calcResult.totalPayable());
                preQualification.put("totalProfit", calcResult.totalCostOfFinancing());
                preQualification.put("profitRate", calcResult.profitRate());
                preQualification.put("apr", calcResult.apr());
                preQualification.put("numInstallments", calcResult.numInstallments());
                preQualification.put("firstInstallmentDueDate", calcResult.firstInstallmentDueDate());
                preQualification.put("processingFee", calcResult.processingFee());
                preQualification.put("adminFee", calcResult.adminFee());
            }
        } catch (Exception e) {
            log.warn("Pre-qualification calculation failed, skipping: {}", e.getMessage());
        }

        // Poll workflow until applicationId is available (Step 1 auto-processing creates it)
        ApplicationStatusInfo statusInfo = null;
        for (int i = 0; i < 10; i++) {
            statusInfo = queryWorkflowStatusSafe(workflowId);
            if (statusInfo != null && statusInfo.applicationId() != null) {
                break;
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        var currentStatusStr = statusInfo != null ? statusInfo.status() : "DRAFT";
        ApplicationStatus appStatus;
        try {
            appStatus = ApplicationStatus.valueOf(currentStatusStr);
        } catch (IllegalArgumentException e) {
            appStatus = ApplicationStatus.DRAFT;
        }
        var steps = LoanApplicationStepInfo.buildSteps(appStatus);
        var nextAction = LoanApplicationStepInfo.getNextAction(appStatus);

        return ResponseEntity.status(HttpStatus.CREATED).body(new InitiateApplicationResponse(
                workflowId,
                request.customerId(),
                statusInfo != null ? statusInfo.applicationId() : null,
                statusInfo != null ? statusInfo.applicationNumber() : null,
                currentStatusStr,
                "Loan application workflow initiated",
                nextAction,
                steps,
                preQualification,
                statusInfo != null ? new StepSignalResponse.WorkflowState(
                        statusInfo.stepperIndex(),
                        statusInfo.stepName(),
                        statusInfo.status(),
                        null, true, null,
                        statusInfo.applicationId(),
                        statusInfo.applicationNumber(),
                        null, null, null, null, null, null
                ) : null
        ));
    }

    public record InitiateApplicationResponse(
            String workflowId,
            String customerId,
            String applicationId,
            String applicationNumber,
            String status,
            String message,
            String nextAction,
            List<LoanApplicationStepInfo> steps,
            Map<String, Object> preQualification,
            StepSignalResponse.WorkflowState workflowState
    ) {}


    // ══════════════════════════════════════════════════════════════
    // SIGNAL ENDPOINTS (one per UI step) — use customerId path
    // Optional: ?applicationId=xxx for precise application targeting
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{customerId}/basic-info")
    @Operation(summary = "Step 1: Submit basic information (product, amount, purpose)")
    public ResponseEntity<StepSignalResponse> submitBasicInfo(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @Valid @RequestBody SubmitBasicInfoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        log.info("Signal: submitBasicInfo for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            workflow.submitBasicInfo(new BasicInfoSignal(
                    request.productId(),
                    null,  // productCode — resolved from product-service
                    null,  // productName — resolved from product-service
                    null,  // shariaStructure — resolved from product-service
                    request.requestedAmount(),
                    request.requestedTenureMonths(),
                    request.purposeOfFinance(),
                    null,  // profitRate — resolved from product-service
                    request.partnerId(),
                    request.leadId()
            ));
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired. Please start a new application.");
        }

        // Basic info triggers product + customer validation — poll until past DRAFT
        return ResponseEntity.ok(buildStepResponseWithWait("BASIC_INFO", workflowId, "DRAFT", 8));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{customerId}/bank-account")
    @Operation(summary = "Step 2: Submit bank account for disbursement")
    public ResponseEntity<StepSignalResponse> submitBankAccount(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @Valid @RequestBody SubmitBankAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        log.info("Signal: submitBankAccount for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            workflow.submitBankAccount(new BankAccountSignal(
                    request.bankCode(),
                    request.bankName(),
                    request.iban(),
                    request.accountNumber()
            ));
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired. Please start a new application.");
        }

        // Bank account triggers IBAN verification — poll until past BANK_ACCOUNT_PENDING
        return ResponseEntity.ok(buildStepResponseWithWait("BANK_ACCOUNT", workflowId, "BANK_ACCOUNT_PENDING", 8));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{customerId}/simah-consent")
    @Operation(summary = "Step 3: Give SIMAH consent for credit check")
    public ResponseEntity<StepSignalResponse> giveSimahConsent(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @Valid @RequestBody SimahConsentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        log.info("Signal: giveSimahConsent for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            workflow.giveSimahConsent(new SimahConsentSignal(request.consentGiven()));
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired. Please start a new application.");
        }

        // SIMAH consent triggers credit check → eligibility → offer calc — poll until past all intermediate states
        return ResponseEntity.ok(buildStepResponseWithWait("SIMAH_CONSENT", workflowId,
                List.of("SIMAH_CONSENT_GIVEN", "ELIGIBILITY_CHECKING"), 30));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{customerId}/accept-offer")
    @Operation(summary = "Step 4: Accept or reject the financing offer")
    public ResponseEntity<StepSignalResponse> acceptOffer(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @Valid @RequestBody AcceptOfferRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        log.info("Signal: acceptOffer for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            workflow.acceptOffer(new AcceptOfferSignal(
                    request.accepted(),
                    request.selectedAmount()
            ));
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired. Please start a new application.");
        }

        // Accept offer triggers recalc + contract generation — poll until past OFFER_PRESENTED
        return ResponseEntity.ok(buildStepResponseWithWait("ACCEPT_OFFER", workflowId, "OFFER_PRESENTED", 10));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{customerId}/sign-contract")
    @Operation(summary = "Step 5a: Sign contract with authorizations")
    public ResponseEntity<StepSignalResponse> signContract(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @Valid @RequestBody SignContractRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        log.info("Signal: signContract for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            workflow.signContract(new SignContractSignal(
                    request.authorizeDigitalSignature(),
                    request.authorizeSellCommodity(),
                    request.wantPhysicalDelivery()
            ));
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired. Please start a new application.");
        }

        // Sign contract triggers consent recording + OTP send — poll until past CONTRACT_SIGNING
        return ResponseEntity.ok(buildStepResponseWithWait("SIGN_CONTRACT", workflowId, "CONTRACT_SIGNING", 8));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{customerId}/verify-otp")
    @Operation(summary = "Step 5b: Verify OTP for contract signing")
    public ResponseEntity<StepSignalResponse> verifyOtp(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @Valid @RequestBody VerifySigningOtpRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        log.info("Signal: verifySigningOtp for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            workflow.verifySigningOtp(new OtpVerifySignal(request.otpCode()));
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired. Please start a new application.");
        }

        // OTP verification triggers IVR call initiation — poll until past OTP_VERIFICATION
        return ResponseEntity.ok(buildStepResponseWithWait("VERIFY_OTP", workflowId, "OTP_VERIFICATION", 8));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{customerId}/ivr-callback")
    @Operation(summary = "Step 5c: IVR call verification callback")
    public ResponseEntity<StepSignalResponse> ivrCallback(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @Valid @RequestBody IvrCallbackRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        log.info("Signal: ivrCallback for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            workflow.ivrCallback(new IvrCallbackSignal(
                    request.verified(),
                    request.callId(),
                    request.verificationStatus()
            ));
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired. Please start a new application.");
        }

        // IVR is the last user step — poll up to 15s for final APPROVED status with loan data
        return ResponseEntity.ok(buildStepResponseWithWait("IVR_CALLBACK", workflowId, (String) null, 15));
    }

    // ══════════════════════════════════════════════════════════════
    // QUERY ENDPOINTS (read workflow state) — use customerId path
    // Optional: ?applicationId=xxx for precise application targeting
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/{customerId}/step")
    @Operation(summary = "Query current step info from running workflow")
    public ResponseEntity<StepInfo> getCurrentStep(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        return ResponseEntity.ok(workflow.getCurrentStep());
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/{customerId}/status")
    @Operation(summary = "Query full application status from running workflow")
    public ResponseEntity<ApplicationStatusInfo> getWorkflowStatus(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        return ResponseEntity.ok(workflow.getApplicationStatus());
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/{customerId}/offer")
    @Operation(summary = "Query offer details from running workflow")
    public ResponseEntity<OfferDetails> getOfferDetails(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        return ResponseEntity.ok(workflow.getOfferDetails());
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/{customerId}/contract")
    @Operation(summary = "Query contract documents from running workflow")
    public ResponseEntity<ContractInfo> getContractDocuments(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        return ResponseEntity.ok(workflow.getContractDocuments());
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/{customerId}/bank-accounts")
    @Operation(summary = "Step 2 helper: Look up bank accounts (customer-service first, then Tarabut fallback)")
    public ResponseEntity<BankAccountInfoResponse> getBankAccounts(
            @PathVariable String customerId,
            @RequestParam String nationalId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Looking up bank accounts for customer: {}, NID: ***{}", customerId, nationalId.substring(nationalId.length() - 4));

        var result = bankAccountLookupService.lookupBankAccounts(customerId, nationalId, tenantId.toString(), jwt.getTokenValue());
        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════════════════════
    // DB-BASED READ ENDPOINTS (for completed/historical applications)
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/by-id/{applicationId}")
    @Operation(summary = "Get loan application from database by ID")
    public ResponseEntity<LoanApplicationResponse> getApplication(
            @PathVariable String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var aggregate = useCase.getApplication(tenantId, UUID.fromString(applicationId));
        return ResponseEntity.ok(LoanApplicationResponse.from(mapper.toDto(aggregate)));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping
    @Operation(summary = "List all loan applications for tenant")
    public ResponseEntity<List<LoanApplicationResponse>> listApplications(
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var aggregates = useCase.listApplications(tenantId);
        var responses = enrichWithLoanData(tenantId, mapper.toDtos(aggregates));
        return ResponseEntity.ok(responses);
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List all loan applications for a specific customer")
    public ResponseEntity<List<LoanApplicationResponse>> listApplicationsByCustomer(
            @PathVariable String customerId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var aggregates = useCase.listApplicationsByCustomer(tenantId, UUID.fromString(customerId));
        var responses = enrichWithLoanData(tenantId, mapper.toDtos(aggregates));
        return ResponseEntity.ok(responses);
    }

    // ══════════════════════════════════════════════════════════════
    // BRD UC#03: APPLICATION TRACKER
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loan-applications.tracker", act = "read")
    @GetMapping("/{customerId}/tracker")
    @Operation(summary = "Get application tracker with step-by-step progress (BRD UC#03)")
    public ResponseEntity<ApplicationTrackerResponse> getTracker(
            @PathVariable String customerId,
            @RequestParam(required = false) String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);

        // Try to get from active workflow first
        try {
            var workflowId = resolveWorkflowId(tenantId, customerId, applicationId);
            var workflow = workflowClient.newWorkflowStub(
                    com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.class, workflowId);
            var statusInfo = workflow.getApplicationStatus();

            if (statusInfo != null) {
                BigDecimal trackerTotalPayable = statusInfo.offer() != null
                        ? statusInfo.offer().totalPayable() : null;

                // If no offer yet but basicInfo exists, calculate preliminary totalPayable
                if (trackerTotalPayable == null && statusInfo.basicInfo() != null
                        && statusInfo.basicInfo().requestedAmount() != null
                        && statusInfo.basicInfo().requestedTenureMonths() > 0) {
                    trackerTotalPayable = calculatePreliminaryTotalPayable(
                            tenantId, statusInfo.basicInfo().requestedAmount(),
                            statusInfo.basicInfo().requestedTenureMonths(),
                            statusInfo.basicInfo().productId());
                }

                return ResponseEntity.ok(ApplicationTrackerResponse.build(
                        statusInfo.applicationId(),
                        statusInfo.applicationNumber(),
                        statusInfo.basicInfo() != null ? statusInfo.basicInfo().requestedAmount() : null,
                        trackerTotalPayable,
                        statusInfo.status()
                ));
            }
        } catch (Exception e) {
            log.debug("No active workflow for tracker, falling back to DB: {}", e.getMessage());
        }

        // Fallback: get from DB
        var apps = useCase.listApplicationsByCustomer(tenantId, java.util.UUID.fromString(customerId));
        if (apps.isEmpty()) {
            return ResponseEntity.ok(ApplicationTrackerResponse.build(
                    null, null, null, null, null));
        }

        var latest = apps.get(0);
        BigDecimal dbTotalPayable = latest.getOfferedTotalPayable();
        if (dbTotalPayable == null && latest.getRequestedAmount() != null && latest.getRequestedTenureMonths() > 0) {
            dbTotalPayable = calculatePreliminaryTotalPayable(tenantId, latest.getRequestedAmount(),
                    latest.getRequestedTenureMonths(), latest.getProductId() != null ? latest.getProductId().toString() : null);
        }
        return ResponseEntity.ok(ApplicationTrackerResponse.build(
                latest.getId().getValue().toString(),
                latest.getApplicationNumber(),
                latest.getRequestedAmount(),
                dbTotalPayable,
                latest.getStatus().name(),
                null,
                latest.getCreatedAt() != null ? latest.getCreatedAt().toString() : null,
                latest.getUpdatedAt() != null ? latest.getUpdatedAt().toString() : null
        ));
    }

    @SecuredEndpoint(obj = "loan-applications.tracker", act = "read")
    @GetMapping("/tracker/{applicationId}")
    @Operation(summary = "Get application tracker by applicationId (BRD UC#03)")
    public ResponseEntity<ApplicationTrackerResponse> getTrackerByApplicationId(
            @PathVariable String applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var aggregate = useCase.getApplication(tenantId, UUID.fromString(applicationId));

        // Try active workflow first for real-time state
        if (aggregate.getWorkflowId() != null) {
            try {
                var workflow = workflowClient.newWorkflowStub(
                        LoanApplicationWorkflow.class, aggregate.getWorkflowId());
                var statusInfo = workflow.getApplicationStatus();
                if (statusInfo != null) {
                    return ResponseEntity.ok(ApplicationTrackerResponse.build(
                            statusInfo.applicationId(),
                            statusInfo.applicationNumber(),
                            statusInfo.basicInfo() != null ? statusInfo.basicInfo().requestedAmount() : null,
                            resolveTotalPayable(statusInfo),
                            statusInfo.status()
                    ));
                }
            } catch (Exception e) {
                log.debug("No active workflow for tracker, using DB: {}", e.getMessage());
            }
        }

        // Fallback: build from DB aggregate
        BigDecimal aggTotalPayable = aggregate.getOfferedTotalPayable();
        if (aggTotalPayable == null && aggregate.getRequestedAmount() != null && aggregate.getRequestedTenureMonths() > 0) {
            aggTotalPayable = calculatePreliminaryTotalPayable(tenantId, aggregate.getRequestedAmount(),
                    aggregate.getRequestedTenureMonths(), aggregate.getProductId() != null ? aggregate.getProductId().toString() : null);
        }
        return ResponseEntity.ok(ApplicationTrackerResponse.build(
                aggregate.getId().getValue().toString(),
                aggregate.getApplicationNumber(),
                aggregate.getRequestedAmount(),
                aggTotalPayable,
                aggregate.getStatus().name(),
                null,
                aggregate.getCreatedAt() != null ? aggregate.getCreatedAt().toString() : null,
                aggregate.getUpdatedAt() != null ? aggregate.getUpdatedAt().toString() : null
        ));
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Resolves the active Temporal workflowId for a given customer.
     * If applicationId is provided, uses it directly for precise lookup.
     * Otherwise falls back to finding the latest non-terminal application by customerId.
     */
    private String resolveWorkflowId(UUID tenantId, String customerId, String applicationId) {
        // If applicationId is provided, use it directly for precise lookup
        if (applicationId != null && !applicationId.isBlank()) {
            var app = useCase.getApplication(tenantId, UUID.fromString(applicationId));
            if (app.getWorkflowId() == null) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST,
                        "Application " + applicationId + " has no active workflow");
            }
            return app.getWorkflowId();
        }

        // Fallback: find latest active application by customerId
        var applications = useCase.listApplicationsByCustomer(tenantId, UUID.fromString(customerId));

        return applications.stream()
                .filter(app -> !app.getStatus().isTerminal())
                .filter(app -> app.getWorkflowId() != null)
                .reduce((first, second) -> second) // take the latest (last in list)
                .map(LoanApplicationAggregate::getWorkflowId)
                .orElseThrow(() -> NotFoundException.forEntity(
                        "Active LoanApplication for customer", customerId));
    }

    /**
     * Builds a rich response after sending a signal.
     * Queries the Temporal workflow for current state (step, status, collected data).
     * Falls back to signal-only response if workflow query fails.
     */
    private StepSignalResponse buildStepResponse(String step, String workflowId) {
        var stepInfo = queryWorkflowStepSafe(workflowId);
        var statusInfo = queryWorkflowStatusSafe(workflowId);

        if (stepInfo == null && statusInfo == null) {
            return StepSignalResponse.signalOnly(step);
        }

        return StepSignalResponse.of(step, stepInfo, statusInfo);
    }

    /**
     * Builds a rich response after sending a signal that triggers async processing.
     * Polls the workflow for up to maxWaitSeconds until the status advances past
     * {@code waitUntilNotStatus}, capturing the final state with full data.
     *
     * @param step              the step label (e.g. "IVR_CALLBACK", "SIMAH_CONSENT")
     * @param workflowId        the Temporal workflow ID
     * @param waitUntilNotStatus stop polling once workflow status != this value (null = wait for terminal)
     * @param maxWaitSeconds     max seconds to poll
     */
    private StepSignalResponse buildStepResponseWithWait(String step, String workflowId,
                                                          String waitUntilNotStatus, int maxWaitSeconds) {
        return buildStepResponseWithWait(step, workflowId,
                waitUntilNotStatus != null ? List.of(waitUntilNotStatus) : null, maxWaitSeconds);
    }

    private StepSignalResponse buildStepResponseWithWait(String step, String workflowId,
                                                          List<String> waitWhileStatuses, int maxWaitSeconds) {
        ApplicationStatusInfo statusInfo = null;
        StepInfo stepInfo = null;

        for (int i = 0; i < maxWaitSeconds * 2; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            stepInfo = queryWorkflowStepSafe(workflowId);
            statusInfo = queryWorkflowStatusSafe(workflowId);

            if (statusInfo != null) {
                var s = statusInfo.status();
                // Always stop on terminal states
                if ("APPROVED".equals(s) || "REJECTED".equals(s)
                        || "CANCELLED".equals(s) || "EXPIRED".equals(s)) {
                    break;
                }
                // Stop when status has advanced past ALL expected intermediate states
                if (waitWhileStatuses != null && !waitWhileStatuses.contains(s)) {
                    break;
                }
            }
        }

        if (stepInfo == null && statusInfo == null) {
            return StepSignalResponse.signalOnly(step);
        }

        return StepSignalResponse.of(step, stepInfo, statusInfo);
    }

    /**
     * Safely queries the workflow for current step info.
     * Returns null if the workflow is not yet ready or query fails.
     */
    private StepInfo queryWorkflowStepSafe(String workflowId) {
        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            return workflow.getCurrentStep();
        } catch (Exception e) {
            log.debug("Could not query workflow step for {}: {}", workflowId, e.getMessage());
            return null;
        }
    }

    /**
     * Safely queries the workflow for full application status.
     * Returns null if the workflow is not yet ready or query fails.
     */
    private ApplicationStatusInfo queryWorkflowStatusSafe(String workflowId) {
        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            return workflow.getApplicationStatus();
        } catch (Exception e) {
            log.debug("Could not query workflow status for {}: {}", workflowId, e.getMessage());
            return null;
        }
    }

    private List<LoanApplicationResponse> enrichWithLoanData(UUID tenantId,
                                                               List<com.ksa.financing.lending.application.dto.LoanApplicationDto> dtos) {
        return dtos.stream().map(dto -> {
            try {
                var loan = loanUseCase.getLoanByApplicationId(tenantId, UUID.fromString(dto.id()));
                var enriched = mapper.withLoanData(dto,
                        loan.getId().getValue().toString(),
                        loan.getLoanNumber(),
                        loan.getStatus().name(),
                        loan.getPrincipalAmount(),
                        loan.getTotalAmount(),
                        loan.getInstallmentAmount(),
                        loan.getFineractLoanId() != null ? loan.getFineractLoanId().toString() : null,
                        loan.getDisbursementDate() != null ? loan.getDisbursementDate() : null);
                return LoanApplicationResponse.from(enriched);
            } catch (Exception e) {
                // No loan yet for this application
                return LoanApplicationResponse.from(dto);
            }
        }).toList();
    }

    private BigDecimal resolveTotalPayable(ApplicationStatusInfo statusInfo) {
        if (statusInfo.offer() != null && statusInfo.offer().totalPayable() != null) {
            return statusInfo.offer().totalPayable();
        }
        var info = statusInfo.basicInfo();
        if (info != null && info.requestedAmount() != null && info.requestedTenureMonths() > 0) {
            BigDecimal profitRate = info.profitRate();
            if (profitRate == null) profitRate = new BigDecimal("0.0385");
            else if (profitRate.compareTo(BigDecimal.ONE) > 0) profitRate = profitRate.movePointLeft(2);
            try {
                var calc = com.ksa.financing.lending.domain.service.FinanceCalculationService.calculate(
                        info.requestedAmount(), profitRate, null, info.requestedTenureMonths(), null, null);
                if (!calc.hasErrors()) return calc.totalPayable();
            } catch (Exception e) {
                log.debug("Preliminary totalPayable failed: {}", e.getMessage());
            }
        }
        return null;
    }

    private BigDecimal calculatePreliminaryTotalPayable(UUID tenantId, BigDecimal amount, int tenureMonths, String productId) {
        try {
            BigDecimal profitRate = new BigDecimal("0.0385");
            try {
                var config = productConfigPort.fetchProductConfig(tenantId, productId, amount, tenureMonths);
                if (config.profitRate() != null) {
                    BigDecimal rate = config.profitRate();
                    profitRate = rate.compareTo(BigDecimal.ONE) > 0 ? rate.movePointLeft(2) : rate;
                }
            } catch (Exception ignored) {}

            var calc = com.ksa.financing.lending.domain.service.FinanceCalculationService.calculate(
                    amount, profitRate, null, tenureMonths, null, null);
            return calc.hasErrors() ? null : calc.totalPayable();
        } catch (Exception e) {
            log.debug("Preliminary totalPayable calculation failed: {}", e.getMessage());
            return null;
        }
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private UUID extractUserId(Jwt jwt) {
        var subject = jwt.getSubject();
        if (subject == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No subject claim found in JWT token");
        }
        return UUID.fromString(subject);
    }
}
