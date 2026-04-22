package com.ksa.islamic.orchestration.activity.collections;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Long-running dunning workflow for an overdue loan.
 *
 * State machine (SAMA-compliant dunning stages):
 *
 *  PRE_DUE_REMINDER (T-3 days)
 *       ↓ due date passes
 *  DUE_DATE          → wait 3 days grace
 *       ↓ no payment
 *  GRACE_PERIOD      → wait 7 days
 *       ↓ no payment
 *  SOFT_COLLECTION   → calls, SMS, email — wait 15 days
 *       ↓ no payment
 *  HARD_COLLECTION   → field visits, formal notices — wait 30 days
 *       ↓ no payment
 *  LEGAL             → legal notice issued — wait 60 days
 *       ↓ no payment
 *  WRITE_OFF         → loan written off, report to SAMA
 *
 * Can be terminated at any stage via paymentReceived signal.
 */
@WorkflowInterface
public interface DunningWorkflow {

    @WorkflowMethod
    DunningResult execute(DunningRequest request);

    /** Payment received — close dunning case */
    @SignalMethod
    void paymentReceived(String paymentId, String paidAmount);

    /** Ops escalation override — jump to next stage immediately */
    @SignalMethod
    void escalateStage(String newStage, String reason, String escalatedBy);

    /** Ops deferral — add extra waiting days at current stage */
    @SignalMethod
    void deferStage(int additionalDays, String reason);

    @QueryMethod
    DunningStatus getCurrentStatus();

    // ══════════════════════════════════════════════════════
    // REQUEST
    // ══════════════════════════════════════════════════════

    record DunningRequest(
            String tenantId,
            String loanId,
            String customerId,
            String scheduleId,
            String mobileNumber,
            int initialDpd,             // days past due when dunning starts
            String outstandingAmount
    ) {}

    // ══════════════════════════════════════════════════════
    // QUERIES
    // ══════════════════════════════════════════════════════

    record DunningStatus(
            String loanId,
            String currentStage,
            int dpd,
            String outstandingAmount,
            boolean closed,
            String closureReason         // "PAYMENT_RECEIVED" | "WRITE_OFF" | "MANUAL_CLOSE"
    ) {}

    // ══════════════════════════════════════════════════════
    // RESULT
    // ══════════════════════════════════════════════════════

    record DunningResult(
            String loanId,
            String finalStage,
            boolean closedByPayment,
            boolean writtenOff
    ) {}
}
