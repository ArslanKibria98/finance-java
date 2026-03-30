package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.review.ReviewTask;
import com.ksa.financing.risk.domain.model.review.ReviewTaskStatus;
import com.ksa.financing.risk.domain.port.out.ReviewTaskRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.ReviewTaskJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewTaskRepositoryImpl implements ReviewTaskRepository {
    private final JpaReviewTaskRepository jpa;

    @Override
    public ReviewTask save(ReviewTask task) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(task)));
    }
    @Override
    public Optional<ReviewTask> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<ReviewTask> findBySessionId(UUID tenantId, UUID sessionId) {
        return jpa.findAllByTenantIdAndSessionId(tenantId, sessionId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<ReviewTask> findByStatus(UUID tenantId, ReviewTaskStatus status) {
        return jpa.findAllByTenantIdAndStatus(tenantId, ReviewTaskJpaEntity.ReviewStatusEnum.valueOf(status.name()))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<ReviewTask> findBySlaDeadlineBefore(UUID tenantId, Instant deadline) {
        return jpa.findAllByTenantIdAndSlaDeadlineBeforeAndSlaBreachedFalse(tenantId, deadline.atOffset(ZoneOffset.UTC))
                .stream().map(RiskPersistenceMapper::toDomain).toList();
    }
}
