package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddBeneficiaryRequest(
        UUID walletId,
        @NotBlank @Size(max = 100) String nickname,
        @NotBlank @Size(max = 200) String beneficiaryName,
        @NotBlank @Size(max = 34)  String iban,
        @Size(max = 20)  String bankCode,
        @Size(max = 120) String bankName
) {}
