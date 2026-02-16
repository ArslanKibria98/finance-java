package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Approval Intent - Domain request for loan approval.
 */
@Data
@Builder
public class ApprovalIntent {

    private String loanAccountId;
    private String approverId;
    private String approverName;
    private String approverRole;

    private ApprovalDecision decision;
    private String approvalNotes;
    private LocalDateTime approvalTimestamp;

    // Risk Assessment
    private String riskScore;
    private String riskCategory; // LOW, MEDIUM, HIGH

    // Conditions if any
    private Map<String, String> conditions;

    // Compliance checks
    private boolean amlCheckPassed;
    private boolean creditCheckPassed;
    private boolean shariaComplianceVerified;

    // For audit trail
    private String approvalReference;
    private String workflowInstanceId;

    public enum ApprovalDecision {
        APPROVED,
        REJECTED,
        PENDING_DOCUMENTATION,
        PENDING_COLLATERAL,
        REFERRED_TO_COMMITTEE
    }
}