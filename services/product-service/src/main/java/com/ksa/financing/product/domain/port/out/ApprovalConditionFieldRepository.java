package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.product.domain.model.ApprovalConditionFieldDefinition;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalConditionFieldRepository {

    PageResponse<ApprovalConditionFieldDefinition> findAllWithOptions(UUID tenantId, PageQuery pageQuery);

    Optional<ApprovalConditionFieldDefinition> findById(UUID tenantId, UUID id);

    boolean existsByFieldKey(UUID tenantId, String fieldKey);

    ApprovalConditionFieldDefinition save(UUID tenantId, ApprovalConditionFieldDefinition field);

    ApprovalConditionFieldDefinition update(UUID tenantId, UUID id, ApprovalConditionFieldDefinition field);

    void delete(UUID tenantId, UUID id);
}
