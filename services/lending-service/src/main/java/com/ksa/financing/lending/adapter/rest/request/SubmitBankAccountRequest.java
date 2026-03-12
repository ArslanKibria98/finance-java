package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Step 2: Submit bank account for disbursement")
public record SubmitBankAccountRequest(

        @NotBlank(message = "Bank code is required")
        @Schema(description = "Bank code (e.g., RJHI, SABB)")
        String bankCode,

        @Schema(description = "Bank name")
        String bankName,

        @NotBlank(message = "IBAN is required")
        @Schema(description = "Saudi IBAN (SA + 22 digits)")
        String iban,

        @Schema(description = "Account number")
        String accountNumber
) {}
