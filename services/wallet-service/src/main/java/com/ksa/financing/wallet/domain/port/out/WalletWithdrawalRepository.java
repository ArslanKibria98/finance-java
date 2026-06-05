package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletWithdrawalRepository {

    WalletWithdrawal save(WalletWithdrawal withdrawal);

    /**
     * Sum of withdrawal amounts that count against the transaction limit for a wallet since the
     * given instant (inclusive) — excludes FAILED / CANCELLED / COMPENSATED (refunded) requests.
     * Never returns null.
     */
    BigDecimal sumWithdrawnSince(UUID tenantId, UUID walletId, Instant since);

    Optional<WalletWithdrawal> findById(UUID id);

    Optional<WalletWithdrawal> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WalletWithdrawal> findByIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<WalletWithdrawal> findBySourceWallet(UUID walletId);

    PageResponse<WalletWithdrawal> findBySourceWallet(UUID walletId, PageQuery query);
}
