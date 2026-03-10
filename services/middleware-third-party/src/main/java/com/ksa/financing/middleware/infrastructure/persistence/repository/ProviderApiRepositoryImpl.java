package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.domain.model.ProviderApi;
import com.ksa.financing.middleware.domain.port.out.ProviderApiRepository;
import com.ksa.financing.middleware.infrastructure.persistence.mapper.MiddlewarePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProviderApiRepositoryImpl implements ProviderApiRepository {

    private final JpaProviderApiRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

    @Override
    public ProviderApi save(ProviderApi api) {
        log.debug("Saving provider API: code={}, tenantId={}", api.getCode(), api.getTenantId());
        var entity = mapper.toEntity(api);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProviderApi> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ProviderApi> findByCode(UUID tenantId, String code) {
        return jpaRepository.findByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ProviderApi> findAllByProvider(UUID tenantId, UUID providerId) {
        return jpaRepository.findByProviderIdAndTenantIdAndDeletedAtIsNullOrderByNameAsc(providerId, tenantId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProviderApi> findAllByTenant(UUID tenantId) {
        return jpaRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID tenantId, UUID id) {
        jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .ifPresent(jpaRepository::delete);
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code) {
        return jpaRepository.existsByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId);
    }
}
