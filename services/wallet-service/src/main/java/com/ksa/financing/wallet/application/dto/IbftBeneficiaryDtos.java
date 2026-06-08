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
     * Account-validation request — shared by both flows:
     *   - {@code type = "phone"} (FT): {@code accountNumber} carries the recipient's MOBILE number.
     *     The customer is verified by mobile and their wallet account number is sent to Scotia.
     *   - {@code type = "account"} (IBFT): {@code accountNumber} is a real bank account number and
     *     is sent straight to Scotia as-is (no customer lookup).
     *   - {@code type} omitted → auto-detected from the value format.
     * transit is NOT supplied — it is derived from the first 5 digits of the resolved account
     * number. fullName is optional.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidateAccountRequest(
            @NotBlank String accountNumber,
            @NotBlank String institutionNumber,
            String fullName,
            String currency,
            String type
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
