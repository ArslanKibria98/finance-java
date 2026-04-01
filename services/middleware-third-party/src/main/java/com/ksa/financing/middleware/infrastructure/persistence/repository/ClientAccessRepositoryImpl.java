package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.domain.model.AccessEnvironment;
import com.ksa.financing.middleware.domain.model.ClientApiAccess;
import com.ksa.financing.middleware.domain.model.ClientProviderAccess;
import com.ksa.financing.middleware.domain.port.out.ClientAccessRepository;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientApiAccessJpaEntity;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientProviderAccessJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClientAccessRepositoryImpl implements ClientAccessRepository {

    private final JpaClientProviderAccessRepository jpaProviderAccessRepo;
    private final JpaClientApiAccessRepository jpaApiAccessRepo;
    private final JpaProviderApiRepository jpaProviderApiRepo;

    // ==================== Provider Access ====================

    @Override
    public ClientProviderAccess saveProviderAccess(ClientProviderAccess access) {
        var entity = toProviderAccessEntity(access);
        var saved = jpaProviderAccessRepo.save(entity);
        return toProviderAccessDomain(saved);
    }

    @Override
    public Optional<ClientProviderAccess> findProviderAccess(UUID tenantId, UUID clientId, UUID providerId) {
        return jpaProviderAccessRepo.findByClientIdAndProviderIdAndTenantId(clientId, providerId, tenantId)
                .map(this::toProviderAccessDomain);
    }

    @Override
    public List<ClientProviderAccess> findProviderAccessByClient(UUID tenantId, UUID clientId) {
        return jpaProviderAccessRepo.findByClientIdAndTenantId(clientId, tenantId)
                .stream().map(this::toProviderAccessDomain).toList();
    }

    @Override
    @Transactional
    public void deleteProviderAccess(UUID tenantId, UUID clientId, UUID providerId) {
        jpaProviderAccessRepo.deleteByClientIdAndProviderIdAndTenantId(clientId, providerId, tenantId);
    }

    // ==================== API Access ====================

    @Override
    public ClientApiAccess saveApiAccess(ClientApiAccess access) {
        var entity = toApiAccessEntity(access);
        var saved = jpaApiAccessRepo.save(entity);
        return toApiAccessDomain(saved);
    }

    @Override
    public Optional<ClientApiAccess> findApiAccess(UUID tenantId, UUID clientId, UUID apiId) {
        return jpaApiAccessRepo.findByClientIdAndApiIdAndTenantId(clientId, apiId, tenantId)
                .map(this::toApiAccessDomain);
    }

    @Override
    public List<ClientApiAccess> findApiAccessByClient(UUID tenantId, UUID clientId) {
        return jpaApiAccessRepo.findByClientIdAndTenantId(clientId, tenantId)
                .stream().map(this::toApiAccessDomain).toList();
    }

    @Override
    public List<ClientApiAccess> findApiAccessByClientAndProvider(UUID tenantId, UUID clientId, UUID providerId) {
        // Get all API IDs that belong to this provider
        var apiIds = jpaProviderApiRepo.findByProviderIdAndTenantIdAndDeletedAtIsNullOrderByNameEnAsc(providerId, tenantId)
                .stream().map(api -> api.getId()).toList();

        if (apiIds.isEmpty()) return List.of();

        // Filter the client's API access to only those belonging to this provider
        return jpaApiAccessRepo.findByClientIdAndTenantId(clientId, tenantId)
                .stream()
                .filter(entity -> apiIds.contains(entity.getApiId()))
                .map(this::toApiAccessDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteApiAccess(UUID tenantId, UUID clientId, UUID apiId) {
        jpaApiAccessRepo.deleteByClientIdAndApiIdAndTenantId(clientId, apiId, tenantId);
    }

    // ==================== Mapping ====================

    private ClientProviderAccess toProviderAccessDomain(ClientProviderAccessJpaEntity entity) {
        var domain = new ClientProviderAccess();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setClientId(entity.getClientId());
        domain.setProviderId(entity.getProviderId());
        domain.setEnvironment(entity.getEnvironment() != null
                ? AccessEnvironment.valueOf(entity.getEnvironment().name()) : null);
        domain.setActive(entity.isActive());
        domain.setGrantedAt(entity.getGrantedAt() != null ? entity.getGrantedAt().toInstant() : null);
        domain.setGrantedBy(entity.getGrantedBy());
        return domain;
    }

    private ClientProviderAccessJpaEntity toProviderAccessEntity(ClientProviderAccess domain) {
        var entity = new ClientProviderAccessJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setClientId(domain.getClientId());
        entity.setProviderId(domain.getProviderId());
        entity.setEnvironment(domain.getEnvironment() != null
                ? ClientProviderAccessJpaEntity.AccessEnvironmentEnum.valueOf(domain.getEnvironment().name()) : null);
        entity.setActive(domain.isActive());
        entity.setGrantedAt(domain.getGrantedAt() != null
                ? domain.getGrantedAt().atOffset(ZoneOffset.UTC) : null);
        entity.setGrantedBy(domain.getGrantedBy());
        return entity;
    }

    private ClientApiAccess toApiAccessDomain(ClientApiAccessJpaEntity entity) {
        var domain = new ClientApiAccess();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setClientId(entity.getClientId());
        domain.setApiId(entity.getApiId());
        domain.setEnvironment(entity.getEnvironment() != null
                ? AccessEnvironment.valueOf(entity.getEnvironment().name()) : null);
        domain.setActive(entity.isActive());
        domain.setGrantedAt(entity.getGrantedAt() != null ? entity.getGrantedAt().toInstant() : null);
        domain.setGrantedBy(entity.getGrantedBy());
        return domain;
    }

    private ClientApiAccessJpaEntity toApiAccessEntity(ClientApiAccess domain) {
        var entity = new ClientApiAccessJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setClientId(domain.getClientId());
        entity.setApiId(domain.getApiId());
        entity.setEnvironment(domain.getEnvironment() != null
                ? ClientApiAccessJpaEntity.AccessEnvironmentEnum.valueOf(domain.getEnvironment().name()) : null);
        entity.setActive(domain.isActive());
        entity.setGrantedAt(domain.getGrantedAt() != null
                ? domain.getGrantedAt().atOffset(ZoneOffset.UTC) : null);
        entity.setGrantedBy(domain.getGrantedBy());
        return entity;
    }
}
