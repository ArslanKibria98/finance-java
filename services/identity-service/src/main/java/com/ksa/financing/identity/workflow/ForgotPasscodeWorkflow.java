package com.ksa.financing.identity.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal workflow for forgot passcode — two-step flow.
 *
 * <ol>
 *   <li>Workflow starts → activity sends OTP to mobile (WAITING_FOR_OTP)</li>
 *   <li>Signal {@code verifyOtp} → activity verifies OTP (WAITING_FOR_RESET)</li>
 *   <li>Signal {@code resetPasscode} → activity resets PIN in Keycloak (COMPLETED)</li>
 * </ol>
 * Workflow expires after 10 minutes if idle.
 */
@WorkflowInterface
public interface ForgotPasscodeWorkflow {

    @WorkflowMethod
    ForgotPasscodeResult execute(ForgotPasscodeWorkflowRequest request);

    /** Step 2 — verify OTP only (no passcode yet). */
    @SignalMethod
    void verifyOtp(VerifyOtpSignal signal);

    /** Step 3 — reset passcode after OTP verified. */
    @SignalMethod
    void resetPasscode(ResetPasscodeSignal signal);

    @QueryMethod
    String getStatus();

    /** Returns the OTP verification message after verifyOtp activity runs (null until then). */
    @QueryMethod
    String getVerifyOtpMessage();

    // ── Inner types ──────────────────────────────────────────────────────────

    record ForgotPasscodeWorkflowRequest(String mobileNumber) {}

    record ForgotPasscodeResult(boolean success, String message) {}

    record VerifyOtpSignal(String otp) {}

    record ResetPasscodeSignal(String newPasscode) {}
}
