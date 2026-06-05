package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSession;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentSessionRepository {

    AssessmentSession save(AssessmentSession session);

    Optional<AssessmentSession> findById(UUID tenantId, UUID id);

    PageResponse<AssessmentSession> findByEntityReference(UUID tenantId, String entityReference, PageQuery pageQuery);

    Optional<AssessmentSession> findByIdempotencyKey(UUID tenantId, String idempotencyKey);

    Optional<AssessmentSession> findLatestByEntityReference(UUID tenantId, String entityReference);
}
