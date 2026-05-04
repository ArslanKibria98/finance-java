package com.ksa.financing.fraud.application.usecase;

import com.ksa.financing.fraud.domain.model.fraud.*;
import com.ksa.financing.fraud.domain.model.rule.RuleEvaluationResult;
import com.ksa.financing.fraud.domain.port.in.EvaluateFraudEventUseCase;
import com.ksa.financing.fraud.domain.port.out.FraudEvaluationRepository;
import com.ksa.financing.fraud.domain.port.out.FraudEventRepository;
import com.ksa.financing.fraud.domain.port.out.FraudRuleRepository;
import com.ksa.financing.fraud.domain.service.FraudDecisionEngine;
import com.ksa.financing.fraud.domain.service.FraudRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core fraud evaluation orchestrator — Sprint 1.
 * Pipeline: receive → persist event → load rules → evaluate → decide → persist result → generate alerts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluateFraudEventService implements EvaluateFraudEventUseCase {

    private final FraudEventRepository fraudEventRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudEvaluationRepository fraudEvaluationRepository;
    private final FraudRuleEngine fraudRuleEngine;
    private final FraudDecisionEngine fraudDecisionEngine;

    @Override
    @Transactional
    public FraudEvaluationResult evaluate(UUID tenantId, FraudEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("Evaluating fraud event eventId={} type={} customer={} tenant={}",
                event.eventId(), event.eventType(), event.customerId(), tenantId);

        // Step 1: Idempotency check — if already evaluated, return cached result
        var existingEval = fraudEvaluationRepository.findByTenantAndEventId(tenantId, event.eventId());
        if (existingEval.isPresent()) {
            log.info("Fraud event already evaluated eventId={}, returning cached result", event.eventId());
            return existingEval.get();
        }

        // Step 2: Persist the raw fraud event
        var savedEvent = fraudEventRepository.save(event);

        // Step 3: Load active rules for this tenant
        var pageQuery = new com.ksa.financing.infra.pagination.PageQuery(
                0, 1000, java.util.List.of(), java.util.List.of(), null);
        var rulesPage = fraudRuleRepository.findActiveByTenant(tenantId, pageQuery);
        var activeRules = rulesPage.content();
        if (activeRules.isEmpty()) {
            log.warn("No active fraud rules found for tenant={}", tenantId);
            return buildAllowResult(tenantId, savedEvent, startTime);
        }

        // Step 4: Evaluate all rules against the enriched event
        var compositeScore = fraudRuleEngine.evaluate(savedEvent, activeRules);

        // Step 5: Determine final decision (BLOCK > HOLD > ALERT > ALLOW)
        var decision = fraudDecisionEngine.determineDecision(compositeScore);
        var blockType = fraudDecisionEngine.determineBlockType(compositeScore);
        var blockReason = fraudDecisionEngine.buildBlockReason(compositeScore);

        long evaluationTimeMs = System.currentTimeMillis() - startTime;

        // Step 6: Build and persist evaluation result
        var result = new FraudEvaluationResult(
                UUID.randomUUID(),
                tenantId,
                savedEvent.eventId(),
                savedEvent.id(),
                savedEvent.customerId(),
                decision,
                blockType,
                compositeScore.score(),
                compositeScore.riskLevel(),
                compositeScore.triggeredRules(),
                blockReason,
                resolveBlockDurationHours(compositeScore),
                buildCustomerMessage(decision),
                evaluationTimeMs,
                LocalDateTime.now()
        );

        var savedResult = fraudEvaluationRepository.save(result);

        log.info("Fraud evaluation completed eventId={} decision={} score={} riskLevel={} triggeredRules={} timeMs={}",
                event.eventId(), decision, compositeScore.score(), compositeScore.riskLevel(),
                compositeScore.triggeredRules().size(), evaluationTimeMs);

        return savedResult;
    }

    private FraudEvaluationResult buildAllowResult(UUID tenantId, FraudEvent event, long startTime) {
        long evaluationTimeMs = System.currentTimeMillis() - startTime;
        var result = new FraudEvaluationResult(
                UUID.randomUUID(), tenantId,
                event.eventId(), event.id(), event.customerId(),
                FraudDecision.ALLOW, null,
                0, "LOW",
                java.util.List.of(),
                null, null, null,
                evaluationTimeMs, LocalDateTime.now()
        );
        return fraudEvaluationRepository.save(result);
    }

    private Integer resolveBlockDurationHours(FraudCompositeScore score) {
        return score.triggeredRules().stream()
                .filter(RuleEvaluationResult::triggered)
                .filter(r -> r.blockType() == FraudBlockType.TEMPORARY)
                .map(r -> 72) // default 72h for temporary blocks
                .findFirst()
                .orElse(null);
    }

    private String buildCustomerMessage(FraudDecision decision) {
        return switch (decision) {
            case BLOCK -> "Your request has been blocked for security reasons. Please contact support.";
            case HOLD -> "Your request is under review. You will be notified shortly.";
            case ALERT -> null; // silent alert — no customer message
            case ALLOW -> null;
        };
    }
}
