package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;
import com.ksa.financing.risk.domain.port.in.EvaluateEligibilityUseCase;
import com.ksa.financing.risk.domain.port.out.CreditScoringRepository;
import com.ksa.financing.risk.domain.service.CreditScoringDecisionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluateEligibilityService implements EvaluateEligibilityUseCase {

    private final CreditScoringRepository repository;
    private final CreditScoringDecisionEngine decisionEngine;

    @Value("${app.risk.credit-scoring.min-pass-percentage:60}")
    private BigDecimal minPassPercentage;

    @Value("${app.risk.credit-scoring.green-threshold:75}")
    private BigDecimal greenThreshold;

    @Value("${app.risk.credit-scoring.amber-threshold:50}")
    private BigDecimal amberThreshold;

    @Override
    @Transactional(readOnly = true)
    public EligibilityEvaluationResult evaluate(UUID tenantId, UUID productId, Map<String, String> answers) {
        return evaluate(tenantId, productId, answers, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public EligibilityEvaluationResult evaluate(UUID tenantId, UUID productId,
                                                 Map<String, String> answers,
                                                 BigDecimal greenOverride,
                                                 BigDecimal amberOverride) {
        log.info("Evaluating eligibility for product={} tenant={} with {} answers",
                productId, tenantId, answers != null ? answers.size() : 0);

        var criteria = repository.findCriteriaByProductId(tenantId, productId);

        if (criteria.isEmpty()) {
            log.info("No scoring criteria for product={}, returning default eligible", productId);
            return EligibilityEvaluationResult.noCriteria();
        }

        var fieldDefinitions = repository.findAllFieldDefinitions(tenantId);

        BigDecimal green = greenOverride != null ? greenOverride : greenThreshold;
        BigDecimal amber = amberOverride != null ? amberOverride : amberThreshold;

        var result = decisionEngine.evaluate(criteria, fieldDefinitions, answers,
                minPassPercentage, green, amber);

        log.info("Eligibility result for product={}: decision={}, score={}/{} ({}%), green={}, amber={}",
                productId, result.decision(), result.totalScore(),
                result.maxPossibleScore(), result.scorePercentage(), green, amber);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditScoringFieldDefinition> getProductFields(UUID tenantId, UUID productId) {
        log.info("Getting eligibility fields for product={} tenant={}", productId, tenantId);

        var productFields = repository.findFieldDefinitionsByProductId(tenantId, productId);

        if (!productFields.isEmpty()) {
            log.info("Found {} product-specific fields for product={}", productFields.size(), productId);
            return productFields;
        }

        log.info("No product-specific fields, returning all active definitions for tenant={}", tenantId);
        return repository.findAllFieldDefinitions(tenantId).stream()
                .filter(CreditScoringFieldDefinition::active)
                .toList();
    }
}
