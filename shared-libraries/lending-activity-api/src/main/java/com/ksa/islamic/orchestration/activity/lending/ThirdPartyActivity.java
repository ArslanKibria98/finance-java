package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

/**
 * Handles third-party integrations via middleware-third-party service.
 * Used for SafeWatch AML, Masdar employment, IBAN verification, commodity trading,
 * IVR calls, e-promissory registration, NABA notification, PaymentGuard.
 */
@ActivityInterface
public interface ThirdPartyActivity {

    // ══════════ SAFEWATCH AML SCREENING (BRD Phase 1) ══════════

    @ActivityMethod
    SafeWatchResult screenSafeWatch(SafeWatchInput input);

    // ══════════ MASDAR EMPLOYMENT VERIFICATION (BRD Phase 5) ══════════

    @ActivityMethod
    MasdarResult verifyEmployment(MasdarInput input);

    // ══════════ AML DECLARATION (BRD Phase 6) ══════════

    @ActivityMethod
    AmlDeclarationResult recordAmlDeclaration(AmlDeclarationInput input);

    // ══════════ BANK / IBAN ══════════

    @ActivityMethod
    IbanVerificationResult verifyIban(IbanVerificationInput input);

    // ══════════ COMMODITY TRADE (Tawarruq) ══════════

    @ActivityMethod
    CommodityTradeResult executeCommodityTrade(CommodityTradeInput input);

    @ActivityMethod
    void reverseCommodityTrade(String tradeId);

    // ══════════ IVR VERIFICATION ══════════

    @ActivityMethod
    IvrInitiateResult initiateIvrCall(IvrInitiateInput input);

    // ══════════ E-PROMISSORY ══════════

    @ActivityMethod
    EPromissoryResult registerEPromissory(EPromissoryInput input);

    // ══════════ NABA NOTIFICATION (BRD Phase 8 - Absher messaging) ══════════

    @ActivityMethod
    NabaResult sendNabaNotification(NabaInput input);

    // ══════════ PAYMENT GUARD (Fraud Check) ══════════

    @ActivityMethod
    PaymentGuardResult checkPaymentGuard(PaymentGuardInput input);

    // ══════════ EMDHA DIGITAL SIGNATURE (BRD V1.8 — Contract Signing) ══════════

    @ActivityMethod
    EmdhaSignResult signWithEmdha(EmdhaSignInput input);

    // ══════════ DAKHLI INCOME VERIFICATION (BRD V1.8 — Rescheduling) ══════════

    @ActivityMethod
    DakhliResult fetchDakhliIncome(DakhliInput input);

    // ══════════ DTOs ══════════

    record IbanVerificationInput(
            String tenantId,
            String iban,
            String nationalId,
            String expectedName
    ) {}

    record IbanVerificationResult(
            boolean verified,
            String accountHolder,
            String bankName,
            String bankCode,
            String rejectionReason
    ) {}

    record CommodityTradeInput(
            String tenantId,
            String applicationId,
            String shariaStructure,
            BigDecimal amount,
            String customerId
    ) {}

    record CommodityTradeResult(
            String tradeId,
            String commodityType,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            String certificateId,
            boolean success
    ) {}

    record IvrInitiateInput(
            String tenantId,
            String applicationId,
            String mobileNumber,
            String customerName,
            BigDecimal loanAmount
    ) {}

    record IvrInitiateResult(
            String callId,
            String status,
            boolean initiated
    ) {}

    record EPromissoryInput(
            String tenantId,
            String loanId,
            String loanNumber,
            String customerId,
            String nationalId,
            BigDecimal totalAmount,
            int tenureMonths,
            BigDecimal installmentAmount
    ) {}

    record EPromissoryResult(
            String registrationId,
            String promissoryNumber,
            boolean registered
    ) {}

    // ══════════ SAFEWATCH DTOs ══════════

    record SafeWatchInput(
            String tenantId,
            String nationalId,
            String customerName,
            String applicationId
    ) {}

    record SafeWatchResult(
            String sessionId,
            String status,
            boolean cleared,
            String matchDetails
    ) {}

    // ══════════ MASDAR DTOs ══════════

    record MasdarInput(
            String tenantId,
            String nationalId,
            String applicationId
    ) {}

    record MasdarResult(
            String employerName,
            String employmentSector,
            String employmentStatus,
            BigDecimal basicSalary,
            BigDecimal totalSalary,
            String employmentStartDate,
            boolean verified
    ) {}

    // ══════════ AML DECLARATION DTOs ══════════

    record AmlDeclarationInput(
            String tenantId,
            String applicationId,
            String customerId,
            String nationalId,
            boolean pep,
            boolean sanctionedCountry,
            boolean sourceOfFundsConfirmed
    ) {}

    record AmlDeclarationResult(
            boolean accepted,
            String referenceId
    ) {}

    // ══════════ NABA DTOs ══════════

    record NabaInput(
            String tenantId,
            String nationalId,
            String mobileNumber,
            String applicationNumber,
            BigDecimal loanAmount,
            String messageType
    ) {}

    record NabaResult(
            boolean sent,
            String referenceId
    ) {}

    // ══════════ PAYMENT GUARD DTOs ══════════

    record PaymentGuardInput(
            String tenantId,
            String applicationId,
            String customerId,
            String nationalId,
            BigDecimal amount,
            String iban
    ) {}

    record PaymentGuardResult(
            String sessionId,
            String status,
            boolean approved,
            String riskLevel
    ) {}

    // ══════════ EMDHA DTOs ══════════

    record EmdhaSignInput(
            String tenantId,
            String applicationId,
            String customerId,
            String nationalId,
            String contractType,
            String documentContent
    ) {}

    record EmdhaSignResult(
            String signatureId,
            String signedDocumentId,
            boolean signed,
            String signatureStatus
    ) {}

    // ══════════ DAKHLI DTOs ══════════

    record DakhliInput(
            String tenantId,
            String nationalId,
            String applicationId
    ) {}

    record DakhliResult(
            BigDecimal verifiedIncome,
            String employerName,
            String employmentStatus,
            String sector,
            boolean success,
            String errorMessage
    ) {}
}
