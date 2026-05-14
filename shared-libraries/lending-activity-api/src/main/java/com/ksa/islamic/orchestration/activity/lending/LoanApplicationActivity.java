package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core lending activity — handles loan application CRUD and internal operations.
 * Runs inside lending-service.
 */
@ActivityInterface
public interface LoanApplicationActivity {

    // ══════════ APPLICATION LIFECYCLE ══════════

    @ActivityMethod
    CreateApplicationResult createDraftApplication(CreateDraftInput input);

    @ActivityMethod
    void saveBasicInfo(SaveBasicInfoInput input);

    @ActivityMethod
    void saveBankAccount(SaveBankAccountInput input);

    @ActivityMethod
    void recordSimahConsent(RecordConsentInput input);

    @ActivityMethod
    void saveEligibilityResult(SaveEligibilityInput input);

    @ActivityMethod
    void saveOffer(SaveOfferInput input);

    @ActivityMethod
    void lockAcceptedOffer(LockOfferInput input);

    @ActivityMethod
    void updateStatus(UpdateStatusInput input);

    @ActivityMethod
    void setDisbursementDelay(SetDisbursementDelayInput input);

    @ActivityMethod
    void cancelApplication(CancelInput input);

    // ══════════ THIRD-PARTY RESULT PERSISTENCE ══════════

    @ActivityMethod
    void saveSafeWatchResult(SaveSafeWatchInput input);

    @ActivityMethod
    void saveMasdarResult(SaveMasdarInput input);

    @ActivityMethod
    void saveAmlDeclaration(SaveAmlDeclarationInput input);

    @ActivityMethod
    void saveNabaNotification(SaveNabaInput input);

    @ActivityMethod
    void savePaymentGuardResult(SavePaymentGuardInput input);

    @ActivityMethod
    void saveOtpAttempt(SaveOtpAttemptInput input);

    @ActivityMethod
    void saveIvrAttempt(SaveIvrAttemptInput input);

    // ══════════ LOAN CREATION ══════════

    @ActivityMethod
    LoanCreationResult createLoan(LoanCreationInput input);

    @ActivityMethod
    void generateAmortizationSchedule(AmortizationInput input);

    @ActivityMethod
    void markLoanDisbursed(MarkDisbursedInput input);

    // ══════════ DTOs ══════════

    record CreateDraftInput(
            String tenantId,
            String customerId,
            String nationalId,
            BigDecimal monthlyIncome,
            BigDecimal totalExpenses,
            BigDecimal existingLiabilities,
            int adultDependents,
            int childDependents,
            BigDecimal foodGroceries,
            BigDecimal utilities,
            BigDecimal healthcare,
            BigDecimal communication,
            BigDecimal housingRent,
            BigDecimal clothingEssentials,
            BigDecimal education,
            BigDecimal transportation,
            String productId,
            BigDecimal requestedAmount,
            Integer requestedTenureMonths,
            String purposeOfFinance,
            String purposeOfFinanceOther,
            String createdBy,
            String workflowId
    ) {}

    record CreateApplicationResult(
            String applicationId,
            String applicationNumber
    ) {}

    record SaveBasicInfoInput(
            String tenantId,
            String applicationId,
            String productId,
            String productCode,
            String productName,
            String shariaStructure,
            BigDecimal requestedAmount,
            int requestedTenureMonths,
            String purposeOfFinance,
            BigDecimal profitRate,
            BigDecimal processingFeePercent,
            BigDecimal processingFeeAmount,
            BigDecimal adminFeeAmount,
            String partnerId,
            String leadId,
            String updatedBy
    ) {}

    record SaveBankAccountInput(
            String tenantId,
            String applicationId,
            String bankCode,
            String bankName,
            String iban,
            String accountNumber,
            String accountHolder,
            boolean verified,
            String updatedBy
    ) {}

    record RecordConsentInput(
            String tenantId,
            String applicationId,
            boolean simahConsent,
            String consentTimestamp,
            String updatedBy
    ) {}

    record SaveEligibilityInput(
            String tenantId,
            String applicationId,
            boolean eligible,
            int creditScore,
            String simahReferenceId,
            BigDecimal verifiedSalary,
            BigDecimal dbrBefore,
            BigDecimal dbrAfter,
            BigDecimal maxEligibleAmount,
            String rejectionReason,
            String updatedBy
    ) {}

    record SaveOfferInput(
            String tenantId,
            String applicationId,
            BigDecimal maxAmount,
            BigDecimal monthlyInstallment,
            BigDecimal annualProfitRate,
            int tenureMonths,
            BigDecimal totalPayable,
            BigDecimal totalProfit,
            BigDecimal processingFee,
            BigDecimal adminFee,
            String updatedBy
    ) {}

    record LockOfferInput(
            String tenantId,
            String applicationId,
            BigDecimal selectedAmount,
            BigDecimal monthlyInstallment,
            BigDecimal totalPayable,
            BigDecimal totalProfit,
            BigDecimal processingFee,
            BigDecimal adminFee,
            String updatedBy
    ) {}

    record UpdateStatusInput(
            String tenantId,
            String applicationId,
            String targetStatus,
            String updatedBy
    ) {}

    record SetDisbursementDelayInput(
            String tenantId,
            String applicationId,
            int disbursementDurationHours,
            String scheduledAt,   // ISO-8601 string, nullable
            String updatedBy
    ) {}

    record CancelInput(
            String tenantId,
            String applicationId,
            String reason,
            String cancelledBy
    ) {}

    record LoanCreationInput(
            String tenantId,
            String applicationId,
            String customerId,
            String productId,
            String productCode,
            String shariaStructure,
            BigDecimal principalAmount,
            BigDecimal profitAmount,
            BigDecimal processingFee,
            BigDecimal adminFee,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal installmentAmount
    ) {}

    record LoanCreationResult(
            String loanId,
            String loanNumber
    ) {}

    record AmortizationInput(
            String tenantId,
            String loanId,
            BigDecimal principalAmount,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal installmentAmount,
            String shariaStructure
    ) {}

    record MarkDisbursedInput(
            String tenantId,
            String loanId,
            String fineractLoanId
    ) {}

    // ══════════ THIRD-PARTY PERSISTENCE DTOs ══════════

    record SaveSafeWatchInput(
            String tenantId,
            String applicationId,
            String sessionId,
            String status,
            String updatedBy
    ) {}

    record SaveMasdarInput(
            String tenantId,
            String applicationId,
            String employerName,
            String employmentSector,
            String employmentStatus,
            BigDecimal basicSalary,
            BigDecimal totalSalary,
            String employmentStartDate,
            String updatedBy
    ) {}

    record SaveAmlDeclarationInput(
            String tenantId,
            String applicationId,
            String updatedBy
    ) {}

    record SaveNabaInput(
            String tenantId,
            String applicationId,
            String updatedBy
    ) {}

    record SavePaymentGuardInput(
            String tenantId,
            String applicationId,
            String sessionId,
            String status,
            String updatedBy
    ) {}

    record SaveOtpAttemptInput(
            String tenantId,
            String applicationId,
            String updatedBy
    ) {}

    record SaveIvrAttemptInput(
            String tenantId,
            String applicationId,
            String updatedBy
    ) {}
}
