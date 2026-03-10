package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.NetWorthRangeOption;
import com.ksa.financing.customer.domain.port.out.NetWorthRangeRepository;
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
public class NetWorthRangeRepositoryImpl implements NetWorthRangeRepository {

    private final JpaNetWorthRangeRepository jpaRepository;

    @Override
    public NetWorthRangeOption save(NetWorthRangeOption option) {
        log.debug("Saving net worth range option: {}", option.getCode());
        var entity = ReferenceDataPersistenceMapper.toNwrEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<NetWorthRangeOption> findById(UUID tenantId, UUID id) {
        log.debug("Finding net worth range option by ID: {} for tenant: {}", id, tenantId);
        return jpaRepository.findByIdAndTenantId(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public List<NetWorthRangeOption> findAllByTenantId(UUID tenantId) {
        log.debug("Finding all net worth range options for tenant: {}", tenantId);
        return jpaRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<NetWorthRangeOption> findActiveByTenantId(UUID tenantId) {
        log.debug("Finding active net worth range options for tenant: {}", tenantId);
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
        log.debug("Deleting net worth range option: {} for tenant: {}", id, tenantId);
        jpaRepository.deleteByIdAndTenantId(id, tenantId);
    }
}
