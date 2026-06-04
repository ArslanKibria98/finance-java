package com.ksa.financing.wallet.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/** Read the wallet's current transaction limits together with today's / this month's usage. */
public interface GetWalletLimitUseCase {

    WalletLimitView getLimit(UUID tenantId, UUID walletId);

    record WalletLimitView(
            UUID walletId,
            BigDecimal dailyLimit,
            BigDecimal monthlyLimit,
            BigDecimal yearlyLimit,
            BigDecimal todaySpent,
            BigDecimal monthSpent,
            BigDecimal yearSpent,
            BigDecimal dailyRemaining,
            BigDecimal monthlyRemaining,
            BigDecimal yearlyRemaining,
            String currency) {}
}
