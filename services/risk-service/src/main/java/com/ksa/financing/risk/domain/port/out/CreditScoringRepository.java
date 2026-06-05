package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditScoringRepository {

    PageResponse<CreditScoringFieldDefinition> findActiveFieldDefinitions(String tenantId, PageQuery pageQuery);

    List<CreditScoringFieldDefinition> findAllFieldDefinitions(UUID tenantId);

    Optional<CreditScoringFieldDefinition> findFieldDefinitionById(UUID tenantId, UUID id);

    CreditScoringFieldDefinition saveFieldDefinition(CreditScoringFieldDefinition fieldDefinition);

    CreditScoringFieldDefinition updateFieldDefinition(CreditScoringFieldDefinition fieldDefinition);

    void deleteFieldDefinition(UUID tenantId, UUID id);

    boolean fieldKeyExists(UUID tenantId, String fieldKey, UUID excludeId);

    List<CreditScoringCriteria> findCriteriaByProductId(UUID tenantId, UUID productId);

    void saveCriteria(UUID tenantId, UUID productId, List<CreditScoringCriteria> criteria);

    void deleteCriteriaByProductId(UUID tenantId, UUID productId);

    /**
     * Find field definitions that have enabled criteria for a specific product.
     * Used to return product-specific form fields to the mobile app.
     */
    List<CreditScoringFieldDefinition> findFieldDefinitionsByProductId(UUID tenantId, UUID productId);
}
