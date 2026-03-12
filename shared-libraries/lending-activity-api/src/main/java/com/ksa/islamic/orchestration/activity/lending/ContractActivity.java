package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles contract generation and signing operations.
 * Used in Step 5 (Sign Contract) of the loan application workflow.
 */
@ActivityInterface
public interface ContractActivity {

    @ActivityMethod
    ContractGenerationResult generateContracts(ContractGenerationInput input);

    @ActivityMethod
    void recordContractConsent(ContractConsentInput input);

    @ActivityMethod
    void sendSigningOtp(SendOtpInput input);

    @ActivityMethod
    OtpVerifyResult verifySigningOtp(VerifyOtpInput input);

    // ══════════ DTOs ══════════

    record ContractGenerationInput(
            String tenantId,
            String applicationId,
            String customerId,
            String nationalId,
            String customerName,
            String shariaStructure,
            BigDecimal approvedAmount,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal monthlyInstallment,
            BigDecimal totalPayable,
            BigDecimal totalProfit,
            String iban,
            String bankName
    ) {}

    record ContractGenerationResult(
            List<GeneratedDocument> documents,
            String expiresAt,           // 24-hour deadline
            boolean success
    ) {}

    record GeneratedDocument(
            String documentId,
            String type,                // COMMODITY_CERTIFICATE, FINANCING_CONTRACT, E_PROMISSORY, SALE_AUTHORIZATION
            String name,
            String status               // GENERATED
    ) {}

    record ContractConsentInput(
            String tenantId,
            String applicationId,
            boolean authorizeDigitalSignature,
            boolean authorizeSellCommodity,
            boolean wantPhysicalDelivery,
            String consentTimestamp,
            String customerId
    ) {}

    record SendOtpInput(
            String tenantId,
            String customerId,
            String mobileNumber,
            String purpose              // "CONTRACT_SIGNING"
    ) {}

    record VerifyOtpInput(
            String tenantId,
            String customerId,
            String otpCode,
            String sessionId
    ) {}

    record OtpVerifyResult(
            boolean verified,
            int remainingAttempts,
            String errorMessage
    ) {}
}
