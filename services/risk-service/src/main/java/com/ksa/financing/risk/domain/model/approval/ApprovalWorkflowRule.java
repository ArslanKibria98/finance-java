package com.ksa.financing.risk.domain.model.approval;

import java.util.List;
import java.util.UUID;

public record ApprovalWorkflowRule(
    UUID id,
    String workflowType,
    String nameEn,
    int priority,
    boolean active,
    List<Condition> conditions,
    String actionType
) {
    public record Condition(
        String field,
        String operator,
        String value
    ) {}
}
