package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.ReviewTaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaReviewTaskRepository extends JpaRepository<ReviewTaskJpaEntity, UUID> {
    Optional<ReviewTaskJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ReviewTaskJpaEntity> findAllByTenantIdAndSessionId(UUID tenantId, UUID sessionId);
    List<ReviewTaskJpaEntity> findAllByTenantIdAndStatus(UUID tenantId, ReviewTaskJpaEntity.ReviewStatusEnum status);
    List<ReviewTaskJpaEntity> findAllByTenantIdAndSlaDeadlineBeforeAndSlaBreachedFalse(UUID tenantId, OffsetDateTime deadline);
}
