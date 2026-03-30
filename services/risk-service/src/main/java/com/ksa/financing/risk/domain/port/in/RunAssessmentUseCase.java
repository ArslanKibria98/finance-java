package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.assessment.AssessmentAnswer;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSession;
import com.ksa.financing.risk.domain.model.assessment.ScoreBreakdown;
import com.ksa.financing.risk.domain.model.parameter.RiskType;

import java.util.List;
import java.util.UUID;

public interface RunAssessmentUseCase {

    AssessmentSession initiateSession(UUID tenantId, InitiateSessionCommand command);

    AssessmentSession submitAnswers(UUID tenantId, UUID sessionId, List<SubmitAnswerCommand> answers, UUID actorId);

    AssessmentSession saveDraft(UUID tenantId, UUID sessionId, List<SubmitAnswerCommand> answers, UUID actorId);

    AssessmentSession scoreSession(UUID tenantId, UUID sessionId);

    AssessmentSession getSession(UUID tenantId, UUID sessionId);

    List<AssessmentSession> getSessionsByEntity(UUID tenantId, String entityReference);

    List<AssessmentAnswer> getAnswersBySession(UUID tenantId, UUID sessionId);

    List<ScoreBreakdown> getScoreBreakdown(UUID tenantId, UUID sessionId);

    AssessmentSession reEvaluate(UUID tenantId, UUID sessionId, UUID actorId);

    record InitiateSessionCommand(
            RiskType riskType,
            String entityReference,
            String idempotencyKey
    ) {}

    record SubmitAnswerCommand(
            UUID parameterId,
            String answerValue,
            String languageCode
    ) {}
}
