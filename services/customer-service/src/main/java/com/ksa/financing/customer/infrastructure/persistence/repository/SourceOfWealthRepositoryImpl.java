package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.SourceOfWealthOption;
import com.ksa.financing.customer.domain.port.out.SourceOfWealthRepository;
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
public class SourceOfWealthRepositoryImpl implements SourceOfWealthRepository {

    private final JpaSourceOfWealthRepository jpaRepository;

    @Override
    public SourceOfWealthOption save(SourceOfWealthOption option) {
        log.debug("Saving source of wealth option: {}", option.getCode());
        var entity = ReferenceDataPersistenceMapper.toSowEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<SourceOfWealthOption> findById(UUID tenantId, UUID id) {
        log.debug("Finding source of wealth option by ID: {} for tenant: {}", id, tenantId);
        return jpaRepository.findByIdAndTenantId(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public List<SourceOfWealthOption> findAllByTenantId(UUID tenantId) {
        log.debug("Finding all source of wealth options for tenant: {}", tenantId);
        return jpaRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<SourceOfWealthOption> findActiveByTenantId(UUID tenantId) {
        log.debug("Finding active source of wealth options for tenant: {}", tenantId);
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
        log.debug("Deleting source of wealth option: {} for tenant: {}", id, tenantId);
        jpaRepository.deleteByIdAndTenantId(id, tenantId);
    }
}
