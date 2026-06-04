package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import java.math.BigDecimal;
import java.util.UUID;

/** Admin: read / configure the platform transaction-limit bounds for a tenant. */
public interface ManageWalletLimitBoundsUseCase {

    WalletLimitBounds getBounds(UUID tenantId);

    WalletLimitBounds updateBounds(UpdateBoundsCommand command);

    record UpdateBoundsCommand(
            UUID tenantId,
            BigDecimal minDailyLimit,
            BigDecimal maxDailyLimit,
            BigDecimal minMonthlyLimit,
            BigDecimal maxMonthlyLimit,
            BigDecimal minYearlyLimit,
            BigDecimal maxYearlyLimit,
            BigDecimal defaultDailyLimit,
            BigDecimal defaultMonthlyLimit,
            BigDecimal defaultYearlyLimit,
            UUID updatedBy) {}
}
