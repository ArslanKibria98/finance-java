package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.assessment.AnswerChangeReason;
import com.ksa.financing.risk.domain.model.assessment.AnswerVersionStatus;
import com.ksa.financing.risk.domain.model.assessment.AssessmentAnswer;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSession;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSessionStatus;
import com.ksa.financing.risk.domain.model.assessment.ScoreBreakdown;
import com.ksa.financing.risk.domain.model.lov.LovEntry;
import com.ksa.financing.risk.domain.model.parameter.ParameterFlagType;
import com.ksa.financing.risk.domain.model.parameter.ParameterInputType;
import com.ksa.financing.risk.domain.model.parameter.RiskParameter;
import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.model.parameter.ScoringThreshold;
import com.ksa.financing.risk.domain.model.review.ReviewTask;
import com.ksa.financing.risk.domain.model.review.ReviewTaskStatus;
import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;
import com.ksa.financing.risk.domain.model.status.EntityRiskStatus;
import com.ksa.financing.risk.domain.model.status.EntityStatusRecord;
import com.ksa.financing.risk.domain.port.in.RunAssessmentUseCase;
import com.ksa.financing.risk.domain.port.out.AssessmentAnswerRepository;
import com.ksa.financing.risk.domain.port.out.AssessmentSessionRepository;
import com.ksa.financing.risk.domain.port.out.EntityStatusRepository;
import com.ksa.financing.risk.domain.port.out.LovEntryRepository;
import com.ksa.financing.risk.domain.port.out.ReviewTaskRepository;
import com.ksa.financing.risk.domain.port.out.RiskParameterRepository;
import com.ksa.financing.risk.domain.port.out.ScenarioRuleRepository;
import com.ksa.financing.risk.domain.port.out.ScoringThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunAssessmentService implements RunAssessmentUseCase {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final RiskParameterRepository parameterRepository;
    private final LovEntryRepository lovEntryRepository;
    private final ScoringThresholdRepository thresholdRepository;
    private final ScenarioRuleRepository scenarioRuleRepository;
    private final ReviewTaskRepository reviewTaskRepository;
    private final EntityStatusRepository entityStatusRepository;

    @Override
    @Transactional
    public AssessmentSession initiateSession(UUID tenantId, InitiateSessionCommand command) {
        // Idempotency check
        var existing = sessionRepository.findByIdempotencyKey(tenantId, command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing session for idempotency key: {}", command.idempotencyKey());
            return existing.get();
        }

        var session = new AssessmentSession();
        session.setId(UUID.randomUUID());
        session.setTenantId(tenantId);
        session.setRiskType(command.riskType());
        session.setEntityReference(command.entityReference());
        session.setStatus(AssessmentSessionStatus.INITIATED);
        session.setTotalScore(BigDecimal.ZERO);
        session.setPepFlag(false);
        session.setEddFlag(false);
        session.setKycFlag(false);
        session.setDominantOverride(false);
        session.setIdempotencyKey(command.idempotencyKey());
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        session.setVersion(1);

        var saved = sessionRepository.save(session);
        log.info("Assessment session initiated: {} for entity: {}", saved.getId(), command.entityReference());
        return saved;
    }

    @Override
    @Transactional
    public AssessmentSession submitAnswers(UUID tenantId, UUID sessionId, List<SubmitAnswerCommand> answers, UUID actorId) {
        var session = sessionRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> NotFoundException.forEntity("AssessmentSession", sessionId.toString()));

        if (session.getStatus() != AssessmentSessionStatus.INITIATED
                && session.getStatus() != AssessmentSessionStatus.IN_PROGRESS
                && session.getStatus() != AssessmentSessionStatus.DRAFT_SAVED) {
            throw new BusinessException("RISK.ASSESSMENT.INVALID_STATE",
                    "Cannot submit answers in state: " + session.getStatus());
        }

        var existingAnswers = answerRepository.findActiveBySessionId(tenantId, sessionId);
        int nextVersion = existingAnswers.isEmpty() ? 1 :
                existingAnswers.stream().mapToInt(AssessmentAnswer::getAnswerVersion).max().orElse(0) + 1;

        // Supersede existing active answers
        for (var ea : existingAnswers) {
            ea.setVersionStatus(AnswerVersionStatus.SUPERSEDED);
            answerRepository.save(ea);
        }

        List<AssessmentAnswer> newAnswers = new ArrayList<>();
        for (var cmd : answers) {
            var answer = new AssessmentAnswer();
            answer.setId(UUID.randomUUID());
            answer.setSessionId(sessionId);
            answer.setTenantId(tenantId);
            answer.setParameterId(cmd.parameterId());
            answer.setAnswerVersion(nextVersion);
            answer.setVersionStatus(AnswerVersionStatus.ACTIVE);
            answer.setAnswerValue(cmd.answerValue());
            answer.setLanguageCode(cmd.languageCode());
            answer.setChangeReason(nextVersion > 1 ? AnswerChangeReason.CORRECTION : null);
            answer.setCreatedAt(Instant.now());
            answer.setUpdatedAt(Instant.now());
            answer.setVersion(1);
            newAnswers.add(answer);
        }

        answerRepository.saveAll(newAnswers);

        session.setStatus(AssessmentSessionStatus.SUBMITTED);
        session.setUpdatedAt(Instant.now());
        var saved = sessionRepository.save(session);

        log.info("Answers submitted for session: {}, version: {}", sessionId, nextVersion);
        return saved;
    }

    @Override
    @Transactional
    public AssessmentSession saveDraft(UUID tenantId, UUID sessionId, List<SubmitAnswerCommand> answers, UUID actorId) {
        var session = sessionRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> NotFoundException.forEntity("AssessmentSession", sessionId.toString()));

        List<AssessmentAnswer> draftAnswers = new ArrayList<>();
        for (var cmd : answers) {
            var answer = new AssessmentAnswer();
            answer.setId(UUID.randomUUID());
            answer.setSessionId(sessionId);
            answer.setTenantId(tenantId);
            answer.setParameterId(cmd.parameterId());
            answer.setAnswerVersion(0); // draft version
            answer.setVersionStatus(AnswerVersionStatus.ACTIVE);
            answer.setAnswerValue(cmd.answerValue());
            answer.setLanguageCode(cmd.languageCode());
            answer.setCreatedAt(Instant.now());
            answer.setUpdatedAt(Instant.now());
            answer.setVersion(1);
            draftAnswers.add(answer);
        }

        answerRepository.saveAll(draftAnswers);

        session.setStatus(AssessmentSessionStatus.DRAFT_SAVED);
        session.setUpdatedAt(Instant.now());
        return sessionRepository.save(session);
    }

    @Override
    @Transactional
    public AssessmentSession scoreSession(UUID tenantId, UUID sessionId) {
        var session = sessionRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> NotFoundException.forEntity("AssessmentSession", sessionId.toString()));

        if (session.getStatus() != AssessmentSessionStatus.SUBMITTED) {
            throw new BusinessException("RISK.ASSESSMENT.INVALID_STATE",
                    "Session must be SUBMITTED to score, current: " + session.getStatus());
        }

        var activeAnswers = answerRepository.findActiveBySessionId(tenantId, sessionId);
        var parameters = parameterRepository.findActiveByRiskType(tenantId, session.getRiskType());

        BigDecimal totalScore = BigDecimal.ZERO;
        boolean pepFlag = false;
        boolean eddFlag = false;
        boolean dominantOverride = false;

        for (var answer : activeAnswers) {
            var paramOpt = parameters.stream()
                    .filter(p -> p.getId().equals(answer.getParameterId()))
                    .findFirst();

            if (paramOpt.isEmpty()) continue;
            var param = paramOpt.get();

            // Check PEP flag
            if (param.getFlagType() == ParameterFlagType.PEP && "true".equalsIgnoreCase(answer.getAnswerValue())) {
                pepFlag = true;
                dominantOverride = true;
            }

            // Check EDD flag
            if (param.getFlagType() == ParameterFlagType.EDD) {
                eddFlag = true;
                // EDD answers excluded from scoring
                answer.setWeightContribution(BigDecimal.ZERO);
                answerRepository.save(answer);
                continue;
            }

            // Calculate score: factorWeight × categoryWeight / 100
            BigDecimal scoreContribution = BigDecimal.ZERO;
            if (param.getInputType() == ParameterInputType.LOV && param.getLovSetId() != null) {
                var entryOpt = lovEntryRepository.findByFactorCode(tenantId, param.getLovSetId(), answer.getAnswerValue());
                if (entryOpt.isPresent()) {
                    var entry = entryOpt.get();
                    scoreContribution = entry.getFactorWeight()
                            .multiply(param.getCategoryWeight())
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                }
            } else if (param.getInputType() == ParameterInputType.BOOLEAN) {
                if ("true".equalsIgnoreCase(answer.getAnswerValue())) {
                    scoreContribution = param.getCategoryWeight();
                }
            }

            answer.setWeightContribution(scoreContribution);
            answerRepository.save(answer);
            totalScore = totalScore.add(scoreContribution);
        }

        session.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        session.setPepFlag(pepFlag);
        session.setEddFlag(eddFlag);
        session.setDominantOverride(dominantOverride);

        // Determine risk level from thresholds
        if (dominantOverride) {
            session.setRiskLevel("PEP");
        } else {
            var thresholds = thresholdRepository.findActiveByRiskType(tenantId, session.getRiskType());
            String riskLevel = determineRiskLevel(totalScore, thresholds);
            session.setRiskLevel(riskLevel);
        }

        session.setStatus(eddFlag ? AssessmentSessionStatus.EDD_TRIGGERED : AssessmentSessionStatus.SCORED);
        session.setUpdatedAt(Instant.now());

        var saved = sessionRepository.save(session);

        // Evaluate scenario rules and auto-create review tasks if matched
        evaluateScenarioRules(tenantId, saved);

        log.info("Session scored: {} score={} level={} pep={} edd={}",
                sessionId, totalScore, saved.getRiskLevel(), pepFlag, eddFlag);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentSession getSession(UUID tenantId, UUID sessionId) {
        return sessionRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> NotFoundException.forEntity("AssessmentSession", sessionId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentSession> getSessionsByEntity(UUID tenantId, String entityReference) {
        return sessionRepository.findByEntityReference(tenantId, entityReference);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentAnswer> getAnswersBySession(UUID tenantId, UUID sessionId) {
        return answerRepository.findBySessionId(tenantId, sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreBreakdown> getScoreBreakdown(UUID tenantId, UUID sessionId) {
        var session = sessionRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> NotFoundException.forEntity("AssessmentSession", sessionId.toString()));

        var activeAnswers = answerRepository.findActiveBySessionId(tenantId, sessionId);
        var parameters = parameterRepository.findActiveByRiskType(tenantId, session.getRiskType());

        List<ScoreBreakdown> breakdowns = new ArrayList<>();
        for (var answer : activeAnswers) {
            var paramOpt = parameters.stream()
                    .filter(p -> p.getId().equals(answer.getParameterId()))
                    .findFirst();
            if (paramOpt.isEmpty()) continue;
            var param = paramOpt.get();

            boolean eddExcluded = param.getFlagType() == ParameterFlagType.EDD;
            BigDecimal factorWeight = BigDecimal.ZERO;
            String factorCode = answer.getAnswerValue();

            if (param.getInputType() == ParameterInputType.LOV && param.getLovSetId() != null) {
                var entryOpt = lovEntryRepository.findByFactorCode(tenantId, param.getLovSetId(), answer.getAnswerValue());
                if (entryOpt.isPresent()) {
                    factorWeight = entryOpt.get().getFactorWeight();
                    factorCode = entryOpt.get().getFactorCode();
                }
            }

            breakdowns.add(new ScoreBreakdown(
                    param.getId(),
                    param.getQuestionEn(),
                    param.getCategory(),
                    answer.getAnswerValue(),
                    factorCode,
                    factorWeight,
                    param.getCategoryWeight(),
                    answer.getWeightContribution() != null ? answer.getWeightContribution() : BigDecimal.ZERO,
                    eddExcluded
            ));
        }
        return breakdowns;
    }

    @Override
    @Transactional
    public AssessmentSession reEvaluate(UUID tenantId, UUID sessionId, UUID actorId) {
        var session = sessionRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> NotFoundException.forEntity("AssessmentSession", sessionId.toString()));

        if (session.getStatus() != AssessmentSessionStatus.COMPLETED) {
            throw new BusinessException("RISK.ASSESSMENT.INVALID_STATE",
                    "Only COMPLETED sessions can be re-evaluated");
        }

        // Create child session
        var newSession = new AssessmentSession();
        newSession.setId(UUID.randomUUID());
        newSession.setTenantId(tenantId);
        newSession.setRiskType(session.getRiskType());
        newSession.setEntityReference(session.getEntityReference());
        newSession.setParentSessionId(sessionId);
        newSession.setStatus(AssessmentSessionStatus.RE_EVALUATED);
        newSession.setTotalScore(BigDecimal.ZERO);
        newSession.setPepFlag(false);
        newSession.setEddFlag(false);
        newSession.setDominantOverride(false);
        newSession.setCreatedAt(Instant.now());
        newSession.setUpdatedAt(Instant.now());
        newSession.setVersion(1);

        var saved = sessionRepository.save(newSession);
        log.info("Re-evaluation session created: {} from parent: {}", saved.getId(), sessionId);
        return saved;
    }

    private void evaluateScenarioRules(UUID tenantId, AssessmentSession session) {
        try {
            var rules = scenarioRuleRepository.findActiveByTenantIdOrderByPriority(tenantId);
            if (rules.isEmpty()) {
                log.debug("No active scenario rules found for tenant: {}", tenantId);
                return;
            }

            String sessionRiskLevel = session.getRiskLevel();
            boolean sessionPepFlag = session.isPepFlag();

            for (var rule : rules) {
                boolean matches = true;

                // Match risk level if the rule specifies one
                if (rule.getTriggerRiskStatus() != null && !rule.getTriggerRiskStatus().isBlank()) {
                    matches = rule.getTriggerRiskStatus().equalsIgnoreCase(sessionRiskLevel);
                }

                // Match PEP flag if the rule specifies one
                if (matches && rule.getTriggerPepFlag() != null) {
                    matches = rule.getTriggerPepFlag().equals(sessionPepFlag);
                }

                // Note: triggerThirdPartyCheckType and triggerThirdPartyResult matching
                // is skipped here because the assessment session does not carry third-party
                // check results. These will be evaluated when third-party checks are integrated.

                if (!matches) {
                    continue;
                }

                log.info("Scenario rule matched: '{}' (id={}) for session={}, riskLevel={}, pep={}",
                        rule.getScenarioName(), rule.getId(), session.getId(), sessionRiskLevel, sessionPepFlag);

                // Create EntityStatusRecord with the rule's resulting statuses
                var statusRecord = new EntityStatusRecord();
                statusRecord.setId(UUID.randomUUID());
                statusRecord.setTenantId(tenantId);
                statusRecord.setSessionId(session.getId());
                statusRecord.setEntityReference(session.getEntityReference());

                // Map the session risk level to the EntityRiskStatus enum
                try {
                    statusRecord.setRiskStatus(EntityRiskStatus.valueOf(sessionRiskLevel));
                } catch (IllegalArgumentException e) {
                    statusRecord.setRiskStatus(EntityRiskStatus.HIGH);
                }

                statusRecord.setAccountStatus(rule.getResultingAccountStatus());
                statusRecord.setComplianceStatus(rule.getResultingComplianceStatus());
                statusRecord.setStatusReason("Auto-triggered by scenario rule: " + rule.getScenarioName());
                statusRecord.setCreatedAt(Instant.now());
                statusRecord.setUpdatedAt(Instant.now());
                statusRecord.setVersion(1);

                entityStatusRepository.save(statusRecord);
                log.info("EntityStatusRecord created for entity={} from scenario rule={}",
                        session.getEntityReference(), rule.getScenarioName());

                // Create ReviewTask if manual review is required
                if (rule.isRequiresManualReview()) {
                    var reviewTask = new ReviewTask();
                    reviewTask.setId(UUID.randomUUID());
                    reviewTask.setTenantId(tenantId);
                    reviewTask.setSessionId(session.getId());
                    reviewTask.setEntityReference(session.getEntityReference());
                    reviewTask.setStatus(ReviewTaskStatus.PENDING_MAKER);
                    reviewTask.setSlaDeadline(Instant.now().plusSeconds((long) rule.getSlaDurationHours() * 3600));
                    reviewTask.setSlaBreached(false);
                    reviewTask.setCreatedAt(Instant.now());
                    reviewTask.setUpdatedAt(Instant.now());
                    reviewTask.setVersion(1);

                    reviewTaskRepository.save(reviewTask);
                    log.info("ReviewTask created (PENDING_MAKER) for session={}, slaHours={}, notifyRole={}",
                            session.getId(), rule.getSlaDurationHours(), rule.getNotifyRole());
                }
            }
        } catch (Exception e) {
            // CRITICAL: Never interrupt scoring due to scenario evaluation failures
            log.error("Scenario rule evaluation failed for session={}, tenant={}: {}",
                    session.getId(), tenantId, e.getMessage(), e);
        }
    }

    private String determineRiskLevel(BigDecimal score, List<ScoringThreshold> thresholds) {
        for (var threshold : thresholds) {
            if (score.compareTo(threshold.getMinScore()) >= 0 && score.compareTo(threshold.getMaxScore()) <= 0) {
                return threshold.getRiskLevel();
            }
        }
        return "UNKNOWN";
    }
}
