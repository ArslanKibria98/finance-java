package com.ksa.financing.kycadapter.infrastructure.persistence.repository;

import com.ksa.financing.kycadapter.infrastructure.persistence.entity.ProviderResponseCacheJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for provider response cache entities.
 */
@Repository
public interface JpaProviderCacheRepository extends JpaRepository<ProviderResponseCacheJpaEntity, UUID> {

    Optional<ProviderResponseCacheJpaEntity> findByCacheKeyAndTenantId(String cacheKey, UUID tenantId);

    List<ProviderResponseCacheJpaEntity> findBySubjectIdAndSubjectIdTypeAndProvider(
            String subjectId,
            String subjectIdType,
            VerificationSessionJpaEntity.KycProviderEnum provider
    );
}
