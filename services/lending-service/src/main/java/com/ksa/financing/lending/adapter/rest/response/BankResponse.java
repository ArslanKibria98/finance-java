package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Unified bank/bank-account response shape — identical to
 * customer-service BankAccountResponse so frontend can render
 * both lists with the same code.
 *
 * For the /api/v1/banks reference list: IBAN-related fields are null
 * (a bank by itself does not own an IBAN — accounts do).
 */
@Schema(description = "Bank reference + account details (unified shape)")
public record BankResponse(
        @Schema(description = "Bank id (or bank account id when used in /bank-accounts)")
        UUID id,

        @Schema(description = "Bank name (English)")
        String bankName,

        @Schema(description = "Bank SWIFT/SAMA code")
        String bankCode,

        @Schema(description = "Bank name in English")
        String nameEn,

        @Schema(description = "Bank name in Arabic")
        String nameAr,

        @Schema(description = "Full IBAN (null for /banks reference list)")
        String iban,

        @Schema(description = "Masked IBAN ****1234 (null for /banks reference list)")
        String maskedIban,

        @Schema(description = "Account holder name (null for /banks)")
        String accountHolderName,

        @Schema(description = "Account type (null for /banks)")
        String accountType,

        @Schema(description = "Primary account flag (false for /banks)")
        boolean isPrimary,

        @Schema(description = "Salary account flag (false for /banks)")
        boolean isSalaryAccount,

        @Schema(description = "Salary account flag — Tarabut compat alias")
        boolean salaryAccount,

        @Schema(description = "Status: ACTIVE / INACTIVE / PENDING_VERIFICATION / VERIFIED")
        String status,

        @Schema(description = "Verification timestamp (null for /banks)")
        Instant verifiedAt,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Display sort order (banks reference)")
        int sortOrder
) {}
