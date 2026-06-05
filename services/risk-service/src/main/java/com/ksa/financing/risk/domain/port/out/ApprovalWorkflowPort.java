package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.approval.ApprovalWorkflowRule;

import java.util.List;
import java.util.UUID;

public interface ApprovalWorkflowPort {

    List<ApprovalWorkflowRule> fetchWorkflowRules(UUID tenantId, UUID productId, String authToken);
}
