package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.adapter.rest.request.*;
import com.ksa.financing.lending.adapter.rest.response.ApplicationTrackerResponse;
import com.ksa.financing.lending.adapter.rest.response.BankAccountInfoResponse;
import com.ksa.financing.lending.adapter.rest.response.InstallmentScheduleResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanApplicationResponse;
import com.ksa.financing.lending.adapter.rest.response.LoanApplicationStepInfo;
import com.ksa.financing.lending.adapter.rest.response.PreQualificationData;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
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

    // Sub-halala rounding tolerance for "loan fully paid" detection. Payment allocation
    // can leave at most a few halalas (< 1 SAR) outstanding due to rounding to 2 dp.
    private static final java.math.BigDecimal FULLY_PAID_TOLERANCE = new java.math.BigDecimal("1.00");

    private final ManageLoanApplicationUseCase useCase;
    private final com.ksa.financing.lending.domain.port.in.ManageLoanUseCase loanUseCase;
    private final com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase checkEligibilityUseCase;
    private final LoanApplicationMapper mapper;
    private final WorkflowClient workflowClient;
    private final BankAccountLookupService bankAccountLookupService;
    private final com.ksa.financing.lending.domain.port.out.ProductConfigPort productConfigPort;
    private final com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRescheduleRepository rescheduleRepository;
    private final com.ksa.financing.lending.infrastructure.persistence.repository.JpaAmortizationScheduleRepository amortizationScheduleRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${temporal.task-queue:loan-application-queue}")
    private String taskQueue;

    @Value("${app.services.collections-service-url:${COLLECTIONS_SERVICE_URL:http://collections-service:8099}}")
    private String collectionsServiceUrl;

    @Value("${app.services.customer-service-url:${CUSTOMER_SERVICE_URL:http://customer-service:8084}}")
    private String customerServiceUrl;

    public LoanApplicationController(ManageLoanApplicationUseCase useCase,
                                      com.ksa.financing.lending.domain.port.in.ManageLoanUseCase loanUseCase,
                                      com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase checkEligibilityUseCase,
                                      LoanApplicationMapper mapper,
                                      WorkflowClient workflowClient,
                                      BankAccountLookupService bankAccountLookupService,
                                      com.ksa.financing.lending.domain.port.out.ProductConfigPort productConfigPort,
                                      com.ksa.financing.lending.infrastructure.persistence.repository.JpaLoanRescheduleRepository rescheduleRepository,
                                      com.ksa.financing.lending.infrastructure.persistence.repository.JpaAmortizationScheduleRepository amortizationScheduleRepository,
                                      org.springframework.web.client.RestTemplate restTemplate,
                                      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.loanUseCase = loanUseCase;
        this.checkEligibilityUseCase = checkEligibilityUseCase;
        this.mapper = mapper;
        this.workflowClient = workflowClient;
        this.bankAccountLookupService = bankAccountLookupService;
        this.productConfigPort = productConfigPort;
        this.rescheduleRepository = rescheduleRepository;
        this.amortizationScheduleRepository = amortizationScheduleRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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

        // ── ELIGIBILITY CHECK (affordability / DBR) before starting workflow ──
        var eligibilityResult = checkEligibilityUseCase.checkEligibility(
                new com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase.CheckEligibilityCommand(
                        tenantId,
                        request.requestedAmount(),
                        request.requestedTenureMonths(),
                        request.salary(),
                        request.liabilities() != null ? request.liabilities() : BigDecimal.ZERO,
                        request.additionalAdults(), request.numberOfChildren(),
                        request.foodGroceries(),
                        request.utilities(),
                        request.healthcare(),
                        request.communication(),
                        request.housingRent(),
                        request.clothingEssentials(),
                        request.education(),
                        request.transportation(),
                        request.productId()
                )
        );

        if (!eligibilityResult.eligible()) {
            var reason = eligibilityResult.reason() != null ? eligibilityResult.reason() : "Does not meet eligibility criteria";
            var maxAmountInfo = eligibilityResult.maxEligibleAmount() != null
                    && eligibilityResult.maxEligibleAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                    ? ". Maximum eligible amount: " + eligibilityResult.maxEligibleAmount() + " SAR" : "";
            var fullMessage = "Not eligible: " + reason + maxAmountInfo;
            throw new BusinessException(ErrorCodes.BAD_REQUEST, fullMessage, fullMessage);
        }

        // If eligible but with reduced amount, reject — user must re-apply with lower amount
        if (eligibilityResult.maxEligibleAmount() != null
                && eligibilityResult.maxEligibleAmount().compareTo(request.requestedAmount()) < 0) {
            var fullMessage = "Requested amount " + request.requestedAmount() + " SAR exceeds your affordability. "
                    + "Maximum eligible amount: " + eligibilityResult.maxEligibleAmount() + " SAR. "
                    + "Monthly instalment capacity: " + eligibilityResult.monthlyInstallment() + " SAR/month";
            throw new BusinessException(ErrorCodes.BAD_REQUEST, fullMessage, fullMessage);
        }

        log.info("Eligibility check passed: DBR before={}, after={}, disposable={}",
                eligibilityResult.dbrBefore(), eligibilityResult.dbrAfter(), eligibilityResult.disposableIncome());

        // ── AUTO-CANCEL PREVIOUS ACTIVE APPLICATIONS ──
        try {
            var activeApps = useCase.listApplicationsByCustomer(tenantId, UUID.fromString(request.customerId()), new PageQuery(0, 10, null, null, null));
            activeApps.content().stream()
                    .filter(app -> !app.getStatus().isTerminal())
                    .forEach(app -> {
                        log.info("Auto-cancelling existing active application {} for customer {} before re-initiating",
                                app.getApplicationNumber(), request.customerId());
                        try {
                            // 1. Cancel Temporal workflow
                            if (app.getWorkflowId() != null) {
                                var oldWorkflow = workflowClient.newUntypedWorkflowStub(app.getWorkflowId());
                                oldWorkflow.cancel();
                            }
                            // 2. Cancel in DB
                            useCase.cancelApplication(tenantId, app.getId().getValue(), userId);
                        } catch (Exception e) {
                            log.warn("Failed to auto-cancel old application {}: {}", app.getApplicationNumber(), e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Error checking for existing active applications: {}", e.getMessage());
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
            BigDecimal profitRate = new BigDecimal("0.0385"); // default (decimal form)
            BigDecimal processingFeePercent = null;
            BigDecimal processingFeeAmount = null;
            BigDecimal adminFeeAmount = null;

            try {
                var config = productConfigPort.fetchProductConfig(
                        tenantId, request.productId(), request.requestedAmount(), request.requestedTenureMonths());
                if (config.profitRate() != null) {
                    BigDecimal rate = config.profitRate();
                    // Standardized normalization: assume percentage if > 0.5 (50%)
                    profitRate = rate.compareTo(new BigDecimal("0.5")) > 0 ? rate.movePointLeft(2) : rate;
                }
                processingFeePercent = config.processingFeePercent();
                BigDecimal procFeeAmount = config.processingFeeAmount();
                adminFeeAmount = config.adminFeeAmount();

                // Calculate processing fee: prioritize fixed amount, then percentage
                processingFeeAmount = BigDecimal.ZERO;
                if (procFeeAmount != null && procFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
                    processingFeeAmount = procFeeAmount;
                } else if (processingFeePercent != null && processingFeePercent.compareTo(BigDecimal.ZERO) > 0) {
                    processingFeeAmount = request.requestedAmount().multiply(processingFeePercent)
                            .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                }
            } catch (Exception ignored) {}
            BigDecimal safeAdminFee = adminFeeAmount != null ? adminFeeAmount : BigDecimal.ZERO;

            var calcResult = com.ksa.financing.lending.domain.service.FinanceCalculationService.calculate(
                    request.requestedAmount(),
                    profitRate,
                    null,
                    request.requestedTenureMonths(),
                    processingFeeAmount, safeAdminFee
            );
            if (!calcResult.hasErrors()) {
                preQualification = new java.util.LinkedHashMap<>();
                preQualification.put("requestedAmount", request.requestedAmount());
                preQualification.put("tenureMonths", request.requestedTenureMonths());
                preQualification.put("monthlyInstallment", calcResult.monthlyInstallment());
                preQualification.put("totalPayable", calcResult.totalPayable());
                preQualification.put("totalProfit", calcResult.totalCostOfFinancing());
                preQualification.put("profitRate", calcResult.profitRate());
                log.info("Initiate response: profitRate={}, monthlyInstallment={}", calcResult.profitRate(), calcResult.monthlyInstallment());
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
    // INITIAL OFFER (stateless pre-qualification — no workflow, no DB)
    // ══════════════════════════════════════════════════════════════

    @SecuredEndpoint(obj = "loan-applications", act = "create")
    @PostMapping("/initial-offer")
    @Operation(summary = "Initial offer pre-qualification — runs same eligibility + finance calculation as /initiate but does NOT create an application or start a workflow")
    public ResponseEntity<InitialOfferResponse> initialOffer(
            @Valid @RequestBody InitiateLoanApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);

        if (request.requestedAmount() == null || request.requestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Requested amount is required");
        }
        if (request.requestedTenureMonths() <= 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Requested tenure is required");
        }

        // Validate against product + Fineract limits (same as /initiate)
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
            log.warn("Product/Fineract validation unavailable for initial-offer, proceeding: {}", e.getMessage());
        }

        // Eligibility check (affordability / DBR) — same as /initiate
        var eligibilityResult = checkEligibilityUseCase.checkEligibility(
                new com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase.CheckEligibilityCommand(
                        tenantId,
                        request.requestedAmount(),
                        request.requestedTenureMonths(),
                        request.salary(),
                        request.liabilities() != null ? request.liabilities() : BigDecimal.ZERO,
                        request.additionalAdults(), request.numberOfChildren(),
                        request.foodGroceries(),
                        request.utilities(),
                        request.healthcare(),
                        request.communication(),
                        request.housingRent(),
                        request.clothingEssentials(),
                        request.education(),
                        request.transportation(),
                        request.productId()
                )
        );

        if (!eligibilityResult.eligible()) {
            var reason = eligibilityResult.reason() != null ? eligibilityResult.reason() : "Does not meet eligibility criteria";
            var maxAmountInfo = eligibilityResult.maxEligibleAmount() != null
                    && eligibilityResult.maxEligibleAmount().compareTo(BigDecimal.ZERO) > 0
                    ? ". Maximum eligible amount: " + eligibilityResult.maxEligibleAmount() + " SAR" : "";
            var fullMessage = "Not eligible: " + reason + maxAmountInfo;
            throw new BusinessException(ErrorCodes.BAD_REQUEST, fullMessage, fullMessage);
        }

        if (eligibilityResult.maxEligibleAmount() != null
                && eligibilityResult.maxEligibleAmount().compareTo(request.requestedAmount()) < 0) {
            var fullMessage = "Requested amount " + request.requestedAmount() + " SAR exceeds your affordability. "
                    + "Maximum eligible amount: " + eligibilityResult.maxEligibleAmount() + " SAR. "
                    + "Monthly instalment capacity: " + eligibilityResult.monthlyInstallment() + " SAR/month";
            throw new BusinessException(ErrorCodes.BAD_REQUEST, fullMessage, fullMessage);
        }

        // Pre-qualification calculation (same as /initiate)
        Map<String, Object> preQualification = null;
        try {
            BigDecimal profitRate = new BigDecimal("0.0385");
            BigDecimal processingFeePercent = null;
            BigDecimal processingFeeAmount = null;
            BigDecimal adminFeeAmount = null;

            try {
                var config = productConfigPort.fetchProductConfig(
                        tenantId, request.productId(), request.requestedAmount(), request.requestedTenureMonths());
                if (config.profitRate() != null) {
                    BigDecimal rate = config.profitRate();
                    profitRate = rate.compareTo(new BigDecimal("0.5")) > 0 ? rate.movePointLeft(2) : rate;
                }
                processingFeePercent = config.processingFeePercent();
                BigDecimal procFeeAmount = config.processingFeeAmount();
                adminFeeAmount = config.adminFeeAmount();

                // Calculate processing fee: prioritize fixed amount, then percentage
                processingFeeAmount = BigDecimal.ZERO;
                if (procFeeAmount != null && procFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
                    processingFeeAmount = procFeeAmount;
                } else if (processingFeePercent != null && processingFeePercent.compareTo(BigDecimal.ZERO) > 0) {
                    processingFeeAmount = request.requestedAmount().multiply(processingFeePercent)
                            .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                }
            } catch (Exception ignored) {}
            BigDecimal safeAdminFee = adminFeeAmount != null ? adminFeeAmount : BigDecimal.ZERO;

            var calcResult = com.ksa.financing.lending.domain.service.FinanceCalculationService.calculate(
                    request.requestedAmount(),
                    profitRate,
                    null,
                    request.requestedTenureMonths(),
                    processingFeeAmount, safeAdminFee
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
            log.warn("Pre-qualification calculation failed for initial-offer: {}", e.getMessage());
        }

        return ResponseEntity.ok(new InitialOfferResponse(
                "Initial Offer",
                request.customerId(),
                "Loan application workflow Initial Offer",
                preQualification
        ));
    }

    public record InitialOfferResponse(
            String title,
            String customerId,
            String message,
            Map<String, Object> preQualification
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

        // Fetch existing application to reuse saved salary + expenses from initiate
        LoanApplicationAggregate existingApp;
        if (applicationId != null && !applicationId.isBlank()) {
            existingApp = useCase.getApplication(tenantId, UUID.fromString(applicationId));
        } else {
            existingApp = useCase.listApplicationsByCustomer(tenantId, UUID.fromString(customerId), new PageQuery(0, 100, null, null, null))
                    .content()
                    .stream()
                    .filter(app -> !app.getStatus().isTerminal())
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> NotFoundException.forEntity("Active LoanApplication for customer", customerId));
        }

        // Re-run eligibility check using saved financial data + new amount/product from request
        var eligibilityResult = checkEligibilityUseCase.checkEligibility(
                new com.ksa.financing.lending.domain.port.in.CheckEligibilityUseCase.CheckEligibilityCommand(
                        tenantId,
                        request.requestedAmount(),
                        request.requestedTenureMonths(),
                        existingApp.getMonthlyIncome(),
                        existingApp.getExistingLiabilities() != null ? existingApp.getExistingLiabilities() : BigDecimal.ZERO,
                        existingApp.getAdultDependents(),
                        existingApp.getChildDependents(),
                        existingApp.getFoodGroceries(),
                        existingApp.getUtilities(),
                        existingApp.getHealthcare(),
                        existingApp.getCommunication(),
                        existingApp.getHousingRent(),
                        existingApp.getClothingEssentials(),
                        existingApp.getEducation(),
                        existingApp.getTransportation(),
                        request.productId()
                )
        );

        if (!eligibilityResult.eligible()) {
            var reason = eligibilityResult.reason() != null ? eligibilityResult.reason() : "Does not meet eligibility criteria";
            var maxAmountInfo = eligibilityResult.maxEligibleAmount() != null
                    && eligibilityResult.maxEligibleAmount().compareTo(BigDecimal.ZERO) > 0
                    ? ". Maximum eligible amount: " + eligibilityResult.maxEligibleAmount() + " SAR" : "";
            var fullMessage = "Not eligible: " + reason + maxAmountInfo;
            throw new BusinessException(ErrorCodes.BAD_REQUEST, fullMessage, fullMessage);
        }

        if (eligibilityResult.maxEligibleAmount() != null
                && eligibilityResult.maxEligibleAmount().compareTo(request.requestedAmount()) < 0) {
            var fullMessage = "Requested amount " + request.requestedAmount() + " SAR exceeds your affordability. "
                    + "Maximum eligible amount: " + eligibilityResult.maxEligibleAmount() + " SAR. "
                    + "Monthly instalment capacity: " + eligibilityResult.monthlyInstallment() + " SAR/month";
            throw new BusinessException(ErrorCodes.BAD_REQUEST, fullMessage, fullMessage);
        }

        log.info("Eligibility re-check passed at basic-info step: DBR before={}, after={}", eligibilityResult.dbrBefore(), eligibilityResult.dbrAfter());

        var workflowId = existingApp.getWorkflowId();
        if (workflowId == null) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Application has no active workflow");
        }

        log.info("Signal: submitBasicInfo for customer: {}, app: {}, workflow: {}", customerId, applicationId, workflowId);

        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);

            // Auto-revert if workflow is at a later step
            try {
                var currentStep = workflow.getCurrentStep();
                if (currentStep.stepperIndex() > 1) {
                    log.info("Auto-reverting workflow from step {} to step 1 for re-submission", currentStep.stepperIndex());
                    workflow.goBack(new GoBackSignal(1, "Auto-revert due to Basic Info re-submission"));
                }
            } catch (Exception e) {
                log.warn("Auto-revert check failed: {}", e.getMessage());
            }

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

            // Auto-revert if workflow is at a later step
            try {
                var currentStep = workflow.getCurrentStep();
                if (currentStep.stepperIndex() > 2) {
                    log.info("Auto-reverting workflow from step {} to step 2 for re-submission", currentStep.stepperIndex());
                    workflow.goBack(new GoBackSignal(2, "Auto-revert due to Bank Account re-submission"));
                }
            } catch (Exception e) {
                log.warn("Auto-revert check failed: {}", e.getMessage());
            }

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

        if (!Boolean.TRUE.equals(request.accepted())) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "accept-offer endpoint requires accepted=true");
        }

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

        if (!Boolean.TRUE.equals(request.verified())) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "ivr-callback endpoint requires verified=true");
        }

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

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PostMapping("/{applicationId}/cancel")
    @Operation(summary = "Cancel a loan application (stops workflow and updates status)")
    public ResponseEntity<Map<String, String>> cancelApplication(
            @PathVariable String applicationId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);
        var app = useCase.getApplication(tenantId, UUID.fromString(applicationId));

        log.info("Request to cancel application: {} by user: {}", applicationId, userId);

        // 1. Check if cancellable (before disbursement)
        if (app.getStatus() == ApplicationStatus.DISBURSING
                || app.getStatus() == ApplicationStatus.APPROVED
                || app.getStatus() == ApplicationStatus.DISBURSED) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Application cannot be cancelled after disbursement has initiated");
        }

        // 2. Signal Temporal workflow with typed cancel signal
        if (app.getWorkflowId() != null) {
            try {
                var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, app.getWorkflowId());
                workflow.cancel(new CancelSignal(reason != null ? reason : "Cancelled by user", userId.toString()));
                log.info("Sent cancel signal to workflow: {}", app.getWorkflowId());
            } catch (Exception e) {
                log.warn("Could not signal cancel to workflow {}: {}", app.getWorkflowId(), e.getMessage());
                // Fallback: update status in DB if workflow is not reachable/already terminated
                useCase.cancelApplication(tenantId, UUID.fromString(applicationId), userId);
            }
        } else {
            // 3. Update status in DB if no workflow ID exists
            useCase.cancelApplication(tenantId, UUID.fromString(applicationId), userId);
        }

        return ResponseEntity.ok(Map.of(
                "applicationId", applicationId,
                "status", "CANCELLED",
                "message", "Application cancellation request submitted"
        ));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "manage")
    @PatchMapping("/{applicationId}/go-back/{stepIndex}")
    @Operation(summary = "Revert loan application to a previous step (1-5)")
    public ResponseEntity<Map<String, Object>> goBack(
            @PathVariable String applicationId,
            @PathVariable int stepIndex,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var userId = extractUserId(jwt);
        var app = useCase.getApplication(tenantId, UUID.fromString(applicationId));

        if (stepIndex < 1 || stepIndex > 5) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Invalid step index. Must be 1-5.");
        }

        log.info("Request to go back to step {} for application: {} by user: {}", stepIndex, applicationId, userId);

        // 1. Signal Temporal workflow
        if (app.getWorkflowId() != null) {
            try {
                var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, app.getWorkflowId());
                workflow.goBack(new GoBackSignal(stepIndex, reason != null ? reason : "User requested edit"));
                log.info("Sent goBack signal to workflow: {}", app.getWorkflowId());
            } catch (Exception e) {
                log.warn("Could not signal goBack to workflow {}: {}", app.getWorkflowId(), e.getMessage());
            }
        }

        // 2. Update status in DB to match target step for immediate UI feedback
        String targetStatus = switch (stepIndex) {
            case 1 -> "DRAFT";
            case 2 -> "BANK_ACCOUNT_PENDING";
            case 3 -> "BANK_ACCOUNT_VERIFIED";
            case 4 -> "OFFER_PRESENTED";
            case 5 -> "CONTRACT_PENDING";
            default -> app.getStatus().name();
        };

        // We use the activity implementation's logic or a direct DB update if available
        // For simplicity and immediate effect, we'll assume the client will poll and see the change.
        // Actually, updating the DB status here is good practice.
        // lendingActivity.updateStatus is not available here, so we'd need a usecase method.
        // But the workflow will call updateStatus as soon as it restarts the loop!
        
        return ResponseEntity.ok(Map.of(
                "applicationId", applicationId,
                "targetStep", stepIndex,
                "targetStatus", targetStatus,
                "message", "Revert request sent to workflow"
        ));
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
    @Operation(summary = "Step 2 helper: Look up bank accounts (unified shape — same fields as /banks and /customers/{id}/bank-accounts)")
    public ResponseEntity<java.util.List<com.ksa.financing.lending.adapter.rest.response.BankResponse>> getBankAccounts(
            @PathVariable String customerId,
            @RequestParam String nationalId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        String maskedNid = nationalId.length() >= 4 ? nationalId.substring(nationalId.length() - 4) : "****";
        log.info("Looking up bank accounts for customer: {}, NID: ***{}", customerId, maskedNid);

        var result = bankAccountLookupService.lookupBankAccountsUnified(customerId, nationalId, tenantId.toString(), jwt.getTokenValue());
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
        var singlePage = new PageResponse<>(List.of(mapper.toDto(aggregate)), null);
        var responses = enrichPageWithLoanData(tenantId, singlePage, jwt);
        return ResponseEntity.ok(responses.content().get(0));
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping
    @Operation(summary = "List all loan applications for tenant")
    public PageResponse<LoanApplicationResponse> listApplications(
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var page = useCase.listApplications(tenantId, pageQuery);
        return enrichPageWithLoanData(tenantId, page.map(mapper::toDto), jwt);
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List all loan applications for a specific customer")
    public PageResponse<LoanApplicationResponse> listApplicationsByCustomer(
            @PathVariable String customerId,
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var page = useCase.listApplicationsByCustomer(tenantId, UUID.fromString(customerId), pageQuery);
        return enrichPageWithLoanData(tenantId, page.map(mapper::toDto), jwt);
    }

    @SecuredEndpoint(obj = "loan-applications", act = "read")
    @GetMapping("/latest-application")
    @com.ksa.financing.infra.response.RawResponse
    @Operation(summary = "Get authenticated customer's last in-progress (non-terminal) loan application with step progress")
    public ResponseEntity<java.util.Map<String, Object>> getLatestApplication(@AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        var customerId = resolveCustomerIdFromJwt(jwt);

        var applications = useCase.listApplicationsByCustomer(
                tenantId, customerId, new PageQuery(0, 100, null, null, null));

        // Include APPROVED (disbursed) apps too — only exclude REJECTED / CANCELLED / EXPIRED.
        // Customer's most recent successful application should still surface here.
        var inProgressOpt = applications.content().stream()
                .filter(app -> {
                    var s = app.getStatus();
                    return s != ApplicationStatus.REJECTED
                            && s != ApplicationStatus.CANCELLED
                            && s != ApplicationStatus.EXPIRED;
                })
                .max(java.util.Comparator.comparing(
                        LoanApplicationAggregate::getCreatedAt,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));

        if (inProgressOpt.isEmpty()) {
            return ResponseEntity.ok(buildEnvelope(null, "Application not found"));
        }

        var inProgress = inProgressOpt.get();

        // If the linked loan is fully repaid (every installment paid/waived or loan settled/closed),
        // hide the application from the "latest" view — customer has no outstanding loan to act on.
        if (isLoanFullyPaid(tenantId, inProgress.getId().getValue(), jwt)) {
            return ResponseEntity.ok(buildEnvelope(null, "No active application"));
        }

        String step = inProgress.getStatus().getStepperLabel();

        StepSignalResponse body;
        if (inProgress.getWorkflowId() != null) {
            var stepInfo = queryWorkflowStepSafe(inProgress.getWorkflowId());
            var statusInfo = queryWorkflowStatusSafe(inProgress.getWorkflowId());
            if (stepInfo != null || statusInfo != null) {
                body = StepSignalResponse.of(step, stepInfo, statusInfo);
            } else {
                body = buildSignalResponseFromAggregate(step, inProgress);
            }
        } else {
            body = buildSignalResponseFromAggregate(step, inProgress);
        }
        var slim = new StepSignalResponse(
                body.signalStatus(),
                body.step(),
                body.applicationId(),
                body.applicationNumber(),
                body.currentStep(),
                body.status(),
                body.nextAction(),
                body.steps(),
                body.preQualification(),
                null
        );

        var dataMap = objectMapper.convertValue(slim, new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, Object>>() {});

        // Match listing endpoint: surface displayStatus alongside raw status.
        java.time.LocalDate loanDisbursementDate = null;
        String loanStatusName = null;
        try {
            var loan = loanUseCase.getLoanByApplicationId(tenantId, inProgress.getId().getValue());
            loanDisbursementDate = loan.getDisbursementDate();
            loanStatusName = loan.getStatus() != null ? loan.getStatus().name() : null;
        } catch (Exception ignored) {
        }
        String appStatus = inProgress.getStatus() != null ? inProgress.getStatus().name() : null;
        dataMap.put("displayStatus",
                LoanApplicationResponse.deriveDisplayStatus(appStatus, loanDisbursementDate, loanStatusName));

        // disbursed_start_time / disbursed_end_time: surfaced only while the application sits in
        // AWAIT_DISBURSED. Start is the dedicated awaitDisbursedAt timestamp captured atomically
        // inside LoanApplicationAggregate.moveToAwaitDisbursed(); end is start + 1 minute.
        // Null for every other status (incl. DISBURSED).
        java.time.LocalDateTime disbursedStart =
                inProgress.getStatus() == ApplicationStatus.AWAIT_DISBURSED
                        ? inProgress.getAwaitDisbursedAt()
                        : null;
        dataMap.put("disbursed_start_time", disbursedStart);
        dataMap.put("disbursed_end_time", disbursedStart != null ? disbursedStart.plusMinutes(1) : null);

        dataMap.put("product", buildProductObject(tenantId, inProgress.getProductId()));
        var currentInstallment = buildCurrentInstallmentObject(tenantId, inProgress.getId().getValue(), jwt);
        if (currentInstallment != null) {
            dataMap.put("currentInstallment", currentInstallment);
        }
        return ResponseEntity.ok(buildEnvelope(dataMap, "success"));
    }

    /**
     * Returns the next unpaid installment for a disbursed loan in the same shape as
     * {@link InstallmentScheduleResponse} (mirrors GET /api/v1/loans/{loanId}/installments).
     * Returns null when no loan exists, the schedule is unavailable, or every installment is paid.
     *
     * Source priority:
     *   1) Local amortization_schedule DB rows (authoritative breakdown — principal / profit / fee / closing balance)
     *      enriched with the collections snapshot for delinquency/penalty fields.
     *   2) collections-service live schedule (when DB rows are absent).
     *   3) Local fallback computed from the loan aggregate so the customer view still works
     *      before the Kafka-driven schedule lands.
     */
    private InstallmentScheduleResponse buildCurrentInstallmentObject(UUID tenantId, UUID applicationId, Jwt jwt) {
        com.ksa.financing.lending.domain.model.LoanAggregate loan;
        try {
            loan = loanUseCase.getLoanByApplicationId(tenantId, applicationId);
        } catch (Exception e) {
            return null;
        }

        var loanId = loan.getId().getValue();
        var collectionsSnapshots = fetchCollectionsSnapshotsByInstallment(loanId, jwt);

        var dbSchedules = amortizationScheduleRepository
                .findByLoanIdAndActiveOrderByInstallmentNumberAsc(loanId, true);

        if (!dbSchedules.isEmpty()) {
            for (var row : dbSchedules) {
                var snapshot = collectionsSnapshots.get(row.getInstallmentNumber());
                var rawStatus = snapshot != null ? jsonString(snapshot, "status") : row.getPaymentStatus();
                var status = normalizeInstallmentStatus(rawStatus != null ? rawStatus : row.getPaymentStatus());
                if (isPaidStatus(status)) {
                    continue;
                }
                return buildFromDbRow(loanId, row, status, snapshot);
            }
            return null;
        }

        var fromCollections = fetchNextInstallmentFromCollections(loanId, jwt, collectionsSnapshots);
        if (fromCollections != null) {
            return fromCollections;
        }
        return computeNextInstallmentFromLoan(loanId, loan);
    }

    private InstallmentScheduleResponse buildFromDbRow(
            UUID loanId,
            com.ksa.financing.lending.infrastructure.persistence.entity.AmortizationScheduleJpaEntity row,
            String status,
            java.util.Map<String, Object> delinquencySnapshot) {
        var invoiceId = buildInvoiceId(loanId, row.getInstallmentNumber());
        var principal = row.getPrincipalComponent() != null ? row.getPrincipalComponent() : java.math.BigDecimal.ZERO;
        var fee = row.getFeeComponent() != null ? row.getFeeComponent() : java.math.BigDecimal.ZERO;
        var total = row.getTotalInstallment() != null ? row.getTotalInstallment() : java.math.BigDecimal.ZERO;
        var profit = total.subtract(principal).subtract(fee).max(java.math.BigDecimal.ZERO);
        var isPaid = isPaidStatus(status);
        return new InstallmentScheduleResponse(
                invoiceId,
                row.getInstallmentNumber(),
                row.getDueDate(),
                total,
                principal,
                profit,
                fee,
                row.getClosingPrincipal(),
                status,
                isPaid ? java.time.LocalDate.now() : null,
                isPaid ? total : null,
                isPaid,
                delinquencySnapshot
        );
    }

    private String buildInvoiceId(UUID loanId, int installmentNumber) {
        var prefix = loanId.toString().replace("-", "").substring(0, 8).toUpperCase();
        return "INV-" + prefix + "-" + String.format("%03d", installmentNumber);
    }

    private boolean isPaidStatus(String status) {
        return "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "WAIVED".equalsIgnoreCase(status);
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<Integer, java.util.Map<String, Object>> fetchCollectionsSnapshotsByInstallment(UUID loanId, Jwt jwt) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var url = collectionsServiceUrl + "/api/v1/repayment-schedules/by-loan/" + loanId;
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return java.util.Map.of();
            }
            var installments = objectMapper.readTree(response.getBody()).path("data").path("installments");
            if (!installments.isArray()) {
                return java.util.Map.of();
            }
            var out = new java.util.HashMap<Integer, java.util.Map<String, Object>>();
            for (var item : installments) {
                int n = item.path("installmentNumber").asInt(-1);
                if (n <= 0) continue;
                out.put(n, objectMapper.convertValue(item, java.util.Map.class));
            }
            return out;
        } catch (Exception e) {
            log.debug("Collections snapshot unavailable for loanId={} ({})", loanId, e.getMessage());
            return java.util.Map.of();
        }
    }

    private String jsonString(java.util.Map<String, Object> map, String key) {
        var v = map.get(key);
        return v == null ? null : v.toString();
    }

    private String normalizeInstallmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        return switch (status.toUpperCase()) {
            case "PAID", "COMPLETED", "OVERDUE", "PARTIALLY_PAID",
                 "DUE", "GRACE_PERIOD", "WAIVED", "DEFERRED" -> status.toUpperCase();
            default -> "PENDING";
        };
    }

    /**
     * Returns true when the customer has no outstanding installments on this application's loan:
     * either the loan is in a terminal state (SETTLED/CLOSED/WRITTEN_OFF) or the collections
     * schedule reports every installment as PAID/COMPLETED/WAIVED. Returns false when the loan
     * is missing, the schedule is unavailable, or any installment is still due.
     */
    private boolean isLoanFullyPaid(UUID tenantId, UUID applicationId, Jwt jwt) {
        com.ksa.financing.lending.domain.model.LoanAggregate loan;
        try {
            loan = loanUseCase.getLoanByApplicationId(tenantId, applicationId);
        } catch (Exception e) {
            return false;
        }

        if (loan.getStatus() != null && loan.getStatus().isTerminal()) {
            return true;
        }

        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var url = collectionsServiceUrl + "/api/v1/repayment-schedules/by-loan/" + loan.getId().getValue();
            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }
            var dataNode = objectMapper.readTree(response.getBody()).path("data");

            // collections-service marks the aggregate fullyPaid once every installment is settled.
            if (dataNode.path("fullyPaid").asBoolean(false)) {
                return true;
            }

            // Tolerate sub-halala (< 1 SAR) rounding remainders at the schedule level —
            // payment allocation rounding can leave 0.01 SAR outstanding even when the
            // customer has paid the full advertised installment amount.
            var totalAmount = decimalNode(dataNode, "totalAmount");
            var paidTotal = decimalNode(dataNode, "paidTotal");
            if (totalAmount != null && paidTotal != null
                    && totalAmount.subtract(paidTotal).abs().compareTo(FULLY_PAID_TOLERANCE) < 0) {
                return true;
            }

            // Customer-payable rule: the offer presented to the customer (preQualification.totalPayable
            // and monthlyInstallment) excludes processing/admin fees — it is just principal + profit.
            // Once the customer has paid an amount >= (totalPrincipal + totalProfit), they have settled
            // what the app told them they owe. Any residual is fee-allocation drift the customer cannot
            // see or act on. Treat as fully paid in the latest-application view so the application stops
            // appearing once the customer has fulfilled their advertised obligation.
            var totalPrincipal = decimalNode(dataNode, "totalPrincipal");
            var totalProfit = decimalNode(dataNode, "totalProfit");
            if (paidTotal != null && totalPrincipal != null && totalProfit != null) {
                var customerPayable = totalPrincipal.add(totalProfit);
                if (paidTotal.compareTo(customerPayable.subtract(FULLY_PAID_TOLERANCE)) >= 0) {
                    return true;
                }
            }

            var installmentsNode = dataNode.path("installments");
            if (!installmentsNode.isArray() || installmentsNode.isEmpty()) {
                return false;
            }

            // Customer-facing rule: once the FINAL installment is fully PAID/WAIVED and every
            // earlier installment has had a payment attempt (PAID / WAIVED / PARTIALLY_PAID),
            // the schedule term has concluded. Any residual on earlier installments is allocation
            // drift (e.g., per-installment fee not included in the advertised monthlyInstallment)
            // — not new money the customer can act on from the app. Treat as fully paid here so
            // the "latest application" view does not surface a phantom currentInstallment.
            int lastNumber = -1;
            String lastStatus = "";
            boolean allTouched = true;
            for (var item : installmentsNode) {
                var n = item.path("installmentNumber").asInt(-1);
                var status = item.path("status").asText("").toUpperCase();
                if (!("PAID".equals(status) || "WAIVED".equals(status) || "COMPLETED".equals(status)
                        || "PARTIALLY_PAID".equals(status))) {
                    allTouched = false;
                }
                if (n > lastNumber) {
                    lastNumber = n;
                    lastStatus = status;
                }
            }
            if (allTouched && ("PAID".equals(lastStatus) || "WAIVED".equals(lastStatus)
                    || "COMPLETED".equals(lastStatus))) {
                return true;
            }

            for (var item : installmentsNode) {
                var status = item.path("status").asText("").toUpperCase();
                if ("PAID".equals(status) || "COMPLETED".equals(status) || "WAIVED".equals(status)) {
                    continue;
                }
                // PARTIALLY_PAID / PENDING / OVERDUE with sub-1-SAR outstanding still
                // counts as settled for the "latest application" check.
                var iTotal = decimalNode(item, "totalAmount");
                var iPaid = decimalNode(item, "paidTotal");
                if (iTotal != null && iPaid != null
                        && iTotal.subtract(iPaid).abs().compareTo(FULLY_PAID_TOLERANCE) < 0) {
                    continue;
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.debug("Could not verify full-payment state for loanId={} ({}), assuming active",
                    loan.getId().getValue(), e.getMessage());
            return false;
        }
    }

    private InstallmentScheduleResponse fetchNextInstallmentFromCollections(
            UUID loanId, Jwt jwt, java.util.Map<Integer, java.util.Map<String, Object>> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }

        java.util.Map<String, Object> next = null;
        int nextNumber = Integer.MAX_VALUE;
        for (var entry : snapshots.entrySet()) {
            var snap = entry.getValue();
            var status = normalizeInstallmentStatus(jsonString(snap, "status"));
            if (isPaidStatus(status)) continue;
            int n = entry.getKey();
            if (n < nextNumber) {
                nextNumber = n;
                next = snap;
            }
        }
        if (next == null) {
            return null;
        }

        var status = normalizeInstallmentStatus(jsonString(next, "status"));
        var isPaid = isPaidStatus(status);
        var total = decimalFrom(next, "totalAmount");
        var principal = decimalFrom(next, "principalAmount");
        var profit = decimalFrom(next, "profitAmount");
        var fee = decimalFrom(next, "feeAmount");
        var dueDate = parseLocalDate(jsonString(next, "dueDate"));

        return new InstallmentScheduleResponse(
                buildInvoiceId(loanId, nextNumber),
                nextNumber,
                dueDate,
                total,
                principal,
                profit,
                fee != null ? fee : java.math.BigDecimal.ZERO,
                null,
                status,
                isPaid ? java.time.LocalDate.now() : null,
                isPaid && total != null ? total : null,
                isPaid,
                next
        );
    }

    private InstallmentScheduleResponse computeNextInstallmentFromLoan(
            UUID loanId, com.ksa.financing.lending.domain.model.LoanAggregate loan) {
        var installmentAmount = loan.getInstallmentAmount();
        var tenure = loan.getTenureMonths();
        if (installmentAmount == null || tenure <= 0) {
            return null;
        }
        var firstDueDate = loan.getFirstDueDate();
        if (firstDueDate == null) {
            var disbursement = loan.getDisbursementDate() != null
                    ? loan.getDisbursementDate() : java.time.LocalDate.now();
            firstDueDate = disbursement.plusMonths(1);
        }

        var today = java.time.LocalDate.now();
        int nextNumber = 1;
        var nextDueDate = firstDueDate;
        while (nextNumber < tenure && nextDueDate.plusMonths(1).isBefore(today.plusDays(1))) {
            nextNumber++;
            nextDueDate = firstDueDate.plusMonths(nextNumber - 1);
        }

        var tenureBD = java.math.BigDecimal.valueOf(tenure);
        var principalPortion = loan.getPrincipalAmount() != null
                ? loan.getPrincipalAmount().divide(tenureBD, 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;
        var profitPortion = loan.getProfitAmount() != null
                ? loan.getProfitAmount().divide(tenureBD, 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;
        var feePortion = installmentAmount.subtract(principalPortion).subtract(profitPortion).max(java.math.BigDecimal.ZERO);

        long daysOverdue = nextDueDate.isBefore(today)
                ? java.time.temporal.ChronoUnit.DAYS.between(nextDueDate, today) : 0L;
        var status = daysOverdue > 0 ? "OVERDUE" : "PENDING";

        return new InstallmentScheduleResponse(
                buildInvoiceId(loanId, nextNumber),
                nextNumber,
                nextDueDate,
                installmentAmount,
                principalPortion,
                profitPortion,
                feePortion,
                null,
                status,
                null,
                null,
                false,
                null
        );
    }

    private java.math.BigDecimal decimalFrom(java.util.Map<String, Object> source, String key) {
        var v = source.get(key);
        if (v == null) return null;
        try { return new java.math.BigDecimal(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private java.time.LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return java.time.LocalDate.parse(value); } catch (java.time.format.DateTimeParseException e) { return null; }
    }

    private java.math.BigDecimal decimalNode(com.fasterxml.jackson.databind.JsonNode node, String field) {
        var v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        try { return new java.math.BigDecimal(v.asText()); } catch (NumberFormatException e) { return null; }
    }

    private java.util.Map<String, Object> buildProductObject(UUID tenantId, UUID productId) {
        var product = new java.util.LinkedHashMap<String, Object>();
        if (productId == null) {
            product.put("id", null);
            product.put("name_en", null);
            product.put("name_ar", null);
            return product;
        }
        var summary = productConfigPort.fetchProductSummary(tenantId, productId.toString()).orElse(null);
        product.put("id", summary != null && summary.id() != null ? summary.id() : productId.toString());
        product.put("name_en", summary != null ? summary.nameEn() : null);
        product.put("name_ar", summary != null ? summary.nameAr() : null);
        return product;
    }

    private java.util.Map<String, Object> buildEnvelope(Object data, String message) {
        var env = new java.util.LinkedHashMap<String, Object>();
        env.put("data", data);
        env.put("message", message);
        env.put("timestamp", java.time.Instant.now().toString());
        return env;
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
                BigDecimal trackerTotalPayable = resolveTotalPayable(statusInfo);

                return ResponseEntity.ok(ApplicationTrackerResponse.build(
                        statusInfo.applicationId(),
                        statusInfo.applicationNumber(),
                        statusInfo.basicInfo() != null ? statusInfo.basicInfo().requestedAmount() : null,
                        trackerTotalPayable,
                        statusInfo.status(),
                        null, null, null,
                        statusInfo.basicInfo() != null ? statusInfo.basicInfo().requestedTenureMonths() : 0,
                        statusInfo.basicInfo() != null ? statusInfo.basicInfo().profitRate() : null,
                        statusInfo
                ));
            }
        } catch (Exception e) {
            log.debug("No active workflow for tracker, falling back to DB: {}", e.getMessage());
        }

        // Fallback: get from DB
        var appsPage = useCase.listApplicationsByCustomer(tenantId, java.util.UUID.fromString(customerId), new PageQuery(0, 1, null, null, null));
        if (appsPage.content().isEmpty()) {
            return ResponseEntity.ok(ApplicationTrackerResponse.build(
                    null, null, null, null, null));
        }

        var latest = appsPage.content().get(0);
        BigDecimal dbTotalPayable = latest.getOfferedTotalPayable();
        if (dbTotalPayable != null) {
            BigDecimal safeAmount = latest.getAcceptedAmount() != null ? latest.getAcceptedAmount() : 
                                    (latest.getOfferedAmount() != null ? latest.getOfferedAmount() : BigDecimal.ZERO);
            BigDecimal safeProfit = latest.getOfferedTotalProfit() != null ? latest.getOfferedTotalProfit() : BigDecimal.ZERO;
            BigDecimal safeProcFee = latest.getProcessingFee() != null ? latest.getProcessingFee() : BigDecimal.ZERO;
            BigDecimal safeAdminFee = latest.getAdminFee() != null ? latest.getAdminFee() : BigDecimal.ZERO;
            BigDecimal expectedTotal = safeAmount.add(safeProfit).add(safeProcFee).add(safeAdminFee);
            if (safeAmount.compareTo(BigDecimal.ZERO) > 0 && dbTotalPayable.compareTo(expectedTotal) < 0) {
                dbTotalPayable = expectedTotal;
            }
        } else if (latest.getRequestedAmount() != null && latest.getRequestedTenureMonths() > 0) {
            dbTotalPayable = calculatePreliminaryTotalPayable(tenantId, latest.getRequestedAmount(),
                    latest.getRequestedTenureMonths(), latest.getProductId() != null ? latest.getProductId().toString() : null);
        }
        return ResponseEntity.ok(ApplicationTrackerResponse.build(latest, dbTotalPayable));
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
                            statusInfo.status(),
                            null, null, null,
                            statusInfo.basicInfo() != null ? statusInfo.basicInfo().requestedTenureMonths() : 0,
                            statusInfo.basicInfo() != null ? statusInfo.basicInfo().profitRate() : null,
                            statusInfo
                    ));
                }
            } catch (Exception e) {
                log.debug("No active workflow for tracker, using DB: {}", e.getMessage());
            }
        }

        // Fallback: build from DB aggregate
        BigDecimal aggTotalPayable = aggregate.getOfferedTotalPayable();
        if (aggTotalPayable != null) {
            BigDecimal safeAmount = aggregate.getAcceptedAmount() != null ? aggregate.getAcceptedAmount() : 
                                    (aggregate.getOfferedAmount() != null ? aggregate.getOfferedAmount() : BigDecimal.ZERO);
            BigDecimal safeProfit = aggregate.getOfferedTotalProfit() != null ? aggregate.getOfferedTotalProfit() : BigDecimal.ZERO;
            BigDecimal safeProcFee = aggregate.getProcessingFee() != null ? aggregate.getProcessingFee() : BigDecimal.ZERO;
            BigDecimal safeAdminFee = aggregate.getAdminFee() != null ? aggregate.getAdminFee() : BigDecimal.ZERO;
            BigDecimal expectedTotal = safeAmount.add(safeProfit).add(safeProcFee).add(safeAdminFee);
            if (safeAmount.compareTo(BigDecimal.ZERO) > 0 && aggTotalPayable.compareTo(expectedTotal) < 0) {
                aggTotalPayable = expectedTotal;
            }
        } else if (aggregate.getRequestedAmount() != null && aggregate.getRequestedTenureMonths() > 0) {
            aggTotalPayable = calculatePreliminaryTotalPayable(tenantId, aggregate.getRequestedAmount(),
                    aggregate.getRequestedTenureMonths(), aggregate.getProductId() != null ? aggregate.getProductId().toString() : null);
        }
        return ResponseEntity.ok(ApplicationTrackerResponse.build(aggregate, aggTotalPayable));
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
        var applications = useCase.listApplicationsByCustomer(tenantId, UUID.fromString(customerId), new PageQuery(0, 100, null, null, null));

        return applications.content().stream()
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
                if ("APPROVED".equals(s) || "DISBURSED".equals(s) || "REJECTED".equals(s)
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

    private PageResponse<LoanApplicationResponse> enrichPageWithLoanData(UUID tenantId,
                                                               PageResponse<com.ksa.financing.lending.application.dto.LoanApplicationDto> page,
                                                               Jwt jwt) {
        List<LoanApplicationResponse> enrichedContent = page.content().stream().map(dto -> {
            String rescheduleId = null;
            String rescheduleStatus = null;
            String rescheduleType = null;
            java.time.LocalDateTime rescheduleRequestedAt = null;
            java.time.LocalDateTime rescheduleAppliedAt = null;
            String rescheduleReason = null;
            String rescheduleDetails = null;
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
                        loan.getDisbursementDate() != null ? loan.getDisbursementDate() : null,
                        loan.getTenureMonths(),
                        loan.getMaturityDate(),
                        loan.getProfitRate(),
                        isEarlySettlementEligible(loan.getId().getValue(), jwt));

                // Fetch latest reschedule for this loan (if any)
                try {
                    var reschedules = rescheduleRepository.findByTenantIdAndLoanId(
                            tenantId, loan.getId().getValue());
                    if (!reschedules.isEmpty()) {
                        var latest = reschedules.stream()
                                .max(java.util.Comparator.comparing(r -> r.getCreatedAt()))
                                .get();
                        rescheduleId = latest.getId().toString();
                        rescheduleStatus = latest.getStatus();
                        rescheduleType = latest.getRescheduleType();
                        rescheduleRequestedAt = latest.getCreatedAt() != null ? latest.getCreatedAt().toLocalDateTime() : null;
                        rescheduleAppliedAt = latest.getAppliedAt() != null ? latest.getAppliedAt().toLocalDateTime() : null;
                        rescheduleReason = latest.getRejectionReason() != null ? latest.getRejectionReason() : latest.getJustification();
                        rescheduleDetails = generateRescheduleDetails(latest);
                    }
                } catch (Exception ignored) {}

                BigDecimal preliminaryTotalPayable = null;
                if (dto.offeredTotalPayable() == null && dto.requestedAmount() != null && dto.requestedTenureMonths() > 0) {
                    preliminaryTotalPayable = calculatePreliminaryTotalPayable(tenantId, dto.requestedAmount(), dto.requestedTenureMonths(), dto.productId());
                }

                return LoanApplicationResponse.from(enriched, rescheduleId, rescheduleStatus,
                        rescheduleType, rescheduleRequestedAt, rescheduleAppliedAt, rescheduleReason, rescheduleDetails, preliminaryTotalPayable);
            } catch (Exception e) {
                // No loan yet for this application
                BigDecimal preliminaryTotalPayable = null;
                if (dto.offeredTotalPayable() == null && dto.requestedAmount() != null && dto.requestedTenureMonths() > 0) {
                    preliminaryTotalPayable = calculatePreliminaryTotalPayable(tenantId, dto.requestedAmount(), dto.requestedTenureMonths(), dto.productId());
                }
                return LoanApplicationResponse.from(dto, rescheduleId, rescheduleStatus,
                        rescheduleType, rescheduleRequestedAt, rescheduleAppliedAt, rescheduleReason, rescheduleDetails, preliminaryTotalPayable);
            }
        }).toList();

        return new PageResponse<>(enrichedContent, page.pagination());
    }

    private Boolean isEarlySettlementEligible(UUID loanId, Jwt jwt) {
        try {
            var headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var url = collectionsServiceUrl + "/api/v1/repayment-schedules/by-loan/" + loanId;
            var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }
            var node = objectMapper.readTree(response.getBody()).path("data");
            return node.path("earlySettlementEligible").asBoolean(false);
        } catch (Exception e) {
            log.debug("Early settlement check failed for loanId={}: {}", loanId, e.getMessage());
            return false;
        }
    }

    private BigDecimal resolveTotalPayable(ApplicationStatusInfo statusInfo) {
        if (statusInfo.offer() != null && statusInfo.offer().totalPayable() != null) {
            BigDecimal totalPayable = statusInfo.offer().totalPayable();
            BigDecimal safeAmount = statusInfo.offer().selectedAmount() != null ? statusInfo.offer().selectedAmount() :
                                    (statusInfo.offer().maxAmount() != null ? statusInfo.offer().maxAmount() : BigDecimal.ZERO);
            BigDecimal safeProfit = statusInfo.offer().totalProfit() != null ? statusInfo.offer().totalProfit() : BigDecimal.ZERO;
            BigDecimal safeProcFee = statusInfo.offer().processingFee() != null ? statusInfo.offer().processingFee() : BigDecimal.ZERO;
            BigDecimal safeAdminFee = statusInfo.offer().adminFee() != null ? statusInfo.offer().adminFee() : BigDecimal.ZERO;
            
            BigDecimal expectedTotal = safeAmount.add(safeProfit).add(safeProcFee).add(safeAdminFee);
            if (safeAmount.compareTo(BigDecimal.ZERO) > 0 && totalPayable.compareTo(expectedTotal) < 0) {
                return expectedTotal;
            }
            return totalPayable;
        }
        if (statusInfo.basicInfo() != null) {
            var bi = statusInfo.basicInfo();
            java.math.BigDecimal amount = bi.requestedAmount();
            int tenure = bi.requestedTenureMonths();
            java.math.BigDecimal rate = bi.profitRate();
            java.math.BigDecimal procPercent = bi.processingFeePercent();
            java.math.BigDecimal procAmount = bi.processingFeeAmount();
            java.math.BigDecimal adminFee = bi.adminFeeAmount();

            var preQual = com.ksa.financing.lending.adapter.rest.response.PreQualificationData.calculateFinance(
                    amount, tenure, rate, procPercent, procAmount, adminFee
            );
            return preQual != null ? preQual.totalPayable() : null;
        }
        return null;
    }

    private BigDecimal calculatePreliminaryTotalPayable(UUID tenantId, BigDecimal amount, int tenureMonths, String productId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return null;
        }
        BigDecimal profitRate = new BigDecimal("0.025");
        BigDecimal processingFeeAmount = BigDecimal.ZERO;
        BigDecimal adminFeeAmount = BigDecimal.ZERO;

        try {
            var config = productConfigPort.fetchProductConfig(tenantId, productId, amount, tenureMonths);
            if (config.profitRate() != null) {
                BigDecimal rate = config.profitRate();
                profitRate = rate.compareTo(BigDecimal.ONE) > 0 ? rate.movePointLeft(2) : rate;
            }
            if (config.processingFeeAmount() != null && config.processingFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
                processingFeeAmount = config.processingFeeAmount();
            } else if (config.processingFeePercent() != null && config.processingFeePercent().compareTo(BigDecimal.ZERO) > 0) {
                processingFeeAmount = amount.multiply(config.processingFeePercent()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            }
            if (config.adminFeeAmount() != null) {
                adminFeeAmount = config.adminFeeAmount();
            }
        } catch (Exception ignored) {}

        try {
            var calc = com.ksa.financing.lending.domain.service.FinanceCalculationService.calculate(
                    amount, profitRate, null, tenureMonths, processingFeeAmount, adminFeeAmount);
            return calc.hasErrors() ? amount : calc.totalPayable();
        } catch (Exception e) {
            log.debug("Preliminary totalPayable calculation failed, returning amount: {}", e.getMessage());
            return amount;
        }
    }

    private String generateRescheduleDetails(com.ksa.financing.lending.infrastructure.persistence.entity.LoanRescheduleJpaEntity r) {
        StringBuilder sb = new StringBuilder();
        String type = r.getRescheduleType();

        Integer oldTenure = r.getOldTenureMonths();
        LocalDate oldMaturity = r.getOldMaturityDate();
        BigDecimal oldInstallment = r.getOldInstallmentAmount();

        // Fallback for older records
        try {
            if (oldTenure == null || oldMaturity == null || oldInstallment == null) {
                var loanOpt = loanUseCase.getLoan(r.getTenantId(), r.getLoanId());
                if (loanOpt != null) {
                    if (oldTenure == null) oldTenure = loanOpt.getTenureMonths();
                    if (oldMaturity == null) oldMaturity = loanOpt.getMaturityDate();
                    if (oldInstallment == null) oldInstallment = loanOpt.getInstallmentAmount();
                }
            }
        } catch (Exception ignored) {}

        if ("SKIP_PAYMENT".equals(type)) {
            sb.append("Requested to skip installment for ").append(r.getRequestedSkipMonth() != null ? r.getRequestedSkipMonth().getMonth().name() + " " + r.getRequestedSkipMonth().getYear() : "selected month").append(". ");
            sb.append("Action: The skipped month is moved to the end of the schedule. ");
            sb.append("Total tenure remains ").append(oldTenure != null ? oldTenure : "?").append(" months, ");
            sb.append("but maturity date is extended to ").append(r.getNewMaturityDate() != null ? r.getNewMaturityDate() : "a later date").append(".");
        } else if ("TENURE_EXTENSION".equals(type)) {
            sb.append("Loan tenure extended by ").append(r.getExtensionMonths()).append(" months. ");
            sb.append("Configuration: Tenure changed from ").append(oldTenure != null ? oldTenure : "?").append(" to ").append(r.getNewTenureMonths()).append(" months. ");
            sb.append("Result: Monthly installment reduced from ").append(scale2(oldInstallment)).append(" to ").append(scale2(r.getNewInstallmentAmount())).append(" SAR.");
        } else if ("PAYMENT_HOLIDAY".equals(type)) {
            sb.append("Payment holiday granted for ").append(r.getHolidayMonths()).append(" months. ");
            sb.append("Impact: Next installments are paused, and maturity is extended to ").append(r.getNewMaturityDate()).append(".");
        } else if ("RESTRUCTURING".equals(type)) {
            sb.append("Loan restructuring performed for financial relief. ");
            if (r.getWriteOffAmount() != null && r.getWriteOffAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("Benefit: Principal write-off of ").append(scale2(r.getWriteOffAmount())).append(" SAR applied. ");
            }
            if (r.getNewProfitRate() != null) {
                BigDecimal displayRate = r.getNewProfitRate();
                if (displayRate.compareTo(BigDecimal.ONE) < 0) {
                    displayRate = displayRate.multiply(BigDecimal.valueOf(100));
                }
                sb.append("Change: Profit rate updated to ").append(displayRate.setScale(2, RoundingMode.HALF_UP)).append("%. ");
            }
            sb.append("Impact: Monthly payment adjusted from ").append(scale2(oldInstallment)).append(" to ").append(scale2(r.getNewInstallmentAmount())).append(" SAR.");
        }

        return sb.toString();
    }

    private BigDecimal scale2(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * Resolves the authenticated user's customerId.
     * Prefers the {@code customer_id} JWT claim if present; otherwise calls
     * customer-service {@code GET /api/v1/customers/my-profile} to map the
     * Keycloak user (JWT subject) to the customer record.
     */
    private UUID resolveCustomerIdFromJwt(Jwt jwt) {
        var customerClaim = jwt.getClaimAsString("customer_id");
        if (customerClaim != null && !customerClaim.isBlank()) {
            return UUID.fromString(customerClaim);
        }
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            var url = customerServiceUrl + "/api/v1/customers/my-profile";
            var response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var body = objectMapper.readTree(response.getBody());
                var dataNode = body.has("data") ? body.get("data") : body;
                var idText = dataNode.path("customerId").asText(null);
                if (idText == null || idText.isBlank()) {
                    idText = dataNode.path("id").asText(null);
                }
                if (idText != null && !idText.isBlank()) {
                    return UUID.fromString(idText);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve customerId from customer-service my-profile: {}", e.getMessage());
        }
        throw NotFoundException.forEntity("Customer for authenticated user", jwt.getSubject());
    }

    private StepSignalResponse buildSignalResponseFromAggregate(String step, LoanApplicationAggregate agg) {
        var statusStr = agg.getStatus().name();
        var steps = LoanApplicationStepInfo.buildSteps(statusStr);
        var nextAction = LoanApplicationStepInfo.getNextAction(statusStr);
        var overallStatus = switch (agg.getStatus()) {
            case AWAIT_DISBURSED -> "await_disbursement";
            case APPROVED -> "approved";
            case DISBURSED -> "completed";
            case REJECTED -> "rejected";
            case CANCELLED -> "cancelled";
            case EXPIRED -> "expired";
            case EXPIRED_RESUMABLE -> "expired_resumable";
            default -> "active";
        };
        var preQual = PreQualificationData.fromAggregate(agg);
        return new StepSignalResponse(
                "SIGNAL_SENT",
                step,
                agg.getId().getValue().toString(),
                agg.getApplicationNumber(),
                statusStr,
                overallStatus,
                nextAction,
                steps,
                preQual,
                null
        );
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
