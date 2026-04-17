package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.approval.ApprovalEvaluationResult;

import java.util.Map;
import java.util.UUID;

public interface EvaluateApprovalWorkflowUseCase {

    ApprovalEvaluationResult evaluate(UUID tenantId, UUID productId, Map<String, String> applicationData, String authToken);
}
