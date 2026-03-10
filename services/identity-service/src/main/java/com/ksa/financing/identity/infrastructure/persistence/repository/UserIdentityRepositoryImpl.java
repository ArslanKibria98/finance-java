package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import com.ksa.financing.identity.infrastructure.persistence.entity.UserIdentityJpaEntity;
import com.ksa.financing.identity.infrastructure.persistence.mapper.UserIdentityPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserIdentityRepositoryImpl implements UserIdentityRepository {

    private final JpaUserIdentityRepository jpaRepository;
    private final UserIdentityPersistenceMapper mapper;

    @Override
    public UserIdentity save(UserIdentity userIdentity) {
        log.debug("Saving user identity for keycloak user: {}", userIdentity.getKeycloakUserId());
        UserIdentityJpaEntity entity = mapper.toEntity(userIdentity);
        UserIdentityJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UserIdentity> findById(UUID id) {
        log.debug("Finding user identity by ID: {}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserIdentity> findByKeycloakUserId(UUID keycloakUserId) {
        log.debug("Finding user identity by Keycloak user ID: {}", keycloakUserId);
        return jpaRepository.findByKeycloakUserId(keycloakUserId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserIdentity> findByInternalUserId(UUID tenantId, UUID internalUserId) {
        log.debug("Finding user identity by internal user ID: {} for tenant: {}", internalUserId, tenantId);
        return jpaRepository.findByInternalUserIdAndTenantId(internalUserId, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserIdentity> findByInternalCustomerId(UUID customerId) {
        log.debug("Finding user identity by internal customer ID: {}", customerId);
        return jpaRepository.findById(customerId)
                .filter(entity -> entity.getInternalCustomerId() != null && entity.getInternalCustomerId().equals(customerId))
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserIdentity> findByKeycloakUsername(String keycloakUsername) {
        log.debug("Finding user identity by Keycloak username: {}", keycloakUsername);
        return jpaRepository.findByKeycloakUsername(keycloakUsername)
                .map(mapper::toDomain);
    }
}
