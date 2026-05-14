package com.ksa.financing.wallet.application.dto;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(
        UUID id,
        UUID customerId,
        UUID walletId,
        String nickname,
        String beneficiaryName,
        String iban,
        String bankCode,
        String bankName,
        boolean verified,
        boolean active,
        Instant createdAt
) {}
