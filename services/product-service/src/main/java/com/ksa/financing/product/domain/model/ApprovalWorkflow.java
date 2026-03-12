package com.ksa.financing.product.domain.model;

import java.util.List;
import java.util.UUID;

public record ApprovalWorkflow(
    UUID id,
    String workflowType,
    String nameEn,
    String nameAr,
    String description,
    String templateSource,
    boolean active,
    int priority,
    List<ApprovalCondition> conditions,
    List<ApprovalAction> actions
) {
    public record ApprovalCondition(
        UUID id,
        String field,
        String operator,
        String value,
        int sortOrder
    ) {}

    public record ApprovalAction(
        UUID id,
        String actionType,
        String configuration,
        int sortOrder
    ) {}
}
