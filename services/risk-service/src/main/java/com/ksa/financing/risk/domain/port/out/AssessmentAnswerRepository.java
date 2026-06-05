package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.assessment.AssessmentAnswer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAnswerRepository {

    AssessmentAnswer save(AssessmentAnswer answer);

    List<AssessmentAnswer> saveAll(List<AssessmentAnswer> answers);

    Optional<AssessmentAnswer> findById(UUID tenantId, UUID id);

    List<AssessmentAnswer> findBySessionId(UUID tenantId, UUID sessionId);

    List<AssessmentAnswer> findActiveBySessionId(UUID tenantId, UUID sessionId);

    List<AssessmentAnswer> findBySessionIdAndParameterId(UUID tenantId, UUID sessionId, UUID parameterId);
}
