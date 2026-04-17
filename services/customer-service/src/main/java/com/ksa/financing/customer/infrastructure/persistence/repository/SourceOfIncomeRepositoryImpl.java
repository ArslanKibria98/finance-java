package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.SourceOfIncomeOption;
import com.ksa.financing.customer.domain.port.out.SourceOfIncomeRepository;
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
public class SourceOfIncomeRepositoryImpl implements SourceOfIncomeRepository {

    private final JpaSourceOfIncomeRepository jpaRepository;

    @Override
    public SourceOfIncomeOption save(SourceOfIncomeOption option) {
        var entity = ReferenceDataPersistenceMapper.toSoiEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<SourceOfIncomeOption> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public List<SourceOfIncomeOption> findAllByTenantId(UUID tenantId) {
        return jpaRepository.findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<SourceOfIncomeOption> findActiveByTenantId(UUID tenantId) {
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
