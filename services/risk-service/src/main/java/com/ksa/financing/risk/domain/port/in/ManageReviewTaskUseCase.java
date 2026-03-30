package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.review.ReviewRecommendation;
import com.ksa.financing.risk.domain.model.review.ReviewAction;
import com.ksa.financing.risk.domain.model.review.ReviewTask;
import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;

import java.util.List;
import java.util.UUID;

public interface ManageReviewTaskUseCase {

    ReviewTask getById(UUID tenantId, UUID taskId);

    List<ReviewTask> getPendingMakerTasks(UUID tenantId);

    List<ReviewTask> getPendingApproverTasks(UUID tenantId);

    List<ReviewTask> getBySession(UUID tenantId, UUID sessionId);

    ReviewTask makerRecommend(UUID tenantId, UUID taskId, MakerRecommendCommand command);

    ReviewTask approverDecide(UUID tenantId, UUID taskId, ApproverDecideCommand command);

    List<ReviewTask> getSlaBreachedTasks(UUID tenantId);

    record MakerRecommendCommand(
            UUID makerId,
            ReviewRecommendation recommendation,
            String comment,
            AccountStatus accountStatus,
            ComplianceStatus complianceStatus
    ) {}

    record ApproverDecideCommand(
            UUID approverId,
            ReviewAction action,
            String comment,
            AccountStatus accountStatus,
            ComplianceStatus complianceStatus
    ) {}
}
