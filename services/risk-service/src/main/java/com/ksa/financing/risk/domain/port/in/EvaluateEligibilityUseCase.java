package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EvaluateEligibilityUseCase {

    /**
     * Evaluate customer eligibility against a product's scoring rules.
     *
     * @param tenantId  tenant UUID
     * @param productId product UUID
     * @param answers   customer answers: field_key → value
     * @return evaluation result with score, details, and eligibility decision
     */
    EligibilityEvaluationResult evaluate(UUID tenantId, UUID productId, Map<String, String> answers);

    /**
     * Get the field definitions assigned to a product (for mobile form rendering).
     * Returns only the fields that have enabled criteria configured for the product.
     * Falls back to all active field definitions if no product-specific criteria exist.
     *
     * @param tenantId  tenant UUID
     * @param productId product UUID
     * @return list of field definitions for the product
     */
    List<CreditScoringFieldDefinition> getProductFields(UUID tenantId, UUID productId);
}
