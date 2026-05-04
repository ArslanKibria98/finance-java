package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Step 2: Submit bank account for disbursement")
public record SubmitBankAccountRequest(

        @NotBlank(message = "LENDING.BANK_ACCOUNT.BANK_CODE_REQUIRED")
        @Schema(description = "Bank code (e.g., RJHI, SABB)")
        String bankCode,

        @Schema(description = "Bank name")
        String bankName,

        @NotBlank(message = "LENDING.BANK_ACCOUNT.IBAN_REQUIRED")
        @Schema(description = "Saudi IBAN (SA + 22 digits)")
        String iban,

        @Schema(description = "Account number")
        String accountNumber
) {}
