package com.ksa.financing.lms.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a loan approval operation.
 */
@Data
@Builder
public class ApprovalResult {

    private LoanAccountId loanAccountId;
    private boolean approved;
    private LoanStatus newStatus;
    private LocalDateTime approvalTimestamp;

    // Approval Details
    private String approvalReference;
    private String approverId;
    private String approvalLevel; // L1, L2, COMMITTEE

    // Conditions if any
    private List<String> conditions;
    private List<String> documents;

    // For rejections
    private String rejectionReason;
    private String rejectionCode;

    // Next Steps
    private String nextAction;
    private LocalDateTime nextActionDueDate;

    // Success/Failure
    private boolean success;
    private String errorMessage;

    public static ApprovalResult approved(LoanAccountId loanAccountId, String approvalRef) {
        return ApprovalResult.builder()
            .loanAccountId(loanAccountId)
            .approved(true)
            .newStatus(LoanStatus.APPROVED)
            .approvalReference(approvalRef)
            .approvalTimestamp(LocalDateTime.now())
            .success(true)
            .build();
    }

    public static ApprovalResult rejected(LoanAccountId loanAccountId, String reason) {
        return ApprovalResult.builder()
            .loanAccountId(loanAccountId)
            .approved(false)
            .newStatus(LoanStatus.REJECTED)
            .rejectionReason(reason)
            .approvalTimestamp(LocalDateTime.now())
            .success(true)
            .build();
    }
}