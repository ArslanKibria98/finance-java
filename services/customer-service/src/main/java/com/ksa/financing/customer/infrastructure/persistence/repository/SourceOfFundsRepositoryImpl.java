package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.SourceOfFundsOption;
import com.ksa.financing.customer.domain.port.out.SourceOfFundsRepository;
import com.ksa.financing.customer.infrastructure.persistence.mapper.ReferenceDataPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SourceOfFundsRepositoryImpl implements SourceOfFundsRepository {

    private final JpaSourceOfFundsRepository jpaRepository;

    @Override
    public SourceOfFundsOption save(SourceOfFundsOption option) {
        var entity = ReferenceDataPersistenceMapper.toSofEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<SourceOfFundsOption> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public List<SourceOfFundsOption> findAllByTenantId(UUID tenantId) {
        return jpaRepository.findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<SourceOfFundsOption> findActiveByTenantId(UUID tenantId) {
        return jpaRepository.findByTenantIdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code) {
        return jpaRepository.existsByTenantIdAndCodeAndDeletedFalse(tenantId, code);
    }

    @Override
    public void softDelete(UUID tenantId, UUID id) {
        jpaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setActive(false);
            jpaRepository.save(entity);
        });
    }
}
