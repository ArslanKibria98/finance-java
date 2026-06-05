package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Customer: raise a request to change their wallet transaction limits, and list their requests. */
public interface RequestWalletLimitChangeUseCase {

    WalletLimitChangeRequest request(RequestCommand command);

    List<WalletLimitChangeRequest> listForWallet(UUID tenantId, UUID walletId);

    record RequestCommand(
            UUID tenantId,
            UUID walletId,
            BigDecimal requestedSingleLimit,
            BigDecimal requestedDailyLimit,
            BigDecimal requestedWeeklyLimit,
            BigDecimal requestedMonthlyLimit,
            BigDecimal requestedYearlyLimit,
            String reason,
            UUID requestedBy) {}
}
