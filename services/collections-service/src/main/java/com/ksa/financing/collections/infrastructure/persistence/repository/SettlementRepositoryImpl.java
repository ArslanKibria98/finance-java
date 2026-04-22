package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.SettlementAggregate;
import com.ksa.financing.collections.domain.model.SettlementId;
import com.ksa.financing.collections.domain.port.out.SettlementRepository;
import com.ksa.financing.collections.infrastructure.persistence.mapper.SettlementPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SettlementRepositoryImpl implements SettlementRepository {

    private final JpaSettlementRepository jpaRepository;
    private final SettlementPersistenceMapper mapper;

    @Override
    public SettlementAggregate save(SettlementAggregate settlement) {
        var entity = mapper.toEntity(settlement);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<SettlementAggregate> findById(UUID tenantId, SettlementId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<SettlementAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .map(mapper::toDomain);
    }
}
