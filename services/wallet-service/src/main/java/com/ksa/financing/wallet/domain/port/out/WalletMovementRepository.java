package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.model.WalletMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletMovementRepository {
    WalletMovement save(WalletMovement movement);
    Optional<WalletMovement> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    List<WalletMovement> findByWalletId(UUID walletId);

    /**
     * Sum of customer-initiated outgoing spend (DEBIT movements with purpose
     * TRANSFER_OUT or WITHDRAWAL) for a wallet since the given instant (inclusive).
     * Used to enforce daily / monthly transaction limits. Never returns null.
     */
    BigDecimal sumSpendSince(UUID tenantId, UUID walletId, Instant since);
}
