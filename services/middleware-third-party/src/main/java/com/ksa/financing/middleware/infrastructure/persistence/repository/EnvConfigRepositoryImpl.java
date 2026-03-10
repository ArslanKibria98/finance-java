package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.domain.model.ApiEnvironmentConfig;
import com.ksa.financing.middleware.domain.port.out.EnvConfigRepository;
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
public class EnvConfigRepositoryImpl implements EnvConfigRepository {

    private final JpaEnvConfigRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

    @Override
    public ApiEnvironmentConfig save(ApiEnvironmentConfig config) {
        log.debug("Saving env config: apiId={}, env={}, tenantId={}",
                config.getApiId(), config.getEnvironment(), config.getTenantId());
        var entity = mapper.toEntity(config);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ApiEnvironmentConfig> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ApiEnvironmentConfig> findAllByApi(UUID tenantId, UUID apiId) {
        return jpaRepository.findByApiIdAndTenantIdAndDeletedAtIsNull(apiId, tenantId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID tenantId, UUID id) {
        jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .ifPresent(jpaRepository::delete);
    }
}
