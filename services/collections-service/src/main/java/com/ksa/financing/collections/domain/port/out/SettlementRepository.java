package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.SettlementAggregate;
import com.ksa.financing.collections.domain.model.SettlementId;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository {
    SettlementAggregate save(SettlementAggregate settlement);
    Optional<SettlementAggregate> findById(UUID tenantId, SettlementId id);
    Optional<SettlementAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
}
