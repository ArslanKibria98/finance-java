package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.SourceOfFundsOption;
import com.ksa.financing.customer.domain.port.out.SourceOfFundsRepository;
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
public class SourceOfFundsRepositoryImpl implements SourceOfFundsRepository {

    private final JpaSourceOfFundsRepository jpaRepository;

    @Override
    public SourceOfFundsOption save(SourceOfFundsOption option) {
        log.debug("Saving source of funds option: {}", option.getCode());
        var entity = ReferenceDataPersistenceMapper.toSofEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<SourceOfFundsOption> findById(UUID tenantId, UUID id) {
        log.debug("Finding source of funds option by ID: {} for tenant: {}", id, tenantId);
        return jpaRepository.findByIdAndTenantId(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public List<SourceOfFundsOption> findAllByTenantId(UUID tenantId) {
        log.debug("Finding all source of funds options for tenant: {}", tenantId);
        return jpaRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId)
                .stream().map(ReferenceDataPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<SourceOfFundsOption> findActiveByTenantId(UUID tenantId) {
        log.debug("Finding active source of funds options for tenant: {}", tenantId);
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
        log.debug("Deleting source of funds option: {} for tenant: {}", id, tenantId);
        jpaRepository.deleteByIdAndTenantId(id, tenantId);
    }
}
