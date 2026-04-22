package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.domain.model.CoaFieldLov;
import com.ksa.financing.ledger.domain.model.CoaFieldStatus;
import com.ksa.financing.ledger.domain.port.out.CoaFieldLovRepository;
import com.ksa.financing.ledger.infrastructure.persistence.mapper.CoaFieldLovPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CoaFieldLovRepositoryImpl implements CoaFieldLovRepository {

    private final JpaCoaFieldLovRepository jpaRepository;
    private final CoaFieldLovPersistenceMapper mapper;

    @Override
    public CoaFieldLov save(CoaFieldLov fieldLov) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(fieldLov)));
    }

    @Override
    public Optional<CoaFieldLov> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
    }

    @Override
    public Optional<CoaFieldLov> findByFieldKey(UUID tenantId, String fieldKey) {
        return jpaRepository.findByTenantIdAndFieldKey(tenantId, fieldKey.trim().toUpperCase()).map(mapper::toDomain);
    }

    @Override
    public List<CoaFieldLov> findAllByTenant(UUID tenantId) {
        return jpaRepository.findAllByTenantIdOrderByDisplayOrderAsc(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CoaFieldLov> findAllActiveByTenant(UUID tenantId) {
        return jpaRepository.findAllByTenantIdAndStatusOrderByDisplayOrderAsc(tenantId, CoaFieldStatus.ACTIVE.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByFieldKey(UUID tenantId, String fieldKey) {
        return jpaRepository.existsByTenantIdAndFieldKey(tenantId, fieldKey.trim().toUpperCase());
    }
}
