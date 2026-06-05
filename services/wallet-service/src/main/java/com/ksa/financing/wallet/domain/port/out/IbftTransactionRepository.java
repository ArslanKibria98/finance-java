package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.IbftTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IbftTransactionRepository {
    IbftTransaction save(IbftTransaction tx);
    Optional<IbftTransaction> findById(UUID id);
    Optional<IbftTransaction> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<IbftTransaction> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    PageResponse<IbftTransaction> findAllByWallet(UUID walletId, PageQuery query);
    /** Non-terminal transfers awaiting settlement (for the reconciliation cron). */
    List<IbftTransaction> findReconcilable(int limit);
}
