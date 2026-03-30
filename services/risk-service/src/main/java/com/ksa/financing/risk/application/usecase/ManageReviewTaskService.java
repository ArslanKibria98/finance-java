package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.review.ReviewAction;
import com.ksa.financing.risk.domain.model.review.ReviewTask;
import com.ksa.financing.risk.domain.model.review.ReviewTaskStatus;
import com.ksa.financing.risk.domain.port.in.ManageReviewTaskUseCase;
import com.ksa.financing.risk.domain.port.out.ReviewTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageReviewTaskService implements ManageReviewTaskUseCase {

    private final ReviewTaskRepository reviewTaskRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewTask getById(UUID tenantId, UUID taskId) {
        return reviewTaskRepository.findById(tenantId, taskId)
                .orElseThrow(() -> NotFoundException.forEntity("ReviewTask", taskId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewTask> getPendingMakerTasks(UUID tenantId) {
        return reviewTaskRepository.findByStatus(tenantId, ReviewTaskStatus.PENDING_MAKER);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewTask> getPendingApproverTasks(UUID tenantId) {
        return reviewTaskRepository.findByStatus(tenantId, ReviewTaskStatus.PENDING_APPROVER);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewTask> getBySession(UUID tenantId, UUID sessionId) {
        return reviewTaskRepository.findBySessionId(tenantId, sessionId);
    }

    @Override
    @Transactional
    public ReviewTask makerRecommend(UUID tenantId, UUID taskId, MakerRecommendCommand command) {
        var task = reviewTaskRepository.findById(tenantId, taskId)
                .orElseThrow(() -> NotFoundException.forEntity("ReviewTask", taskId.toString()));

        if (task.getStatus() != ReviewTaskStatus.PENDING_MAKER) {
            throw new BusinessException("RISK.REVIEW.INVALID_STATE",
                    "Task is not in PENDING_MAKER state: " + task.getStatus());
        }

        task.setMakerId(command.makerId());
        task.setMakerRecommendation(command.recommendation());
        task.setMakerComment(command.comment());
        task.setMakerAccountStatus(command.accountStatus());
        task.setMakerComplianceStatus(command.complianceStatus());
        task.setMakerActionAt(Instant.now());
        task.setStatus(ReviewTaskStatus.PENDING_APPROVER);
        task.setUpdatedAt(Instant.now());

        var saved = reviewTaskRepository.save(task);
        log.info("Maker recommendation submitted for task: {} recommendation: {}", taskId, command.recommendation());
        return saved;
    }

    @Override
    @Transactional
    public ReviewTask approverDecide(UUID tenantId, UUID taskId, ApproverDecideCommand command) {
        var task = reviewTaskRepository.findById(tenantId, taskId)
                .orElseThrow(() -> NotFoundException.forEntity("ReviewTask", taskId.toString()));

        if (task.getStatus() != ReviewTaskStatus.PENDING_APPROVER) {
            throw new BusinessException("RISK.REVIEW.INVALID_STATE",
                    "Task is not in PENDING_APPROVER state: " + task.getStatus());
        }

        // Maker and approver must be different users
        if (task.getMakerId() != null && task.getMakerId().equals(command.approverId())) {
            throw new BusinessException("RISK.REVIEW.SAME_USER",
                    "Maker and approver must be different users");
        }

        task.setApproverId(command.approverId());
        task.setApproverAction(command.action());
        task.setApproverComment(command.comment());
        task.setApproverAccountStatus(command.accountStatus());
        task.setApproverComplianceStatus(command.complianceStatus());
        task.setApproverActionAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        if (command.action() == ReviewAction.APPROVE) {
            task.setStatus(ReviewTaskStatus.APPROVED);
        } else if (command.action() == ReviewAction.REJECT) {
            task.setStatus(ReviewTaskStatus.REJECTED);
        } else if (command.action() == ReviewAction.ESCALATE) {
            task.setStatus(ReviewTaskStatus.ESCALATED);
        } else if (command.action() == ReviewAction.RETURN_TO_MAKER) {
            task.setStatus(ReviewTaskStatus.PENDING_MAKER);
            task.setApproverId(null);
            task.setApproverAction(null);
        }

        var saved = reviewTaskRepository.save(task);
        log.info("Approver decision for task: {} action: {}", taskId, command.action());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewTask> getSlaBreachedTasks(UUID tenantId) {
        return reviewTaskRepository.findBySlaDeadlineBefore(tenantId, Instant.now());
    }
}
