package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.adapter.rest.request.*;
import com.ksa.financing.lending.adapter.rest.response.BankAccountInfoResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanApplicationResponse;
import com.ksa.financing.lending.adapter.rest.response.StepSignalResponse;
import com.ksa.financing.lending.application.mapper.LoanApplicationMapper;
import com.ksa.financing.lending.application.usecase.BankAccountLookupService;
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
    private final LoanApplicationMapper mapper;
    private final WorkflowClient workflowClient;
    private final BankAccountLookupService bankAccountLookupService;

    @Value("${temporal.task-queue:loan-application-queue}")
    private String taskQueue;

    public LoanApplicationController(ManageLoanApplicationUseCase useCase,
                                      LoanApplicationMapper mapper,
                                      WorkflowClient workflowClient,
                                      BankAccountLookupService bankAccountLookupService) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.workflowClient = workflowClient;
        this.bankAccountLookupService = bankAccountLookupService;
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

        // Query workflow for initial state (applicationId, applicationNumber)
        ApplicationStatusInfo statusInfo = queryWorkflowStatusSafe(workflowId);

        return ResponseEntity.status(HttpStatus.CREATED).body(new InitiateApplicationResponse(
                workflowId,
                request.customerId(),
                statusInfo != null ? statusInfo.applicationId() : null,
                statusInfo != null ? statusInfo.applicationNumber() : null,
                "DRAFT",
                "Loan application workflow initiated",
                null,
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

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        workflow.submitBasicInfo(new BasicInfoSignal(
                request.productId(),
                request.productCode(),
                request.productName(),
                request.shariaStructure(),
                request.requestedAmount(),
                request.requestedTenureMonths(),
                request.purposeOfFinance(),
                request.profitRate(),
                request.partnerId(),
                request.leadId()
        ));

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

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        workflow.submitBankAccount(new BankAccountSignal(
                request.bankCode(),
                request.bankName(),
                request.iban(),
                request.accountNumber()
        ));

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

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        workflow.giveSimahConsent(new SimahConsentSignal(request.consentGiven()));

        // SIMAH consent triggers credit check → eligibility → offer calc — poll until past SIMAH_CONSENT_GIVEN
        return ResponseEntity.ok(buildStepResponseWithWait("SIMAH_CONSENT", workflowId, "SIMAH_CONSENT_GIVEN", 10));
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

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        workflow.acceptOffer(new AcceptOfferSignal(
                request.accepted(),
                request.selectedAmount()
        ));

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

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        workflow.signContract(new SignContractSignal(
                request.authorizeDigitalSignature(),
                request.authorizeSellCommodity(),
                request.wantPhysicalDelivery()
        ));

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

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        workflow.verifySigningOtp(new OtpVerifySignal(request.otpCode()));

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

        var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
        workflow.ivrCallback(new IvrCallbackSignal(
                request.verified(),
                request.callId(),
                request.verificationStatus()
        ));

        // IVR is the last user step — poll up to 15s for final APPROVED status with loan data
        return ResponseEntity.ok(buildStepResponseWithWait("IVR_CALLBACK", workflowId, null, 15));
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
        var responses = mapper.toDtos(aggregates).stream()
                .map(LoanApplicationResponse::from)
                .toList();
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
        var responses = mapper.toDtos(aggregates).stream()
                .map(LoanApplicationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
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
                // Stop when status has advanced past the expected intermediate state
                if (waitUntilNotStatus != null && !waitUntilNotStatus.equals(s)) {
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
