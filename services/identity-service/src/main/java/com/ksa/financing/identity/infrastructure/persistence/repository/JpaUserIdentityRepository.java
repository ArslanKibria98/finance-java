package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.infrastructure.persistence.entity.UserIdentityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserIdentityRepository extends JpaRepository<UserIdentityJpaEntity, UUID> {

    Optional<UserIdentityJpaEntity> findByKeycloakUserIdAndTenantId(UUID keycloakUserId, UUID tenantId);

    Optional<UserIdentityJpaEntity> findByInternalUserIdAndTenantId(UUID internalUserId, UUID tenantId);

    Optional<UserIdentityJpaEntity> findByInternalCustomerIdAndTenantId(UUID internalCustomerId, UUID tenantId);

    Optional<UserIdentityJpaEntity> findByInternalCustomerId(UUID internalCustomerId);

    Optional<UserIdentityJpaEntity> findByKeycloakUserId(UUID keycloakUserId);

    boolean existsByKeycloakUserIdAndTenantId(UUID keycloakUserId, UUID tenantId);

    Optional<UserIdentityJpaEntity> findByKeycloakUsername(String keycloakUsername);

    List<UserIdentityJpaEntity> findByMobileNumber(String mobileNumber);
}
