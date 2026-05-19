package com.ksa.financing.wallet.application.dto;

import java.util.List;
import java.util.UUID;

public record CheckRecipientsResponse(List<RecipientCheckItem> results) {

    public record RecipientCheckItem(
            boolean found,
            UUID walletId,
            String walletNumber,
            String iban,
            String maskedName,
            String maskedMobile,
            String currency,
            String walletStatus,
            String userStatus,
            boolean canReceive,
            String reason
    ) {}
}
