package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;

import java.util.Optional;
import java.util.UUID;

public interface ExternalFundTransferRepository {
    ExternalFundTransfer save(ExternalFundTransfer transfer);
    Optional<ExternalFundTransfer> findById(UUID id);
    Optional<ExternalFundTransfer> findByMovementId(UUID movementId);
    Optional<ExternalFundTransfer> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<ExternalFundTransfer> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    PageResponse<ExternalFundTransfer> findAllByWallet(UUID walletId, PageQuery query);
}
