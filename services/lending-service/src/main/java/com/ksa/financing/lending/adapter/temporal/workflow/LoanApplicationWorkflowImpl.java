package com.ksa.financing.lending.adapter.temporal.workflow;

import com.ksa.islamic.orchestration.activity.lending.*;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Signal-driven loan application workflow matching the mobile app UI stepper:
 * Basic Information → Add Bank Account → Checking Eligibility → Accept Offer → Sign Contract → Done
 *
 * <p>Customer drives the flow via signals at each step. Backend processes
 * each signal, calls required services, and waits for the next signal.</p>
 *
 * <p>SAGA compensation: On failure after offer acceptance, previous financial
 * actions are compensated (contracts cancelled, commodity trade reversed).</p>
 */
public class LoanApplicationWorkflowImpl implements LoanApplicationWorkflow {

    private static final Logger log = Workflow.getLogger(LoanApplicationWorkflowImpl.class);

    // ══════════════════════════════════════════════════════════════
    // TIMEOUTS
    // ══════════════════════════════════════════════════════════════

    private static final Duration STEP_TIMEOUT = Duration.ofHours(24);
    private static final Duration OVERALL_TIMEOUT = Duration.ofDays(7);
    private static final Duration CONTRACT_SIGNING_TIMEOUT = Duration.ofHours(24);
    private static final Duration OTP_TIMEOUT = Duration.ofMinutes(2);   // BRD: 2 minutes
    private static final Duration IVR_TIMEOUT = Duration.ofMinutes(10);
    private static final int MAX_OTP_ATTEMPTS = 3;   // BRD: max 3 OTP attempts
    private static final int MAX_IVR_ATTEMPTS = 3;   // BRD: max 3 IVR attempts

    // ══════════════════════════════════════════════════════════════
    // ACTIVITY STUBS
    // ══════════════════════════════════════════════════════════════

    private final ActivityOptions defaultOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    private final ActivityOptions thirdPartyOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(3))
                    .setMaximumInterval(Duration.ofSeconds(60))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    private final ActivityOptions disbursementOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5))
                    .setMaximumInterval(Duration.ofMinutes(2))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    private final LoanApplicationActivity lendingActivity =
            Workflow.newActivityStub(LoanApplicationActivity.class, defaultOptions);

    private final ProductValidationActivity productActivity =
            Workflow.newActivityStub(ProductValidationActivity.class, defaultOptions);

    private final CustomerValidationActivity customerActivity =
            Workflow.newActivityStub(CustomerValidationActivity.class, defaultOptions);

    private final CreditCheckActivity creditCheckActivity =
            Workflow.newActivityStub(CreditCheckActivity.class, thirdPartyOptions);

    private final ThirdPartyActivity thirdPartyActivity =
            Workflow.newActivityStub(ThirdPartyActivity.class, thirdPartyOptions);

    private final ContractActivity contractActivity =
            Workflow.newActivityStub(ContractActivity.class, defaultOptions);

    private final DisbursementActivity disbursementActivity =
            Workflow.newActivityStub(DisbursementActivity.class, disbursementOptions);

    private final ActivityOptions ledgerOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(5)
                    .build())
            .build();

    private final LedgerActivity ledgerActivity =
            Workflow.newActivityStub(LedgerActivity.class, ledgerOptions);

    // ══════════════════════════════════════════════════════════════
    // WORKFLOW STATE (all mutable, preserved across signals)
    // ══════════════════════════════════════════════════════════════

    // Core identifiers
    private String tenantId;
    private String customerId;
    private String nationalId;
    private String mobileNumber;
    private String createdBy;

    // Application tracking
    private String applicationId;
    private String applicationNumber;
    private String status = "DRAFT";
    private int stepperIndex = 1;
    private String stepName = "Basic Information";
    private String subStep;
    private String errorMessage;

    // Product + eligibility data (from initiate)
    private String productId;
    private BigDecimal requestedAmount;
    private int requestedTenureMonths;
    private String purposeOfFinance;
    private Map<String, String> eligibilityAnswers;
    private BigDecimal monthlyIncome;
    private BigDecimal totalExpenses;
    private BigDecimal existingLiabilities;

    // Step 1: Basic Info
    private BasicInfoSignal basicInfoSignal;
    private boolean basicInfoReceived;
    private BasicInfoData basicInfoData;

    // Step 1 validation results
    private ProductValidationActivity.ProductValidationResult productValidation;
    private CustomerValidationActivity.CustomerValidationResult customerValidation;

    // Step 2: Bank Account
    private BankAccountSignal bankAccountSignal;
    private boolean bankAccountReceived;
    private BankAccountData bankAccountData;

    // Step 3: SIMAH / Eligibility
    private SimahConsentSignal simahConsentSignal;
    private boolean simahConsentReceived;
    private EligibilityData eligibilityData;
    private BigDecimal resolvedProfitRate;  // Set once during eligibility, used everywhere after

    // Step 4: Offer
    private AcceptOfferSignal acceptOfferSignal;
    private boolean offerAccepted;
    private OfferDetails offerDetails;

    // Step 5: Contract
    private SignContractSignal signContractSignal;
    private boolean contractSignalReceived;
    private OtpVerifySignal otpSignal;
    private boolean otpReceived;
    private IvrCallbackSignal ivrSignal;
    private boolean ivrReceived;
    private ContractInfo contractInfo;

    // Loan result
    private LoanInfo loanInfo;

    // BRD Phase tracking
    private int otpAttemptCount;
    private int ivrAttemptCount;

    // Expense categories (from initiate)
    private BigDecimal foodGroceries;
    private BigDecimal utilities;
    private BigDecimal healthcare;
    private BigDecimal communication;
    private BigDecimal housingRent;
    private BigDecimal clothingEssentials;
    private BigDecimal education;
    private BigDecimal transportation;

    // SAGA compensation tracking
    private String commodityTradeId;
    private boolean contractsGenerated;

    // ══════════════════════════════════════════════════════════════
    // MAIN WORKFLOW METHOD
    // ══════════════════════════════════════════════════════════════

    @Override
    public LoanApplicationResult execute(InitiateRequest request) {
        String workflowId = Workflow.getInfo().getWorkflowId();
        log.info("Starting loan application workflow: {}", workflowId);

        // Store request context
        this.tenantId = request.tenantId();
        this.customerId = request.customerId();
        this.nationalId = request.nationalId();
        this.mobileNumber = request.mobileNumber();
        this.createdBy = request.createdBy();
        this.productId = request.productId();
        this.requestedAmount = request.requestedAmount();
        this.requestedTenureMonths = request.requestedTenureMonths();
        this.purposeOfFinance = request.purposeOfFinance();
        this.eligibilityAnswers = request.eligibilityAnswers() != null ? request.eligibilityAnswers() : Map.of();

        // Extract financial data from eligibility answers
        this.monthlyIncome = parseDecimal(eligibilityAnswers.get("monthly_income"));
        this.totalExpenses = parseDecimal(eligibilityAnswers.get("total_expenses"));
        this.existingLiabilities = parseDecimal(eligibilityAnswers.get("existing_liabilities"));

        // Individual expense categories from initiate request
        this.foodGroceries = request.foodGroceries();
        this.utilities = request.utilities();
        this.healthcare = request.healthcare();
        this.communication = request.communication();
        this.housingRent = request.housingRent();
        this.clothingEssentials = request.clothingEssentials();
        this.education = request.education();
        this.transportation = request.transportation();

        try {
            boolean received;

            // ── CREATE DRAFT APPLICATION ──
            var createResult = lendingActivity.createDraftApplication(
                    new LoanApplicationActivity.CreateDraftInput(
                            tenantId, customerId, nationalId,
                            monthlyIncome, totalExpenses, existingLiabilities,
                            0, 0,
                            foodGroceries, utilities, healthcare, communication,
                            housingRent, clothingEssentials, education, transportation,
                            createdBy, workflowId
                    )
            );
            applicationId = createResult.applicationId();
            applicationNumber = createResult.applicationNumber();
            log.info("Draft application created: {}", applicationNumber);

            // Pre-populate basicInfoData from initiate request so tracker shows requestedAmount immediately
            if (requestedAmount != null) {
                basicInfoData = new BasicInfoData(
                        productId, null, null, null,
                        requestedAmount, requestedTenureMonths, purposeOfFinance, null
                );
            }

            // ── BRD PHASE 1: SAFEWATCH AML SCREENING ──
            processSafeWatchScreening();

            // ══════════ STEP 1: BASIC INFORMATION ══════════
            updateStep(1, "Basic Information", "DRAFT", "AWAITING_INPUT");

            // Wait for basic info signal from user (POST /{customerId}/basic-info)
            received = Workflow.await(STEP_TIMEOUT, () -> basicInfoReceived);
            if (!received) {
                return expireApplication(workflowId, "Step 1 timed out: Basic information not submitted");
            }

            updateStep(1, "Basic Information", "DRAFT", "PROCESSING");
            processBasicInfoFromInitiate();

            // ══════════ STEP 2: ADD BANK ACCOUNT ══════════
            updateStep(2, "Add Bank Account", "BANK_ACCOUNT_PENDING", "AWAITING_INPUT");
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "BANK_ACCOUNT_PENDING", createdBy));

            received = Workflow.await(STEP_TIMEOUT, () -> bankAccountReceived);
            if (!received) {
                return expireApplication(workflowId, "Step 2 timed out: Bank account not submitted");
            }

            processBankAccount();

            // ── BRD PHASE 5: MASDAR EMPLOYMENT VERIFICATION ──
            processMasdarVerification();

            // ── BRD PHASE 6: AML DECLARATION ──
            processAmlDeclaration();

            // ══════════ STEP 3: CHECKING ELIGIBILITY ══════════
            updateStep(3, "Checking Eligibility", "BANK_ACCOUNT_VERIFIED", "AWAITING_CONSENT");
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "BANK_ACCOUNT_VERIFIED", createdBy));

            // Wait for SIMAH consent
            received = Workflow.await(STEP_TIMEOUT, () -> simahConsentReceived);
            if (!received) {
                return expireApplication(workflowId, "Step 3 timed out: SIMAH consent not given");
            }

            if (!simahConsentSignal.consentGiven()) {
                return cancelApplication(workflowId, "Customer declined SIMAH consent");
            }

            processEligibilityCheck();

            // ══════════ STEP 4: ACCEPT OFFER ══════════
            updateStep(4, "Accept Offer", "OFFER_PRESENTED", "AWAITING_ACCEPTANCE");
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "OFFER_PRESENTED", createdBy));

            received = Workflow.await(STEP_TIMEOUT, () -> offerAccepted);
            if (!received) {
                return expireApplication(workflowId, "Step 4 timed out: Offer not accepted");
            }

            if (!acceptOfferSignal.accepted()) {
                return cancelApplication(workflowId, "Customer rejected the offer");
            }

            // ── BRD: PAYMENT GUARD FRAUD CHECK ──
            processPaymentGuardCheck();

            processOfferAcceptance();

            // ══════════ STEP 5: SIGN CONTRACT ══════════
            updateStep(5, "Sign Contract", "CONTRACT_PENDING", "GENERATING_CONTRACTS");
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "CONTRACT_PENDING", createdBy));

            processContractGeneration();

            // 5a: Wait for contract signing authorizations
            subStep = "AWAITING_SIGNATURE";
            received = Workflow.await(CONTRACT_SIGNING_TIMEOUT, () -> contractSignalReceived);
            if (!received) {
                return compensateAndExpire(workflowId, "Contract signing timed out (24 hours)");
            }

            processContractSigning();

            // 5b: OTP Verification with retry loop (BRD: max 3 attempts, 2 min timeout each)
            status = "OTP_VERIFICATION";
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "OTP_VERIFICATION", createdBy));

            boolean otpVerifiedOk = false;
            while (otpAttemptCount < MAX_OTP_ATTEMPTS && !otpVerifiedOk) {
                subStep = "AWAITING_OTP";
                otpReceived = false;
                otpSignal = null;

                received = Workflow.await(OTP_TIMEOUT, () -> otpReceived);
                if (!received) {
                    otpAttemptCount++;
                    lendingActivity.saveOtpAttempt(new LoanApplicationActivity.SaveOtpAttemptInput(
                            tenantId, applicationId, createdBy));
                    if (otpAttemptCount >= MAX_OTP_ATTEMPTS) {
                        return compensateAndExpire(workflowId, "OTP verification timed out after " + MAX_OTP_ATTEMPTS + " attempts");
                    }
                    errorMessage = "OTP timed out. Attempt " + otpAttemptCount + " of " + MAX_OTP_ATTEMPTS;
                    // Re-send OTP for next attempt
                    contractActivity.sendSigningOtp(new ContractActivity.SendOtpInput(
                            tenantId, customerId, mobileNumber, "CONTRACT_SIGNING"));
                    continue;
                }

                otpAttemptCount++;
                lendingActivity.saveOtpAttempt(new LoanApplicationActivity.SaveOtpAttemptInput(
                        tenantId, applicationId, createdBy));
                otpVerifiedOk = processOtpVerificationWithRetry();
                if (!otpVerifiedOk && otpAttemptCount >= MAX_OTP_ATTEMPTS) {
                    return compensateAndExpire(workflowId, "OTP verification failed after " + MAX_OTP_ATTEMPTS + " attempts");
                }
            }

            // ── BRD PHASE 8: NABA NOTIFICATION (after OTP verified) ──
            processNabaNotification();

            // 5c: IVR Verification with retry loop (BRD: max 3 attempts)
            status = "IVR_VERIFICATION";
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "IVR_VERIFICATION", createdBy));

            boolean ivrVerifiedOk = false;
            while (ivrAttemptCount < MAX_IVR_ATTEMPTS && !ivrVerifiedOk) {
                subStep = "AWAITING_IVR";
                ivrReceived = false;
                ivrSignal = null;

                received = Workflow.await(IVR_TIMEOUT, () -> ivrReceived);
                if (!received) {
                    ivrAttemptCount++;
                    lendingActivity.saveIvrAttempt(new LoanApplicationActivity.SaveIvrAttemptInput(
                            tenantId, applicationId, createdBy));
                    if (ivrAttemptCount >= MAX_IVR_ATTEMPTS) {
                        return compensateAndExpire(workflowId, "IVR verification timed out after " + MAX_IVR_ATTEMPTS + " attempts");
                    }
                    errorMessage = "IVR timed out. Attempt " + ivrAttemptCount + " of " + MAX_IVR_ATTEMPTS;
                    // Re-initiate IVR for next attempt
                    thirdPartyActivity.initiateIvrCall(new ThirdPartyActivity.IvrInitiateInput(
                            tenantId, applicationId, mobileNumber,
                            customerValidation.fullName(),
                            offerDetails.selectedAmount() != null ? offerDetails.selectedAmount() : basicInfoSignal.requestedAmount()));
                    continue;
                }

                ivrAttemptCount++;
                lendingActivity.saveIvrAttempt(new LoanApplicationActivity.SaveIvrAttemptInput(
                        tenantId, applicationId, createdBy));
                ivrVerifiedOk = processIvrVerificationWithRetry();
                if (!ivrVerifiedOk && ivrAttemptCount >= MAX_IVR_ATTEMPTS) {
                    return compensateAndExpire(workflowId, "IVR verification failed after " + MAX_IVR_ATTEMPTS + " attempts");
                }
            }

            // ══════════ PHASE 6: LOAN CREATION & DISBURSEMENT ══════════
            updateStep(5, "Sign Contract", "CONTRACT_SIGNED", "CREATING_LOAN");
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "CONTRACT_SIGNED", createdBy));

            // Product-configured disbursement delay (hours). 0 = immediate (default).
            int disbursementDelayHours = productValidation != null
                    ? productValidation.disbursementDurationHours() : 0;
            if (disbursementDelayHours > 0) {
                log.info("Disbursement delay active: waiting {} hour(s) before disbursement for application {}",
                        disbursementDelayHours, applicationNumber);
                var scheduledAt = Workflow.currentTimeMillis() + disbursementDelayHours * 3_600_000L;
                lendingActivity.setDisbursementDelay(new LoanApplicationActivity.SetDisbursementDelayInput(
                        tenantId, applicationId, disbursementDelayHours,
                        Instant.ofEpochMilli(scheduledAt).toString(), createdBy));
                updateStep(5, "Sign Contract", "CONTRACT_SIGNED", "WAITING_DISBURSEMENT");
                Workflow.sleep(Duration.ofHours(disbursementDelayHours));
                log.info("Disbursement delay elapsed for application {}, proceeding to disbursement",
                        applicationNumber);
            }

            processLoanCreationAndDisbursement(workflowId);

            // ══════════ DONE ══════════
            status = "APPROVED";
            subStep = "COMPLETED";
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "APPROVED", createdBy));

            log.info("Loan application workflow completed. Application: {}, Loan: {}",
                    applicationNumber, loanInfo != null ? loanInfo.loanNumber() : "N/A");

            return new LoanApplicationResult(
                    workflowId, applicationId, applicationNumber,
                    loanInfo != null ? loanInfo.loanId() : null,
                    loanInfo != null ? loanInfo.loanNumber() : null,
                    "APPROVED", null);

        } catch (ApplicationFailure af) {
            throw af;
        } catch (Exception e) {
            log.error("Loan application workflow failed: {}", e.getMessage(), e);
            return compensateAndFail(workflowId, "Unexpected error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // STEP PROCESSORS
    // ══════════════════════════════════════════════════════════════

    /**
     * Processes basic info from the InitiateRequest (product, amount, purpose already provided at initiate).
     * Validates product, validates customer, then saves basic info — no signal wait needed.
     */
    private void processBasicInfoFromInitiate() {
        log.info("Processing Step 1: Basic Information (from initiate, product={}, amount={})", productId, requestedAmount);

        // Validate product
        productValidation = productActivity.validateProduct(
                new ProductValidationActivity.ProductValidationInput(
                        tenantId, productId, requestedAmount, requestedTenureMonths
                )
        );

        if (!productValidation.valid()) {
            errorMessage = productValidation.rejectionReason();
            throw ApplicationFailure.newNonRetryableFailure(
                    "Product validation failed: " + productValidation.rejectionReason(),
                    "PRODUCT_VALIDATION_FAILED");
        }

        // Validate customer
        customerValidation = customerActivity.validateCustomer(
                new CustomerValidationActivity.CustomerValidationInput(tenantId, customerId)
        );

        if (!customerValidation.valid()) {
            errorMessage = customerValidation.rejectionReason();
            throw ApplicationFailure.newNonRetryableFailure(
                    "Customer validation failed: " + customerValidation.rejectionReason(),
                    "CUSTOMER_VALIDATION_FAILED");
        }

        // Resolve product details from validation (product-service is source of truth)
        String productCode = productValidation.productCode();
        String productName = productValidation.productName();
        String shariaStructure = productValidation.shariaStructure();
        BigDecimal profitRate = productValidation.profitRate();

        // Save basic info to application
        lendingActivity.saveBasicInfo(new LoanApplicationActivity.SaveBasicInfoInput(
                tenantId, applicationId,
                productId,
                productCode,
                productName,
                shariaStructure,
                requestedAmount,
                requestedTenureMonths,
                purposeOfFinance,
                profitRate,
                null, // partnerId
                null, // leadId
                createdBy
        ));

        // Update status
        status = "BASIC_INFO_SUBMITTED";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "BASIC_INFO_SUBMITTED", createdBy));

        basicInfoData = new BasicInfoData(
                productId,
                productCode,
                productName,
                shariaStructure,
                requestedAmount,
                requestedTenureMonths,
                purposeOfFinance,
                profitRate
        );

        // Synthesize basicInfoSignal so downstream methods can use it uniformly
        basicInfoSignal = new BasicInfoSignal(
                productId,
                productCode,
                productName,
                shariaStructure != null ? shariaStructure : "MURABAHA",
                requestedAmount,
                requestedTenureMonths,
                purposeOfFinance,
                profitRate,
                null, // partnerId
                null  // leadId
        );

        errorMessage = null;
        log.info("Step 1 completed (from initiate): product={}, amount={}", productId, requestedAmount);
    }

    private void processBasicInfo() {
        log.info("Processing Step 1: Basic Information");

        // Validate product
        productValidation = productActivity.validateProduct(
                new ProductValidationActivity.ProductValidationInput(
                        tenantId, basicInfoSignal.productId(),
                        basicInfoSignal.requestedAmount(),
                        basicInfoSignal.requestedTenureMonths()
                )
        );

        if (!productValidation.valid()) {
            errorMessage = productValidation.rejectionReason();
            throw ApplicationFailure.newNonRetryableFailure(
                    "Product validation failed: " + productValidation.rejectionReason(),
                    "PRODUCT_VALIDATION_FAILED");
        }

        // Validate customer
        customerValidation = customerActivity.validateCustomer(
                new CustomerValidationActivity.CustomerValidationInput(tenantId, customerId)
        );

        if (!customerValidation.valid()) {
            errorMessage = customerValidation.rejectionReason();
            throw ApplicationFailure.newNonRetryableFailure(
                    "Customer validation failed: " + customerValidation.rejectionReason(),
                    "CUSTOMER_VALIDATION_FAILED");
        }

        // Resolve product details from validation (product-service is source of truth)
        String resolvedProductCode = productValidation.productCode() != null
                ? productValidation.productCode() : basicInfoSignal.productCode();
        String productName = productValidation.productName() != null
                ? productValidation.productName() : basicInfoSignal.productName();
        String resolvedShariaStructure = productValidation.shariaStructure() != null
                ? productValidation.shariaStructure() : basicInfoSignal.shariaStructure();
        BigDecimal profitRate = productValidation.profitRate() != null
                ? productValidation.profitRate() : basicInfoSignal.requestedProfitRate();

        lendingActivity.saveBasicInfo(new LoanApplicationActivity.SaveBasicInfoInput(
                tenantId, applicationId,
                basicInfoSignal.productId(),
                resolvedProductCode,
                productName,
                resolvedShariaStructure,
                basicInfoSignal.requestedAmount(),
                basicInfoSignal.requestedTenureMonths(),
                basicInfoSignal.purposeOfFinance(),
                profitRate,
                basicInfoSignal.partnerId(),
                basicInfoSignal.leadId(),
                createdBy
        ));

        // Update status
        status = "BASIC_INFO_SUBMITTED";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "BASIC_INFO_SUBMITTED", createdBy));

        basicInfoData = new BasicInfoData(
                basicInfoSignal.productId(),
                resolvedProductCode,
                productName,
                resolvedShariaStructure,
                basicInfoSignal.requestedAmount(),
                basicInfoSignal.requestedTenureMonths(),
                basicInfoSignal.purposeOfFinance(),
                profitRate
        );

        errorMessage = null;
        log.info("Step 1 completed: product={}, amount={}", basicInfoSignal.productCode(), basicInfoSignal.requestedAmount());
    }

    private void processBankAccount() {
        log.info("Processing Step 2: Bank Account");

        // Verify IBAN via third-party
        var ibanResult = thirdPartyActivity.verifyIban(
                new ThirdPartyActivity.IbanVerificationInput(
                        tenantId,
                        bankAccountSignal.iban(),
                        nationalId,
                        customerValidation.fullName()
                )
        );

        if (!ibanResult.verified()) {
            errorMessage = ibanResult.rejectionReason();
            // Reset signal so customer can retry
            bankAccountReceived = false;
            bankAccountSignal = null;
            throw ApplicationFailure.newNonRetryableFailure(
                    "IBAN verification failed: " + ibanResult.rejectionReason(),
                    "IBAN_VERIFICATION_FAILED");
        }

        // Save bank account to application
        lendingActivity.saveBankAccount(new LoanApplicationActivity.SaveBankAccountInput(
                tenantId, applicationId,
                bankAccountSignal.bankCode(),
                ibanResult.bankName(),
                bankAccountSignal.iban(),
                bankAccountSignal.accountNumber(),
                ibanResult.accountHolder(),
                true,
                createdBy
        ));

        bankAccountData = new BankAccountData(
                bankAccountSignal.bankCode(),
                ibanResult.bankName(),
                bankAccountSignal.iban(),
                ibanResult.accountHolder(),
                true
        );

        errorMessage = null;
        log.info("Step 2 completed: bank={}, iban=***{}", bankAccountSignal.bankCode(),
                bankAccountSignal.iban().substring(Math.max(0, bankAccountSignal.iban().length() - 4)));
    }

    private void processEligibilityCheck() {
        log.info("Processing Step 3: Eligibility Check");

        // Record SIMAH consent
        String consentTimestamp = Workflow.currentTimeMillis() + "";
        lendingActivity.recordSimahConsent(new LoanApplicationActivity.RecordConsentInput(
                tenantId, applicationId, true, consentTimestamp, createdBy));

        status = "SIMAH_CONSENT_GIVEN";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "SIMAH_CONSENT_GIVEN", createdBy));

        // Move to checking
        status = "ELIGIBILITY_CHECKING";
        subStep = "CREDIT_CHECK";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "ELIGIBILITY_CHECKING", createdBy));

        // 3a: Credit check via SIMAH
        // Use workflow fields (set from initiate) with fallback to basicInfoSignal
        BigDecimal effectiveAmount = requestedAmount;
        int effectiveTenure = requestedTenureMonths;
        if (effectiveAmount == null && basicInfoSignal != null) {
            effectiveAmount = basicInfoSignal.requestedAmount();
        }
        if (effectiveTenure <= 0 && basicInfoSignal != null) {
            effectiveTenure = basicInfoSignal.requestedTenureMonths();
        }

        var creditResult = creditCheckActivity.performCreditCheck(
                new CreditCheckActivity.CreditCheckInput(
                        tenantId, nationalId, customerId,
                        effectiveAmount
                )
        );

        // 3b: Calculate eligibility using product criteria + credit data
        subStep = "CALCULATING_ELIGIBILITY";
        // Use signal profit rate as fallback when product validation returned null
        BigDecimal effectiveProfitRate = productValidation.profitRate() != null
                ? productValidation.profitRate()
                : (basicInfoSignal != null ? basicInfoSignal.requestedProfitRate() : null);
        if (effectiveProfitRate == null) {
            effectiveProfitRate = new BigDecimal("0.12"); // default rate
        }
        resolvedProfitRate = effectiveProfitRate;  // Store for later steps

        // BRD Steps 5-6: Sum declared expenses from 8 categories for affordability check
        // Prefer individual expense fields (from initiate request) over pre-computed totalExpenses
        BigDecimal declaredExpensesTotal = sumExpenses(
                foodGroceries, utilities, healthcare, communication,
                housingRent, clothingEssentials, education, transportation);
        if (declaredExpensesTotal.compareTo(BigDecimal.ZERO) == 0 && totalExpenses != null) {
            declaredExpensesTotal = totalExpenses; // Fallback to pre-computed total
        }

        var eligibilityResult = creditCheckActivity.calculateEligibility(
                new CreditCheckActivity.EligibilityInput(
                        tenantId,
                        creditResult.creditScore(),
                        creditResult.verifiedSalary(),
                        creditResult.existingObligations(),
                        creditResult.hasActiveDefaults(),
                        customerValidation.age(),
                        customerValidation.employmentDurationMonths(),
                        productValidation.minCreditScore(),
                        productValidation.minSalary(),
                        productValidation.minAge(),
                        productValidation.maxAge(),
                        productValidation.minEmploymentMonths(),
                        productValidation.maxDbrPercent(),
                        effectiveAmount,
                        effectiveProfitRate,
                        effectiveTenure,
                        // BRD Affordability fields
                        monthlyIncome,
                        declaredExpensesTotal,
                        existingLiabilities
                )
        );

        // Save eligibility result
        lendingActivity.saveEligibilityResult(new LoanApplicationActivity.SaveEligibilityInput(
                tenantId, applicationId,
                eligibilityResult.eligible(),
                creditResult.creditScore(),
                creditResult.simahReferenceId(),
                creditResult.verifiedSalary(),
                eligibilityResult.dbrBefore(),
                eligibilityResult.dbrAfter(),
                eligibilityResult.maxEligibleAmount(),
                eligibilityResult.rejectionReason(),
                createdBy
        ));

        if (!eligibilityResult.eligible()) {
            // Reject
            status = "REJECTED";
            errorMessage = eligibilityResult.rejectionReason();
            lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                    tenantId, applicationId, "REJECTED", createdBy));
            throw ApplicationFailure.newNonRetryableFailure(
                    "Eligibility check failed: " + eligibilityResult.rejectionReason(),
                    "ELIGIBILITY_FAILED");
        }

        // 3c: Calculate offer
        subStep = "CALCULATING_OFFER";
        var offerCalc = creditCheckActivity.calculateOffer(
                new CreditCheckActivity.ProfitCalculationInput(
                        basicInfoSignal.shariaStructure(),
                        eligibilityResult.maxEligibleAmount(),
                        effectiveProfitRate,
                        basicInfoSignal.requestedTenureMonths(),
                        productValidation.processingFeePercent(),
                        productValidation.adminFeeAmount()
                )
        );

        // Save offer
        lendingActivity.saveOffer(new LoanApplicationActivity.SaveOfferInput(
                tenantId, applicationId,
                eligibilityResult.maxEligibleAmount(),
                offerCalc.monthlyInstallment(),
                effectiveProfitRate,
                basicInfoSignal.requestedTenureMonths(),
                offerCalc.totalPayable(),
                offerCalc.totalProfit(),
                offerCalc.processingFee(),
                offerCalc.adminFee(),
                createdBy
        ));

        // Update state
        status = "ELIGIBILITY_PASSED";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "ELIGIBILITY_PASSED", createdBy));

        eligibilityData = new EligibilityData(
                true,
                creditResult.creditScore(),
                eligibilityResult.dbrBefore(),
                eligibilityResult.dbrAfter(),
                eligibilityResult.maxEligibleAmount(),
                creditResult.verifiedSalary(),
                null
        );

        offerDetails = new OfferDetails(
                eligibilityResult.maxEligibleAmount(),
                null,
                offerCalc.monthlyInstallment(),
                resolvedProfitRate,
                offerCalc.apr(),
                basicInfoSignal.requestedTenureMonths(),
                offerCalc.totalPayable(),
                offerCalc.totalProfit(),
                offerCalc.processingFee(),
                offerCalc.adminFee(),
                offerCalc.firstInstallmentDueDate() != null ? offerCalc.firstInstallmentDueDate().toString() : null
        );

        errorMessage = null;
        log.info("Step 3 completed: eligible=true, maxAmount={}, creditScore={}",
                eligibilityResult.maxEligibleAmount(), creditResult.creditScore());
    }

    private void processOfferAcceptance() {
        log.info("Processing Step 4: Offer Acceptance");

        // Use selectedAmount from signal, fallback to requested amount (NOT maxAmount)
        BigDecimal selectedAmount = acceptOfferSignal.selectedAmount() != null
                ? acceptOfferSignal.selectedAmount()
                : basicInfoSignal.requestedAmount();

        // Validate: selectedAmount must not exceed maxEligibleAmount
        if (offerDetails.maxAmount() != null && selectedAmount.compareTo(offerDetails.maxAmount()) > 0) {
            log.warn("Selected amount {} exceeds max eligible amount {}, capping to max",
                    selectedAmount, offerDetails.maxAmount());
            selectedAmount = offerDetails.maxAmount();
        }

        // Recalculate if customer chose a lower amount
        var recalc = creditCheckActivity.calculateOffer(
                new CreditCheckActivity.ProfitCalculationInput(
                        basicInfoSignal.shariaStructure(),
                        selectedAmount,
                        resolvedProfitRate,
                        basicInfoSignal.requestedTenureMonths(),
                        productValidation.processingFeePercent(),
                        productValidation.adminFeeAmount()
                )
        );

        // Lock the accepted offer
        lendingActivity.lockAcceptedOffer(new LoanApplicationActivity.LockOfferInput(
                tenantId, applicationId,
                selectedAmount,
                recalc.monthlyInstallment(),
                recalc.totalPayable(),
                recalc.totalProfit(),
                createdBy
        ));

        status = "OFFER_ACCEPTED";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "OFFER_ACCEPTED", createdBy));

        // Update offer details with selected amount
        offerDetails = new OfferDetails(
                offerDetails.maxAmount(),
                selectedAmount,
                recalc.monthlyInstallment(),
                resolvedProfitRate,
                recalc.apr(),
                basicInfoSignal.requestedTenureMonths(),
                recalc.totalPayable(),
                recalc.totalProfit(),
                recalc.processingFee(),
                recalc.adminFee(),
                recalc.firstInstallmentDueDate() != null ? recalc.firstInstallmentDueDate().toString() : null
        );

        errorMessage = null;
        log.info("Step 4 completed: selectedAmount={}, installment={}",
                selectedAmount, recalc.monthlyInstallment());
    }

    private void processContractGeneration() {
        log.info("Processing Step 5: Contract Generation");

        BigDecimal selectedAmount = offerDetails.selectedAmount() != null
                ? offerDetails.selectedAmount() : basicInfoSignal.requestedAmount();

        // Execute commodity trade for Tawarruq structure
        if ("TAWARRUQ".equalsIgnoreCase(basicInfoSignal.shariaStructure())
                || "MURABAHA".equalsIgnoreCase(basicInfoSignal.shariaStructure())) {
            var tradeResult = thirdPartyActivity.executeCommodityTrade(
                    new ThirdPartyActivity.CommodityTradeInput(
                            tenantId, applicationId,
                            basicInfoSignal.shariaStructure(),
                            selectedAmount, customerId
                    )
            );
            commodityTradeId = tradeResult.tradeId();
            log.info("Commodity trade executed: tradeId={}", commodityTradeId);
        }

        // Generate contract documents
        var contractResult = contractActivity.generateContracts(
                new ContractActivity.ContractGenerationInput(
                        tenantId, applicationId,
                        customerId, nationalId,
                        customerValidation.fullName(),
                        basicInfoSignal.shariaStructure(),
                        selectedAmount,
                        resolvedProfitRate,
                        basicInfoSignal.requestedTenureMonths(),
                        offerDetails.monthlyInstallment(),
                        offerDetails.totalPayable(),
                        offerDetails.totalProfit(),
                        bankAccountData.iban(),
                        bankAccountData.bankName()
                )
        );

        contractsGenerated = true;

        List<ContractDocument> docs = new ArrayList<>();
        for (var doc : contractResult.documents()) {
            docs.add(new ContractDocument(doc.type(), doc.name(), doc.documentId(), doc.status()));
        }

        contractInfo = new ContractInfo(
                docs,
                contractResult.expiresAt(),
                CONTRACT_SIGNING_TIMEOUT.getSeconds(),
                false
        );

        status = "CONTRACT_SIGNING";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "CONTRACT_SIGNING", createdBy));

        log.info("Contracts generated: {} documents, expires at {}", docs.size(), contractResult.expiresAt());
    }

    private void processContractSigning() {
        log.info("Processing Step 5a: Contract Signing Consent");

        String consentTimestamp = Workflow.currentTimeMillis() + "";
        contractActivity.recordContractConsent(new ContractActivity.ContractConsentInput(
                tenantId, applicationId,
                signContractSignal.authorizeDigitalSignature(),
                signContractSignal.authorizeSellCommodity(),
                signContractSignal.wantPhysicalDelivery(),
                consentTimestamp, customerId
        ));

        // Send OTP for signing verification
        contractActivity.sendSigningOtp(new ContractActivity.SendOtpInput(
                tenantId, customerId, mobileNumber, "CONTRACT_SIGNING"
        ));

        log.info("Contract consent recorded, OTP sent to {}", "***" + mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)));
    }

    private void processOtpVerification() {
        log.info("Processing Step 5b: OTP Verification");

        var otpResult = contractActivity.verifySigningOtp(new ContractActivity.VerifyOtpInput(
                tenantId, customerId, otpSignal.otpCode(), null
        ));

        if (!otpResult.verified()) {
            errorMessage = otpResult.errorMessage();
            // Allow retry — reset signal
            otpReceived = false;
            otpSignal = null;
            throw ApplicationFailure.newNonRetryableFailure(
                    "OTP verification failed: " + otpResult.errorMessage(),
                    "OTP_VERIFICATION_FAILED");
        }

        // Initiate IVR call
        thirdPartyActivity.initiateIvrCall(new ThirdPartyActivity.IvrInitiateInput(
                tenantId, applicationId, mobileNumber,
                customerValidation.fullName(),
                offerDetails.selectedAmount() != null ? offerDetails.selectedAmount() : offerDetails.maxAmount()
        ));

        errorMessage = null;
        log.info("OTP verified, IVR call initiated");
    }

    /**
     * OTP verification with retry support — returns true if verified, false if not.
     */
    private boolean processOtpVerificationWithRetry() {
        log.info("Processing OTP Verification (attempt {})", otpAttemptCount);

        var otpResult = contractActivity.verifySigningOtp(new ContractActivity.VerifyOtpInput(
                tenantId, customerId, otpSignal.otpCode(), null
        ));

        if (!otpResult.verified()) {
            errorMessage = "OTP verification failed (attempt " + otpAttemptCount + "): " + otpResult.errorMessage();
            log.warn(errorMessage);
            return false;
        }

        // OTP verified — initiate IVR call
        thirdPartyActivity.initiateIvrCall(new ThirdPartyActivity.IvrInitiateInput(
                tenantId, applicationId, mobileNumber,
                customerValidation.fullName(),
                offerDetails.selectedAmount() != null ? offerDetails.selectedAmount() : offerDetails.maxAmount()
        ));

        errorMessage = null;
        log.info("OTP verified (attempt {}), IVR call initiated", otpAttemptCount);
        return true;
    }

    private void processIvrVerification() {
        log.info("Processing Step 5c: IVR Verification");

        if (!ivrSignal.verified()) {
            errorMessage = "IVR verification failed: " + ivrSignal.verificationStatus();
            throw ApplicationFailure.newNonRetryableFailure(
                    "IVR verification failed", "IVR_VERIFICATION_FAILED");
        }

        errorMessage = null;
        log.info("IVR verified: callId={}", ivrSignal.callId());
    }

    /**
     * IVR verification with retry support — returns true if verified, false if not.
     */
    private boolean processIvrVerificationWithRetry() {
        log.info("Processing IVR Verification (attempt {})", ivrAttemptCount);

        if (!ivrSignal.verified()) {
            errorMessage = "IVR verification failed (attempt " + ivrAttemptCount + "): " + ivrSignal.verificationStatus();
            log.warn(errorMessage);
            return false;
        }

        errorMessage = null;
        log.info("IVR verified (attempt {}): callId={}", ivrAttemptCount, ivrSignal.callId());
        return true;
    }

    // ══════════════════════════════════════════════════════════════
    // BRD INTEGRATION PROCESSORS
    // ══════════════════════════════════════════════════════════════

    /**
     * BRD Phase 1: SafeWatch AML screening at application initiation.
     * Reject if customer matches sanctions/watchlist.
     */
    private void processSafeWatchScreening() {
        log.info("BRD Phase 1: SafeWatch AML Screening");
        subStep = "SAFEWATCH_SCREENING";

        var result = thirdPartyActivity.screenSafeWatch(new ThirdPartyActivity.SafeWatchInput(
                tenantId, nationalId,
                customerValidation != null ? customerValidation.fullName() : null,
                applicationId
        ));

        // Persist result
        lendingActivity.saveSafeWatchResult(new LoanApplicationActivity.SaveSafeWatchInput(
                tenantId, applicationId, result.sessionId(), result.status(), createdBy
        ));

        if (!result.cleared()) {
            errorMessage = "SafeWatch AML screening failed: " + result.matchDetails();
            throw ApplicationFailure.newNonRetryableFailure(
                    "SafeWatch AML screening failed — customer flagged: " + result.status(),
                    "SAFEWATCH_BLOCKED");
        }

        log.info("SafeWatch AML screening passed: sessionId={}", result.sessionId());
    }

    /**
     * BRD Phase 5: Masdar/GOSI employment verification after bank account.
     * Verifies employment status and salary through government data.
     */
    private void processMasdarVerification() {
        log.info("BRD Phase 5: Masdar Employment Verification");
        subStep = "MASDAR_VERIFICATION";

        var result = thirdPartyActivity.verifyEmployment(new ThirdPartyActivity.MasdarInput(
                tenantId, nationalId, applicationId
        ));

        // Persist result regardless of outcome
        lendingActivity.saveMasdarResult(new LoanApplicationActivity.SaveMasdarInput(
                tenantId, applicationId,
                result.employerName(), result.employmentSector(), result.employmentStatus(),
                result.basicSalary(), result.totalSalary(), result.employmentStartDate(),
                createdBy
        ));

        if (!result.verified()) {
            log.warn("Masdar verification returned unverified — proceeding with self-declared data");
        } else {
            log.info("Masdar verification passed: employer={}, salary={}", result.employerName(), result.totalSalary());
        }
    }

    /**
     * BRD Phase 6: AML self-declaration recording.
     * Records customer's PEP/sanctions/source-of-funds declarations.
     */
    private void processAmlDeclaration() {
        log.info("BRD Phase 6: AML Declaration");
        subStep = "AML_DECLARATION";

        var result = thirdPartyActivity.recordAmlDeclaration(new ThirdPartyActivity.AmlDeclarationInput(
                tenantId, applicationId, customerId, nationalId,
                false, false, true // Default: not PEP, not sanctioned country, funds confirmed
        ));

        // Persist result
        lendingActivity.saveAmlDeclaration(new LoanApplicationActivity.SaveAmlDeclarationInput(
                tenantId, applicationId, createdBy
        ));

        log.info("AML declaration recorded: referenceId={}", result.referenceId());
    }

    /**
     * BRD Phase 8: NABA Absher notification after OTP verification.
     * Sends notification to customer through government messaging system.
     */
    private void processNabaNotification() {
        log.info("BRD Phase 8: NABA Notification");
        subStep = "NABA_NOTIFICATION";

        BigDecimal amount = offerDetails.selectedAmount() != null
                ? offerDetails.selectedAmount() : offerDetails.maxAmount();

        var result = thirdPartyActivity.sendNabaNotification(new ThirdPartyActivity.NabaInput(
                tenantId, nationalId, mobileNumber, applicationNumber, amount, "LOAN_CONFIRMATION"
        ));

        // Persist result
        lendingActivity.saveNabaNotification(new LoanApplicationActivity.SaveNabaInput(
                tenantId, applicationId, createdBy
        ));

        if (!result.sent()) {
            log.warn("NABA notification failed — continuing (non-blocking): applicationId={}", applicationId);
        } else {
            log.info("NABA notification sent: referenceId={}", result.referenceId());
        }
    }

    /**
     * PaymentGuard fraud check before offer lock.
     * Validates the transaction is not fraudulent.
     */
    private void processPaymentGuardCheck() {
        log.info("PaymentGuard Fraud Check");
        subStep = "PAYMENT_GUARD_CHECK";

        BigDecimal amount = acceptOfferSignal.selectedAmount() != null
                ? acceptOfferSignal.selectedAmount() : offerDetails.maxAmount();
        String iban = bankAccountData != null ? bankAccountData.iban() : null;

        var result = thirdPartyActivity.checkPaymentGuard(new ThirdPartyActivity.PaymentGuardInput(
                tenantId, applicationId, customerId, nationalId, amount, iban
        ));

        // Persist result
        lendingActivity.savePaymentGuardResult(new LoanApplicationActivity.SavePaymentGuardInput(
                tenantId, applicationId, result.sessionId(), result.status(), createdBy
        ));

        if (!result.approved()) {
            errorMessage = "PaymentGuard fraud check failed: risk=" + result.riskLevel();
            throw ApplicationFailure.newNonRetryableFailure(
                    "PaymentGuard fraud check rejected: " + result.status(),
                    "PAYMENT_GUARD_REJECTED");
        }

        log.info("PaymentGuard check passed: sessionId={}, risk={}", result.sessionId(), result.riskLevel());
    }

    private void processLoanCreationAndDisbursement(String workflowId) {
        log.info("Processing Phase 6: Loan Creation & Disbursement");

        BigDecimal selectedAmount = offerDetails.selectedAmount() != null
                ? offerDetails.selectedAmount() : basicInfoSignal.requestedAmount();

        // 6a: Update status to LOAN_CREATING
        status = "LOAN_CREATING";
        subStep = "CREATING_LOAN";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "LOAN_CREATING", createdBy));

        // 6b: Create loan in lending-service DB
        var loanResult = lendingActivity.createLoan(new LoanApplicationActivity.LoanCreationInput(
                tenantId, applicationId, customerId,
                basicInfoSignal.productId(),
                basicInfoSignal.productCode() != null ? basicInfoSignal.productCode() : basicInfoSignal.productName(),
                basicInfoSignal.shariaStructure(),
                selectedAmount,
                offerDetails.totalProfit(),
                resolvedProfitRate,
                basicInfoSignal.requestedTenureMonths(),
                offerDetails.monthlyInstallment()
        ));

        // 6c: Generate amortization schedule
        subStep = "GENERATING_SCHEDULE";
        lendingActivity.generateAmortizationSchedule(new LoanApplicationActivity.AmortizationInput(
                tenantId, loanResult.loanId(),
                selectedAmount,
                resolvedProfitRate,
                basicInfoSignal.requestedTenureMonths(),
                offerDetails.monthlyInstallment(),
                basicInfoSignal.shariaStructure()
        ));

        // 6d: Register with Fineract core banking
        subStep = "REGISTERING_FINERACT";
        status = "DISBURSING";
        lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                tenantId, applicationId, "DISBURSING", createdBy));

        var fineractResult = disbursementActivity.registerWithFineract(
                new DisbursementActivity.FineractInput(
                        tenantId, loanResult.loanId(), customerId,
                        basicInfoSignal.productCode(),
                        productValidation != null ? productValidation.fineractProductId() : null,
                        selectedAmount,
                        resolvedProfitRate,
                        basicInfoSignal.requestedTenureMonths(),
                        offerDetails.monthlyInstallment(),
                        basicInfoSignal.shariaStructure()
                )
        );

        // 6e: Register e-promissory note
        subStep = "REGISTERING_EPROMISSORY";
        thirdPartyActivity.registerEPromissory(new ThirdPartyActivity.EPromissoryInput(
                tenantId, loanResult.loanId(), loanResult.loanNumber(),
                customerId, nationalId,
                offerDetails.totalPayable(),
                basicInfoSignal.requestedTenureMonths(),
                offerDetails.monthlyInstallment()
        ));

        // 6f: Pre-disbursement fraud gate (PaymentGuard)
        subStep = "FRAUD_CHECK_PRE_DISBURSE";
        try {
            var fraudResult = thirdPartyActivity.checkPaymentGuard(
                    new ThirdPartyActivity.PaymentGuardInput(
                            tenantId, applicationId, customerId, nationalId,
                            selectedAmount,
                            bankAccountData.iban()
                    ));
            if (!fraudResult.approved()) {
                log.warn("PaymentGuard flagged disbursement: status={}, risk={}, sessionId={}. Proceeding with caution.",
                        fraudResult.status(), fraudResult.riskLevel(), fraudResult.sessionId());
                // Log warning but do NOT block — fraud-service review task will handle manually
            } else {
                log.info("PaymentGuard check PASSED: sessionId={}, riskLevel={}",
                        fraudResult.sessionId(), fraudResult.riskLevel());
            }
        } catch (Exception e) {
            log.warn("PaymentGuard pre-disbursement check failed ({}), proceeding with disbursement.", e.getMessage());
        }

        // 6g: Disburse funds to customer IBAN
        subStep = "DISBURSING_FUNDS";
        var disbursementResult = disbursementActivity.disburseFunds(
                new DisbursementActivity.DisburseFundsInput(
                        tenantId, loanResult.loanId(), loanResult.loanNumber(),
                        selectedAmount,
                        bankAccountData.iban(),
                        bankAccountData.bankCode(),
                        bankAccountData.accountHolder(),
                        workflowId + "-disburse"
                )
        );

        // 6g-2: Post disbursement GL entry via ledger-service → Fineract GL
        // Ledger-service is the single GL bridge. This Temporal activity retries
        // independently — disbursement funds have already been sent successfully.
        subStep = "POSTING_GL_ENTRY";
        try {
            ledgerActivity.postDisbursementGlEntry(new LedgerActivity.DisbursementGlInput(
                    tenantId,
                    loanResult.loanId(),
                    loanResult.loanNumber(),
                    selectedAmount,
                    workflowId + "-gl-disburse",
                    createdBy
            ));
            log.info("Disbursement GL entry posted: loanId={} amount={}", loanResult.loanId(), selectedAmount);
        } catch (Exception e) {
            // GL posting failure must NOT block loan completion — ledger-service recon will catch discrepancies
            log.warn("GL entry posting failed (non-blocking, recon will reconcile): loanId={} error={}",
                    loanResult.loanId(), e.getMessage());
        }

        // 6h: Send completion notification
        subStep = "SENDING_NOTIFICATION";
        disbursementActivity.sendCompletionNotification(
                new DisbursementActivity.NotificationInput(
                        tenantId, customerId, mobileNumber,
                        applicationNumber, loanResult.loanNumber(),
                        selectedAmount, "BOTH"
                )
        );

        // 6i: Mark loan as disbursed in lending DB
        subStep = "MARKING_DISBURSED";
        lendingActivity.markLoanDisbursed(new LoanApplicationActivity.MarkDisbursedInput(
                tenantId, loanResult.loanId(),
                null
        ));

        loanInfo = new LoanInfo(
                loanResult.loanId(),
                loanResult.loanNumber(),
                "ACTIVE",
                selectedAmount,
                Instant.ofEpochMilli(Workflow.currentTimeMillis()).toString()
        );

        log.info("Phase 6 completed: loanId={}, loanNumber={}, disbursementRef={}",
                loanResult.loanId(), loanResult.loanNumber(), disbursementResult.paymentReference());
    }

    // ══════════════════════════════════════════════════════════════
    // SIGNAL HANDLERS
    // ══════════════════════════════════════════════════════════════

    @Override
    public void submitBasicInfo(BasicInfoSignal signal) {
        log.info("Signal received: submitBasicInfo (product={}, amount={})",
                signal.productCode(), signal.requestedAmount());
        this.basicInfoSignal = signal;

        // Update workflow fields so processBasicInfoFromInitiate uses signal data
        if (signal.productId() != null) this.productId = signal.productId();
        if (signal.requestedAmount() != null) this.requestedAmount = signal.requestedAmount();
        if (signal.requestedTenureMonths() > 0) this.requestedTenureMonths = signal.requestedTenureMonths();
        if (signal.purposeOfFinance() != null) this.purposeOfFinance = signal.purposeOfFinance();

        this.basicInfoReceived = true;
    }

    @Override
    public void submitBankAccount(BankAccountSignal signal) {
        log.info("Signal received: submitBankAccount (bank={}, iban=***)",
                signal.bankCode());
        this.bankAccountSignal = signal;
        this.bankAccountReceived = true;
    }

    @Override
    public void giveSimahConsent(SimahConsentSignal signal) {
        log.info("Signal received: giveSimahConsent (consent={})", signal.consentGiven());
        this.simahConsentSignal = signal;
        this.simahConsentReceived = true;
    }

    @Override
    public void acceptOffer(AcceptOfferSignal signal) {
        log.info("Signal received: acceptOffer (accepted={}, amount={})",
                signal.accepted(), signal.selectedAmount());
        this.acceptOfferSignal = signal;
        this.offerAccepted = true;
    }

    @Override
    public void signContract(SignContractSignal signal) {
        log.info("Signal received: signContract (digitalSig={}, sellCommodity={})",
                signal.authorizeDigitalSignature(), signal.authorizeSellCommodity());
        this.signContractSignal = signal;
        this.contractSignalReceived = true;
    }

    @Override
    public void verifySigningOtp(OtpVerifySignal signal) {
        log.info("Signal received: verifySigningOtp");
        this.otpSignal = signal;
        this.otpReceived = true;
    }

    @Override
    public void ivrCallback(IvrCallbackSignal signal) {
        log.info("Signal received: ivrCallback (verified={}, callId={})",
                signal.verified(), signal.callId());
        this.ivrSignal = signal;
        this.ivrReceived = true;
    }

    // ══════════════════════════════════════════════════════════════
    // QUERY HANDLERS
    // ══════════════════════════════════════════════════════════════

    @Override
    public StepInfo getCurrentStep() {
        return new StepInfo(
                stepperIndex,
                stepName,
                status,
                subStep,
                errorMessage == null,
                errorMessage
        );
    }

    @Override
    public ApplicationStatusInfo getApplicationStatus() {
        return new ApplicationStatusInfo(
                Workflow.getInfo().getWorkflowId(),
                applicationId,
                applicationNumber,
                status,
                stepperIndex,
                stepName,
                basicInfoData,
                bankAccountData,
                eligibilityData,
                offerDetails,
                contractInfo,
                loanInfo,
                Workflow.getInfo().getRunId(),
                null
        );
    }

    @Override
    public OfferDetails getOfferDetails() {
        return offerDetails;
    }

    @Override
    public ContractInfo getContractDocuments() {
        return contractInfo;
    }

    // ══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ══════════════════════════════════════════════════════════════

    private void updateStep(int index, String name, String newStatus, String newSubStep) {
        this.stepperIndex = index;
        this.stepName = name;
        this.status = newStatus;
        this.subStep = newSubStep;
        this.errorMessage = null;
    }

    // ══════════════════════════════════════════════════════════════
    // SAGA COMPENSATION
    // ══════════════════════════════════════════════════════════════

    private LoanApplicationResult expireApplication(String workflowId, String reason) {
        log.warn("Application expired: {} — {}", applicationId, reason);
        // Skip DB update if already in a terminal state (CANCELLED, EXPIRED, APPROVED, REJECTED)
        if (status != null && (status.equals("CANCELLED") || status.equals("EXPIRED")
                || status.equals("APPROVED") || status.equals("REJECTED"))) {
            log.info("Application {} already in terminal state {}, skipping expire update", applicationId, status);
        } else {
            try {
                lendingActivity.updateStatus(new LoanApplicationActivity.UpdateStatusInput(
                        tenantId, applicationId, "EXPIRED", createdBy));
            } catch (Exception e) {
                log.error("Failed to expire application: {}", e.getMessage());
            }
        }
        status = "EXPIRED";
        errorMessage = reason;
        return new LoanApplicationResult(workflowId, applicationId, applicationNumber,
                null, null, "EXPIRED", reason);
    }

    private LoanApplicationResult cancelApplication(String workflowId, String reason) {
        log.warn("Application cancelled: {} — {}", applicationId, reason);
        try {
            lendingActivity.cancelApplication(new LoanApplicationActivity.CancelInput(
                    tenantId, applicationId, reason, createdBy));
        } catch (Exception e) {
            log.error("Failed to cancel application: {}", e.getMessage());
        }
        status = "CANCELLED";
        errorMessage = reason;
        return new LoanApplicationResult(workflowId, applicationId, applicationNumber,
                null, null, "CANCELLED", reason);
    }

    /**
     * SAGA compensation after financial actions (post-offer acceptance).
     * Reverses commodity trades and cancels the application.
     */
    private LoanApplicationResult compensateAndExpire(String workflowId, String reason) {
        log.warn("SAGA compensation triggered: {} — {}", applicationId, reason);

        // Compensate: reverse commodity trade
        if (commodityTradeId != null) {
            try {
                thirdPartyActivity.reverseCommodityTrade(commodityTradeId);
                log.info("SAGA: Commodity trade reversed: {}", commodityTradeId);
            } catch (Exception e) {
                log.error("SAGA: Failed to reverse commodity trade {}: {}", commodityTradeId, e.getMessage());
            }
        }

        // Cancel the application
        try {
            lendingActivity.cancelApplication(new LoanApplicationActivity.CancelInput(
                    tenantId, applicationId, "SAGA compensation: " + reason, createdBy));
        } catch (Exception e) {
            log.error("SAGA: Failed to cancel application: {}", e.getMessage());
        }

        status = "EXPIRED";
        errorMessage = reason;
        return new LoanApplicationResult(workflowId, applicationId, applicationNumber,
                null, null, "EXPIRED", reason);
    }

    private LoanApplicationResult compensateAndFail(String workflowId, String reason) {
        log.error("Workflow failed with compensation: {} — {}", applicationId, reason);

        // Same compensation as expire
        if (commodityTradeId != null) {
            try {
                thirdPartyActivity.reverseCommodityTrade(commodityTradeId);
            } catch (Exception e) {
                log.error("SAGA: Failed to reverse commodity trade: {}", e.getMessage());
            }
        }

        try {
            if (applicationId != null) {
                lendingActivity.cancelApplication(new LoanApplicationActivity.CancelInput(
                        tenantId, applicationId, reason, createdBy));
            }
        } catch (Exception e) {
            log.error("SAGA: Failed to cancel application: {}", e.getMessage());
        }

        status = "CANCELLED";
        errorMessage = reason;
        throw ApplicationFailure.newNonRetryableFailure(reason, "LOAN_APPLICATION_FAILED");
    }

    // ══════════════════════════════════════════════════════════════
    // UTILITY HELPERS
    // ══════════════════════════════════════════════════════════════

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * BRD Steps 5-6: Sum the 8 declared expense categories for affordability check.
     * Null-safe — treats null values as zero.
     */
    private static BigDecimal sumExpenses(
            BigDecimal foodGroceries, BigDecimal utilities, BigDecimal healthcare,
            BigDecimal communication, BigDecimal housingRent, BigDecimal clothingEssentials,
            BigDecimal education, BigDecimal transportation) {
        BigDecimal total = BigDecimal.ZERO;
        if (foodGroceries != null) total = total.add(foodGroceries);
        if (utilities != null) total = total.add(utilities);
        if (healthcare != null) total = total.add(healthcare);
        if (communication != null) total = total.add(communication);
        if (housingRent != null) total = total.add(housingRent);
        if (clothingEssentials != null) total = total.add(clothingEssentials);
        if (education != null) total = total.add(education);
        if (transportation != null) total = total.add(transportation);
        return total;
    }
}
