package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.domain.model.ThirdPartyProvider;
import com.ksa.financing.middleware.domain.port.out.ProviderRepository;
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
public class ProviderRepositoryImpl implements ProviderRepository {

    private final JpaProviderRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

    @Override
    public ThirdPartyProvider save(ThirdPartyProvider provider) {
        log.debug("Saving provider: code={}, tenantId={}", provider.getCode(), provider.getTenantId());
        var entity = mapper.toEntity(provider);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ThirdPartyProvider> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ThirdPartyProvider> findByCode(UUID tenantId, String code) {
        return jpaRepository.findByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ThirdPartyProvider> findAllByTenant(UUID tenantId) {
        return jpaRepository.findByTenantIdAndDeletedAtIsNullOrderByNameEnAsc(tenantId)
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
