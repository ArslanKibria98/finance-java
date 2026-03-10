package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateApprovalWorkflowsRequest(
    @Valid List<ApprovalWorkflowItem> workflows
) {
    public record ApprovalWorkflowItem(
        String workflowType,
        String nameEn,
        String nameAr,
        String description,
        String templateSource,
        boolean active,
        int priority,
        List<ApprovalConditionItem> conditions,
        List<ApprovalActionItem> actions
    ) {}

    public record ApprovalConditionItem(
        String field,
        String operator,
        String value,
        int sortOrder
    ) {}

    public record ApprovalActionItem(
        String actionType,
        String configuration,
        int sortOrder
    ) {}
}
