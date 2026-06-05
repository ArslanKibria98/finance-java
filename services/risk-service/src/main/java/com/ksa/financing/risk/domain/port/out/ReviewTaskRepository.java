package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.review.ReviewTask;
import com.ksa.financing.risk.domain.model.review.ReviewTaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewTaskRepository {

    ReviewTask save(ReviewTask task);

    Optional<ReviewTask> findById(UUID tenantId, UUID id);

    List<ReviewTask> findBySessionId(UUID tenantId, UUID sessionId);

    List<ReviewTask> findByStatus(UUID tenantId, ReviewTaskStatus status);

    List<ReviewTask> findBySlaDeadlineBefore(UUID tenantId, Instant deadline);
}
