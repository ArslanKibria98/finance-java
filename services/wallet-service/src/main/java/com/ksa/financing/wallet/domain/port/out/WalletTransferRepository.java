package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.WalletTransfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransferRepository {
    WalletTransfer save(WalletTransfer transfer);
    Optional<WalletTransfer> findById(UUID id);
    Optional<WalletTransfer> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<WalletTransfer> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    List<WalletTransfer> findBySourceWallet(UUID walletId);
    List<WalletTransfer> findByDestinationWallet(UUID walletId);
    PageResponse<WalletTransfer> findAllByWallet(UUID walletId, PageQuery query);
    List<UUID> findRecentRecipientWalletIds(UUID sourceWalletId, int limit);
    Optional<WalletTransfer> findMostRecentTransferToRecipient(UUID sourceWalletId, UUID destinationWalletId);
}
