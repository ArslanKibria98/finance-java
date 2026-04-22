package com.ksa.islamic.orchestration.activity.collections;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;

/**
 * Orchestrates a single installment payment end-to-end.
 *
 * Supported payment paths:
 *  - WALLET_AUTO_DEBIT / WALLET_MANUAL → instant debit → complete
 *  - HYPERPAY_MADA / VISA / MASTERCARD / APPLE_PAY → checkout created → wait for callback signal → verify status → complete/fail
 *  - SADAD → bill created → wait for confirmation signal → complete/fail
 *
 * After completion: waterfall allocation applied → ledger GL posted → notification sent.
 */
@WorkflowInterface
public interface InstallmentPaymentWorkflow {

    @WorkflowMethod
    PaymentWorkflowResult execute(PaymentWorkflowRequest request);

    /** HyperPay / SADAD: payment gateway sends callback → signal workflow to proceed */
    @SignalMethod
    void paymentCallbackReceived(PaymentCallbackSignal signal);

    /** Ops/admin: manually complete a stuck payment */
    @SignalMethod
    void forceComplete(String providerTransactionId, String adminNote);

    /** Ops/admin: manually fail a stuck payment */
    @SignalMethod
    void forceCancel(String reason);

    @QueryMethod
    PaymentWorkflowStatus getStatus();

    // ══════════════════════════════════════════════════════
    // REQUEST
    // ══════════════════════════════════════════════════════

    record PaymentWorkflowRequest(
            String tenantId,
            String loanId,
            String customerId,
            String paymentId,
            BigDecimal amount,
            String paymentMethod,       // PaymentMethod enum name
            String idempotencyKey,
            String mobileNumber,
            String walletId             // nullable — for WALLET_* methods
    ) {}

    // ══════════════════════════════════════════════════════
    // SIGNALS
    // ══════════════════════════════════════════════════════

    record PaymentCallbackSignal(
            String checkoutId,
            String providerTransactionId,
            boolean success,
            String resultCode,
            String resultDescription
    ) {}

    // ══════════════════════════════════════════════════════
    // QUERIES
    // ══════════════════════════════════════════════════════

    record PaymentWorkflowStatus(
            String paymentId,
            String status,              // PENDING, PROCESSING, COMPLETED, FAILED
            String currentStep,         // INITIATED, AWAITING_GATEWAY, VERIFYING, ALLOCATING, POSTING_LEDGER, DONE
            String failureReason
    ) {}

    // ══════════════════════════════════════════════════════
    // RESULT
    // ══════════════════════════════════════════════════════

    record PaymentWorkflowResult(
            String paymentId,
            String status,
            String providerTransactionId,
            String failureReason,
            boolean allocationApplied,
            boolean ledgerPosted,
            boolean notificationSent
    ) {}
}
