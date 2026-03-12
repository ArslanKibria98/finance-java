package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Signal-driven Temporal workflow matching the mobile app UI stepper:
 *
 * <pre>
 * Basic Information → Add Bank Account → Checking Eligibility → Accept Offer → Sign Contract → Done
 * </pre>
 *
 * <p>Customer drives the flow via signals at each step. Backend processes
 * each signal, calls required services, and waits for the next signal.</p>
 *
 * <p>SAGA compensation: On failure after offer acceptance, previous financial
 * actions are compensated (contracts cancelled, commodity trade reversed).</p>
 */
@WorkflowInterface
public interface LoanApplicationWorkflow {

    // ══════════════════════════════════════════════════════════════
    // WORKFLOW METHOD — starts the flow
    // ══════════════════════════════════════════════════════════════

    @WorkflowMethod
    LoanApplicationResult execute(InitiateRequest request);

    // ══════════════════════════════════════════════════════════════
    // SIGNALS — Customer sends data at each UI step
    // ══════════════════════════════════════════════════════════════

    /** Step 1: Customer selects product, amount, purpose */
    @SignalMethod
    void submitBasicInfo(BasicInfoSignal signal);

    /** Step 2: Customer selects bank and provides IBAN */
    @SignalMethod
    void submitBankAccount(BankAccountSignal signal);

    /** Step 3: Customer gives SIMAH consent to check credit */
    @SignalMethod
    void giveSimahConsent(SimahConsentSignal signal);

    /** Step 4: Customer accepts (or adjusts) the offer */
    @SignalMethod
    void acceptOffer(AcceptOfferSignal signal);

    /** Step 5a: Customer signs contract with authorizations */
    @SignalMethod
    void signContract(SignContractSignal signal);

    /** Step 5b: Customer enters OTP for signing verification */
    @SignalMethod
    void verifySigningOtp(OtpVerifySignal signal);

    /** Step 5c: IVR service sends callback after call verification */
    @SignalMethod
    void ivrCallback(IvrCallbackSignal signal);

    // ══════════════════════════════════════════════════════════════
    // QUERIES — UI reads current state
    // ══════════════════════════════════════════════════════════════

    /** Returns current step info (stepperIndex, stepName, status, substep) */
    @QueryMethod
    StepInfo getCurrentStep();

    /** Returns full application status with all collected data */
    @QueryMethod
    ApplicationStatusInfo getApplicationStatus();

    /** Returns calculated offer details (after eligibility passes) */
    @QueryMethod
    OfferDetails getOfferDetails();

    /** Returns contract documents (after offer accepted) */
    @QueryMethod
    ContractInfo getContractDocuments();

    // ══════════════════════════════════════════════════════════════
    // INITIATE REQUEST (starts workflow)
    // ══════════════════════════════════════════════════════════════

    record InitiateRequest(
            String tenantId,
            String customerId,
            String nationalId,
            String mobileNumber,
            String createdBy,
            // Product selection
            String productId,
            BigDecimal requestedAmount,         // null for ELIGIBILITY_CHECK mode
            int requestedTenureMonths,          // 0 for ELIGIBILITY_CHECK mode
            String purposeOfFinance,            // null for ELIGIBILITY_CHECK mode
            String purposeOfFinanceOther,       // when purposeOfFinance = OTHER
            // Individual expense categories (BRD Section 3.3)
            BigDecimal foodGroceries,
            BigDecimal utilities,
            BigDecimal healthcare,
            BigDecimal communication,
            BigDecimal housingRent,
            BigDecimal clothingEssentials,
            BigDecimal education,
            BigDecimal transportation,
            // Eligibility answers (dynamic fields: field_key → value)
            Map<String, String> eligibilityAnswers
    ) {}

    // ══════════════════════════════════════════════════════════════
    // SIGNAL RECORDS
    // ══════════════════════════════════════════════════════════════

    record BasicInfoSignal(
            String productId,
            String productCode,
            String productName,
            String shariaStructure,
            BigDecimal requestedAmount,
            int requestedTenureMonths,
            String purposeOfFinance,
            BigDecimal requestedProfitRate,
            String partnerId,
            String leadId
    ) {}

    record BankAccountSignal(
            String bankCode,
            String bankName,
            String iban,
            String accountNumber
    ) {}

    record SimahConsentSignal(
            boolean consentGiven
    ) {}

    record AcceptOfferSignal(
            boolean accepted,
            BigDecimal selectedAmount  // can be <= maxEligibleAmount
    ) {}

    record SignContractSignal(
            boolean authorizeDigitalSignature,
            boolean authorizeSellCommodity,
            boolean wantPhysicalDelivery
    ) {}

    record OtpVerifySignal(
            String otpCode
    ) {}

    record IvrCallbackSignal(
            boolean verified,
            String callId,
            String verificationStatus
    ) {}

    // ══════════════════════════════════════════════════════════════
    // QUERY RESPONSE RECORDS
    // ══════════════════════════════════════════════════════════════

    record StepInfo(
            int stepperIndex,          // 1-5 matching UI stepper
            String stepName,           // "Basic Information", "Add Bank Account", etc.
            String status,             // ApplicationStatus name
            String subStep,            // e.g., "AWAITING_OTP", "AWAITING_IVR"
            boolean canProceed,        // true if current step is complete
            String errorMessage        // null if no error
    ) {}

    record ApplicationStatusInfo(
            String workflowId,
            String applicationId,
            String applicationNumber,
            String status,
            int stepperIndex,
            String stepName,
            // Collected data per step
            BasicInfoData basicInfo,
            BankAccountData bankAccount,
            EligibilityData eligibility,
            OfferDetails offer,
            ContractInfo contract,
            LoanInfo loan,
            // Timing
            String createdAt,
            String expiresAt
    ) {}

    record BasicInfoData(
            String productId,
            String productCode,
            String productName,
            String shariaStructure,
            BigDecimal requestedAmount,
            int requestedTenureMonths,
            String purposeOfFinance
    ) {}

    record BankAccountData(
            String bankCode,
            String bankName,
            String iban,
            String accountHolder,
            boolean verified
    ) {}

    record EligibilityData(
            boolean eligible,
            int creditScore,
            BigDecimal dbrBefore,
            BigDecimal dbrAfter,
            BigDecimal maxEligibleAmount,
            BigDecimal verifiedSalary,
            String rejectionReason
    ) {}

    record OfferDetails(
            BigDecimal maxAmount,
            BigDecimal selectedAmount,
            BigDecimal monthlyInstallment,
            BigDecimal annualProfitRate,
            int tenureMonths,
            BigDecimal totalPayable,
            BigDecimal totalProfit,
            BigDecimal processingFee,
            BigDecimal adminFee
    ) {}

    record ContractInfo(
            List<ContractDocument> documents,
            String expiresAt,
            long remainingSeconds,
            boolean allSigned
    ) {}

    record ContractDocument(
            String type,        // COMMODITY_CERTIFICATE, FINANCING_CONTRACT, E_PROMISSORY, SALE_AUTHORIZATION
            String name,
            String documentId,
            String status       // GENERATED, VIEWED, SIGNED
    ) {}

    record LoanInfo(
            String loanId,
            String loanNumber,
            String status,
            BigDecimal disbursedAmount,
            String disbursementDate
    ) {}

    // ══════════════════════════════════════════════════════════════
    // RESULT
    // ══════════════════════════════════════════════════════════════

    record LoanApplicationResult(
            String workflowId,
            String applicationId,
            String applicationNumber,
            String loanId,
            String loanNumber,
            String status,
            String failureReason
    ) {}
}
