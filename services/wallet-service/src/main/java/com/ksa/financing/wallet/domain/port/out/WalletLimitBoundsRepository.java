package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import java.util.Optional;
import java.util.UUID;

public interface WalletLimitBoundsRepository {
    WalletLimitBounds save(WalletLimitBounds bounds);
    Optional<WalletLimitBounds> findByTenantId(UUID tenantId);
}
