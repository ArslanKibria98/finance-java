package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EvaluateEligibilityUseCase {

    /**
     * Evaluate customer eligibility against a product's scoring rules using
     * the configured (global) green/amber thresholds.
     */
    EligibilityEvaluationResult evaluate(UUID tenantId, UUID productId, Map<String, String> answers);

    /**
     * Evaluate with per-request overrides for the green/amber thresholds — used by
     * services that store thresholds at the product level (e.g. lending product config).
     * Pass {@code null} for either override to fall back to the configured default.
     */
    EligibilityEvaluationResult evaluate(UUID tenantId, UUID productId,
                                         Map<String, String> answers,
                                         BigDecimal greenThresholdOverride,
                                         BigDecimal amberThresholdOverride);

    /**
     * Get the field definitions assigned to a product (for mobile form rendering).
     * Returns only the fields that have enabled criteria configured for the product.
     * Falls back to all active field definitions if no product-specific criteria exist.
     */
    List<CreditScoringFieldDefinition> getProductFields(UUID tenantId, UUID productId);
}
