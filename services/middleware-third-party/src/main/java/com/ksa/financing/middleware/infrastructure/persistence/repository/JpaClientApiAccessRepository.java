package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.infrastructure.persistence.entity.ClientApiAccessJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientApiAccessRepository extends JpaRepository<ClientApiAccessJpaEntity, UUID> {

    List<ClientApiAccessJpaEntity> findByClientIdAndTenantId(UUID clientId, UUID tenantId);

    List<ClientApiAccessJpaEntity> findByApiIdAndTenantId(UUID apiId, UUID tenantId);

    Optional<ClientApiAccessJpaEntity> findByClientIdAndApiIdAndTenantId(UUID clientId, UUID apiId, UUID tenantId);

    void deleteByClientIdAndApiIdAndTenantId(UUID clientId, UUID apiId, UUID tenantId);
}
