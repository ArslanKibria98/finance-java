package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.model.LimitRequestStatus;
import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletLimitChangeRequestRepository {
    WalletLimitChangeRequest save(WalletLimitChangeRequest request);
    Optional<WalletLimitChangeRequest> findById(UUID id);
    List<WalletLimitChangeRequest> findByTenantAndStatus(UUID tenantId, LimitRequestStatus status);
    List<WalletLimitChangeRequest> findByTenant(UUID tenantId);
    List<WalletLimitChangeRequest> findByWallet(UUID walletId);
    boolean existsByWalletAndStatus(UUID walletId, LimitRequestStatus status);
}
