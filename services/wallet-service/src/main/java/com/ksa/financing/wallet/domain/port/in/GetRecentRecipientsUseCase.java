package com.ksa.financing.wallet.domain.port.in;

import java.util.List;
import java.util.UUID;

public interface GetRecentRecipientsUseCase {

    List<RecentRecipient> getRecentRecipients(UUID tenantId, UUID customerId);

    record RecentRecipient(
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
