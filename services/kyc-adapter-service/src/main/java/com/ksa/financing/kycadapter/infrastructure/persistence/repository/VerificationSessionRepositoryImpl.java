package com.ksa.financing.kycadapter.infrastructure.persistence.repository;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import com.ksa.financing.kycadapter.domain.port.out.VerificationSessionRepository;
import com.ksa.financing.kycadapter.infrastructure.persistence.entity.VerificationSessionJpaEntity;
import com.ksa.financing.kycadapter.infrastructure.persistence.mapper.KycPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing the domain VerificationSessionRepository port.
 * Delegates to Spring Data JPA repository and maps between entity and domain model.
 */
@Repository
@RequiredArgsConstructor
public class VerificationSessionRepositoryImpl implements VerificationSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(VerificationSessionRepositoryImpl.class);

    private final JpaVerificationSessionRepository jpaRepository;
    private final KycPersistenceMapper mapper;

    @Override
    public VerificationSession save(VerificationSession session) {
        log.debug("Saving verification session: sessionNumber={}, tenantId={}",
                session.getSessionNumber(), session.getTenantId());

        VerificationSessionJpaEntity entity = mapper.toEntity(session);
        VerificationSessionJpaEntity saved = jpaRepository.save(entity);

        log.debug("Saved verification session with id={}", saved.getId());
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<VerificationSession> findById(UUID id) {
        log.debug("Finding verification session by id={}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<VerificationSession> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        log.debug("Finding verification session by idempotencyKey={}, tenantId={}", idempotencyKey, tenantId);
        return jpaRepository.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<VerificationSession> findBySessionNumber(UUID tenantId, String sessionNumber) {
        log.debug("Finding verification session by sessionNumber={}, tenantId={}", sessionNumber, tenantId);
        return jpaRepository.findBySessionNumberAndTenantId(sessionNumber, tenantId)
                .map(mapper::toDomain);
    }
}
