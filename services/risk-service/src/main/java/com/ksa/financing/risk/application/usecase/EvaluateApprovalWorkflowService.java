package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.approval.ApprovalEvaluationResult;
import com.ksa.financing.risk.domain.port.in.EvaluateApprovalWorkflowUseCase;
import com.ksa.financing.risk.domain.port.out.ApprovalWorkflowPort;
import com.ksa.financing.risk.domain.service.ApprovalWorkflowEvaluationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluateApprovalWorkflowService implements EvaluateApprovalWorkflowUseCase {

    private final ApprovalWorkflowPort approvalWorkflowPort;
    private final ApprovalWorkflowEvaluationEngine evaluationEngine;

    @Override
    public ApprovalEvaluationResult evaluate(UUID tenantId, UUID productId, Map<String, String> applicationData, String authToken) {
        log.info("Evaluating approval workflows for product={} tenant={}", productId, tenantId);

        var rules = approvalWorkflowPort.fetchWorkflowRules(tenantId, productId, authToken);
        log.info("Loaded {} approval workflow rules for product={}", rules.size(), productId);

        var result = evaluationEngine.evaluate(rules, applicationData);
        log.info("Approval decision for product={}: {} — matched rule: {} (priority={})",
                productId, result.decision(), result.matchedRuleName(), result.matchedRulePriority());

        return result;
    }
}
