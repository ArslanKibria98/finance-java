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

    @Override
    @Transactional(readOnly = true)
    public EligibilityEvaluationResult evaluate(UUID tenantId, UUID productId, Map<String, String> answers) {
        log.info("Evaluating eligibility for product={} tenant={} with {} answers",
                productId, tenantId, answers != null ? answers.size() : 0);

        // Load product criteria with rules
        var criteria = repository.findCriteriaByProductId(tenantId, productId);

        if (criteria.isEmpty()) {
            log.info("No scoring criteria for product={}, returning default eligible", productId);
            return EligibilityEvaluationResult.noCriteria();
        }

        // Load all field definitions for the tenant (to resolve field_key from criteria)
        var fieldDefinitions = repository.findAllFieldDefinitions(tenantId);

        // Run decision engine
        var result = decisionEngine.evaluate(criteria, fieldDefinitions, answers, minPassPercentage);

        log.info("Eligibility result for product={}: eligible={}, score={}/{}  ({}%)",
                productId, result.eligible(), result.totalScore(),
                result.maxPossibleScore(), result.scorePercentage());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditScoringFieldDefinition> getProductFields(UUID tenantId, UUID productId) {
        log.info("Getting eligibility fields for product={} tenant={}", productId, tenantId);

        // Get fields that have enabled criteria for this product
        var productFields = repository.findFieldDefinitionsByProductId(tenantId, productId);

        if (!productFields.isEmpty()) {
            log.info("Found {} product-specific fields for product={}", productFields.size(), productId);
            return productFields;
        }

        // Fallback: return all active field definitions
        log.info("No product-specific fields, returning all active definitions for tenant={}", tenantId);
        return repository.findAllFieldDefinitions(tenantId).stream()
                .filter(CreditScoringFieldDefinition::active)
                .toList();
    }
}
