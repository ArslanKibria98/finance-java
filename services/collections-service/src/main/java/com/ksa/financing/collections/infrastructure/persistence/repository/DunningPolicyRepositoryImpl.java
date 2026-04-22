package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.DunningPolicy;
import com.ksa.financing.collections.domain.model.DunningPolicyId;
import com.ksa.financing.collections.domain.port.out.DunningPolicyRepository;
import com.ksa.financing.collections.infrastructure.persistence.mapper.DunningPolicyPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class DunningPolicyRepositoryImpl implements DunningPolicyRepository {

    private final JpaDunningPolicyRepository jpaRepository;
    private final DunningPolicyPersistenceMapper mapper;

    @Override
    public DunningPolicy save(DunningPolicy policy) {
        var entity = mapper.toEntity(policy);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DunningPolicy> findById(UUID tenantId, DunningPolicyId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DunningPolicy> findByName(UUID tenantId, String policyName) {
        return jpaRepository.findByTenantIdAndPolicyName(tenantId, policyName)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DunningPolicy> findActiveByProductCode(UUID tenantId, String productCode) {
        return jpaRepository.findActiveByProductCode(tenantId, productCode)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DunningPolicy> findActiveDefault(UUID tenantId) {
        return jpaRepository.findActiveDefault(tenantId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DunningPolicy> findAll(UUID tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DunningPolicy> findAllActive(UUID tenantId) {
        return jpaRepository.findAllByTenantIdAndActiveTrue(tenantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void clearDefaultExcept(UUID tenantId, DunningPolicyId keepId) {
        jpaRepository.clearDefaultExcept(tenantId, keepId.getValue());
    }

    @Override
    public void deleteById(UUID tenantId, DunningPolicyId id) {
        jpaRepository.deleteByTenantIdAndId(tenantId, id.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(UUID tenantId, String policyName) {
        return jpaRepository.existsByTenantIdAndPolicyName(tenantId, policyName);
    }
}
