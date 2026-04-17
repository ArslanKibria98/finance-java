package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;
import com.ksa.financing.customer.domain.port.out.PurposeOfFinanceRepository;
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
public class PurposeOfFinanceRepositoryImpl implements PurposeOfFinanceRepository {

    private final JpaPurposeOfFinanceRepository jpaRepository;

    @Override
    public PurposeOfFinanceOption save(PurposeOfFinanceOption option) {
        var entity = ReferenceDataPersistenceMapper.toPofEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<PurposeOfFinanceOption> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public List<PurposeOfFinanceOption> findAllByTenantId(UUID tenantId) {
        return jpaRepository.findByTenantIdAndDeletedFalseOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<PurposeOfFinanceOption> findActiveByTenantId(UUID tenantId) {
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
