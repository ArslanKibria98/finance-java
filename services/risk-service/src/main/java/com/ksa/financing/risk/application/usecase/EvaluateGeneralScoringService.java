package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreCurrent;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreSnapshot;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;
import com.ksa.financing.risk.domain.model.credit.GeneralScoringConfig;
import com.ksa.financing.risk.domain.port.in.EvaluateGeneralScoringUseCase;
import com.ksa.financing.risk.domain.port.out.CreditScoringRepository;
import com.ksa.financing.risk.domain.port.out.GeneralCreditScoringRepository;
import com.ksa.financing.risk.domain.service.CreditScoringDecisionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs the general (onboarding) credit scoring engine.
 * Pulls criteria from {@link GeneralCreditScoringRepository}, not the
 * product-scoped repo. Optionally persists a per-stage snapshot and
 * updates the customer's cached current score row.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluateGeneralScoringService implements EvaluateGeneralScoringUseCase {

    private final GeneralCreditScoringRepository generalRepository;
    private final CreditScoringRepository fieldDefinitionRepository;
    private final CreditScoringDecisionEngine decisionEngine;

    @Override
    @Transactional
    public EligibilityEvaluationResult evaluate(EvaluateInput input) {
        log.info("Evaluating general scoring tenant={} customer={} stage={} answers={}",
                input.tenantId(), input.customerId(), input.stage(),
                input.answers() != null ? input.answers().size() : 0);

        var criteria = generalRepository.findAllCriteria(input.tenantId());
        if (criteria.isEmpty()) {
            log.warn("No general scoring criteria configured for tenant={}", input.tenantId());
            return EligibilityEvaluationResult.noCriteria();
        }

        var fieldDefinitions = fieldDefinitionRepository.findAllFieldDefinitions(input.tenantId());
        var config = generalRepository.findConfig(input.tenantId())
                .orElseGet(() -> GeneralScoringConfig.defaults(input.tenantId()));

        Map<String, String> answers = input.answers() != null ? input.answers() : new HashMap<>();

        var result = decisionEngine.evaluate(
                criteria,
                fieldDefinitions,
                answers,
                config.minPassPercentage(),
                config.greenThreshold(),
                config.amberThreshold()
        );

        log.info("General scoring result tenant={} customer={} stage={}: decision={}, score={}/{} ({}%)",
                input.tenantId(), input.customerId(), input.stage(),
                result.decision(), result.totalScore(), result.maxPossibleScore(),
                result.scorePercentage());

        if (input.persistSnapshot() && input.customerId() != null) {
            persistSnapshotAndCurrent(input, result, answers);
        }

        return result;
    }

    private void persistSnapshotAndCurrent(EvaluateInput input,
                                           EligibilityEvaluationResult result,
                                           Map<String, String> answers) {
        var snapshot = new CustomerCreditScoreSnapshot(
                null,
                input.tenantId(),
                input.customerId(),
                input.workflowId(),
                input.stage(),
                result.scorePercentage(),
                result.totalScore(),
                result.maxPossibleScore(),
                result.decision() != null ? result.decision().name() : "INSUFFICIENT_DATA",
                result.reasonCode(),
                result.matchedCriteria(),
                result.totalCriteria(),
                answers,
                result.details(),
                result.summary(),
                null
        );

        var saved = generalRepository.saveSnapshot(snapshot);

        var current = new CustomerCreditScoreCurrent(
                input.customerId(),
                input.tenantId(),
                result.scorePercentage(),
                result.totalScore(),
                result.maxPossibleScore(),
                saved.decision(),
                result.reasonCode(),
                input.stage(),
                saved.id(),
                null
        );
        generalRepository.upsertCurrent(current);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerCreditScoreCurrent> getCurrentScore(UUID tenantId, UUID customerId) {
        return generalRepository.findCurrentByCustomer(tenantId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerCreditScoreSnapshot> getSnapshotHistory(UUID tenantId, UUID customerId) {
        return generalRepository.findSnapshotsByCustomer(tenantId, customerId);
    }
}
