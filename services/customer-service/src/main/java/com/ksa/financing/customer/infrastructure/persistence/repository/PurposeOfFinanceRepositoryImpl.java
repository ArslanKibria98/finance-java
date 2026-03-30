package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;
import com.ksa.financing.customer.domain.port.out.PurposeOfFinanceRepository;
import com.ksa.financing.customer.infrastructure.persistence.mapper.ReferenceDataPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
        log.debug("Saving purpose of finance option: {}", option.getCode());
        var entity = ReferenceDataPersistenceMapper.toPofEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<PurposeOfFinanceOption> findById(UUID tenantId, UUID id) {
        log.debug("Finding purpose of finance option by ID: {} for tenant: {}", id, tenantId);
        return jpaRepository.findByIdAndTenantId(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public List<PurposeOfFinanceOption> findAllByTenantId(UUID tenantId) {
        log.debug("Finding all purpose of finance options for tenant: {}", tenantId);
        return jpaRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<PurposeOfFinanceOption> findActiveByTenantId(UUID tenantId) {
        log.debug("Finding active purpose of finance options for tenant: {}", tenantId);
        return jpaRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code) {
        return jpaRepository.existsByTenantIdAndCode(tenantId, code);
    }

    @Override
    @Transactional
    public void deleteById(UUID tenantId, UUID id) {
        log.debug("Deleting purpose of finance option: {} for tenant: {}", id, tenantId);
        jpaRepository.deleteByIdAndTenantId(id, tenantId);
    }
}
