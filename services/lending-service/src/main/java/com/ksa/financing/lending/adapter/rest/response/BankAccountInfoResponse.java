package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bank accounts lookup result for Step 2")
public record BankAccountInfoResponse(

        @Schema(description = "Source of bank accounts: CUSTOMER_SERVICE or TARABUT")
        String source,

        @Schema(description = "List of bank accounts found")
        List<BankAccountItem> accounts
) {

    @Schema(description = "Individual bank account details")
    public record BankAccountItem(
            @Schema(description = "Bank name (e.g., Al Rajhi Bank)")
            String bankName,

            @Schema(description = "Bank code (e.g., RJHI)")
            String bankCode,

            @Schema(description = "Full IBAN")
            String iban,

            @Schema(description = "Account holder name")
            String accountHolderName,

            @Schema(description = "Account type (e.g., SAVINGS, CURRENT)")
            String accountType,

            @Schema(description = "Whether this is a salary account")
            boolean salaryAccount,

            @Schema(description = "Verification status (VERIFIED, PENDING_VERIFICATION, etc.)")
            String status
    ) {}
}
