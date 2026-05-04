package com.ksa.financing.wallet.application.dto;

import java.util.UUID;

public record RecipientLookupResponse(
        boolean found,
        UUID walletId,
        String walletNumber,
        String maskedName,
        String maskedMobile,
        String currency,
        String walletStatus,
        String userStatus,
        boolean canReceive,
        String reason
) {}
