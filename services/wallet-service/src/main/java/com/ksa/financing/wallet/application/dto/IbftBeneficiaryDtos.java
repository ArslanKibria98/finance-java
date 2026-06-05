package com.ksa.financing.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ksa.financing.wallet.domain.model.IbftBeneficiary;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class IbftBeneficiaryDtos {

    private IbftBeneficiaryDtos() {}

    /**
     * transit is NOT supplied — it is derived from the first 5 digits of {@code accountNumber}.
     * A stray {@code transit} sent by older clients is ignored (not rejected).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddIbftBeneficiaryRequest(
            String nickname,
            @NotBlank String beneficiaryName,
            @NotBlank String institutionNumber,
            @NotBlank String accountNumber,
            String bankName,
            String currency
    ) {}

    /**
     * Account-validation request. transit is NOT supplied — it is derived from the
     * first 5 digits of accountNumber. fullName is optional. The account must first
     * exist in our wallets (exact account_number match) before Scotia is called.
     */
    public record ValidateAccountRequest(
            @NotBlank String accountNumber,
            @NotBlank String institutionNumber,
            String fullName,
            String currency
    ) {}

    public record ValidateAccountResponse(
            boolean valid,
            String status,
            String accountNumber,
            String transit,
            String institutionNumber,
            String message,
            String scotiaRef,
            Object scotia
    ) {}

    public record IbftBeneficiaryResponse(
            UUID id,
            String nickname,
            String beneficiaryName,
            String institutionNumber,
            String transit,
            String accountNumber,
            String bankName,
            String currency,
            boolean validated,
            boolean active,
            Instant createdAt
    ) {
        public static IbftBeneficiaryResponse from(IbftBeneficiary b) {
            return new IbftBeneficiaryResponse(
                    b.getId(), b.getNickname(), b.getBeneficiaryName(), b.getInstitutionNumber(),
                    b.getTransit(), b.getAccountNumber(), b.getBankName(), b.getCurrency(),
                    b.isValidated(), b.isActive(), b.getCreatedAt());
        }
    }
}
