package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientProviderAccessJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientProviderAccessRepository extends JpaRepository<ClientProviderAccessJpaEntity, UUID> {

    List<ClientProviderAccessJpaEntity> findByClientIdAndTenantId(UUID clientId, UUID tenantId);

    List<ClientProviderAccessJpaEntity> findByProviderIdAndTenantId(UUID providerId, UUID tenantId);

    Optional<ClientProviderAccessJpaEntity> findByClientIdAndProviderIdAndTenantId(UUID clientId, UUID providerId, UUID tenantId);

    void deleteByClientIdAndProviderIdAndTenantId(UUID clientId, UUID providerId, UUID tenantId);
}
