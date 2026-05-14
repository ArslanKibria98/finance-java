package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.domain.model.ApiClient;
import com.ksa.financing.middleware.domain.port.out.ClientRepository;
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
public class ClientRepositoryImpl implements ClientRepository {

    private final JpaClientRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

    @Override
    public ApiClient save(ApiClient client) {
        log.debug("Saving client: code={}, tenantId={}", client.getCode(), client.getTenantId());
        var entity = mapper.toEntity(client);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ApiClient> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ApiClient> findByCode(UUID tenantId, String code) {
        return jpaRepository.findByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ApiClient> findByCode(String code) {
        return jpaRepository.findFirstByCodeAndDeletedAtIsNull(code)
                .map(mapper::toDomain);
    }

    @Override
    public List<ApiClient> findAllByTenant(UUID tenantId) {
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

    @Override
    public Optional<ApiClient> findBySecretKey(String secretKey) {
        return jpaRepository.findBySecretKeyAndDeletedAtIsNull(secretKey)
                .map(mapper::toDomain);
    }
}
