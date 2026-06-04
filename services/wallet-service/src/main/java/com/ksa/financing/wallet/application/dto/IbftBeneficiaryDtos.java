package com.ksa.financing.wallet.application.dto;

import com.ksa.financing.wallet.domain.model.IbftBeneficiary;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class IbftBeneficiaryDtos {

    private IbftBeneficiaryDtos() {}

    public record AddIbftBeneficiaryRequest(
            String nickname,
            @NotBlank String beneficiaryName,
            @NotBlank String institutionNumber,
            @NotBlank String transit,
            @NotBlank String accountNumber,
            String bankName,
            String currency
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
