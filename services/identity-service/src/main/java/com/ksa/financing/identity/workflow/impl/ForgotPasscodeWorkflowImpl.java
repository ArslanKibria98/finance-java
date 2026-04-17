package com.ksa.financing.identity.workflow.impl;

import com.ksa.financing.identity.workflow.ForgotPasscodeWorkflow;
import com.ksa.financing.identity.workflow.activity.ForgotPasscodeActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Two-step signal-driven Temporal workflow for forgot passcode.
 *
 * <pre>
 * STARTED
 *   → sendOtp activity
 * WAITING_FOR_OTP  (10 min)
 *   → verifyOtp signal → verifyOtp activity
 * WAITING_FOR_RESET  (10 min from OTP verified)
 *   → resetPasscode signal → resetPin activity
 * COMPLETED / EXPIRED / FAILED
 * </pre>
 */
public class ForgotPasscodeWorkflowImpl implements ForgotPasscodeWorkflow {

    private static final Logger log = Workflow.getLogger(ForgotPasscodeWorkflowImpl.class);

    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final ForgotPasscodeActivity activity =
            Workflow.newActivityStub(ForgotPasscodeActivity.class, activityOptions);

    private static final int MAX_OTP_ATTEMPTS = 3;

    // ── Signal state ─────────────────────────────────────────────────────────

    private VerifyOtpSignal   otpSignal          = null;
    private boolean           otpSignalReceived  = false;

    private ResetPasscodeSignal resetSignal       = null;
    private boolean             resetSignalReceived = false;

    private String status           = "STARTED";
    private String verifyOtpMessage = null;
    private int    otpAttempts      = 0;

    // ── Workflow ─────────────────────────────────────────────────────────────

    @Override
    public ForgotPasscodeResult execute(ForgotPasscodeWorkflowRequest request) {
        log.info("ForgotPasscodeWorkflow started for mobile ****{}", maskSuffix(request.mobileNumber()));

        // ── Step 1: Send OTP ────────────────────────────────────────────────
        ForgotPasscodeActivity.SendOtpOutput sendResult =
                activity.sendOtp(new ForgotPasscodeActivity.SendOtpInput(request.mobileNumber()));

        if (!sendResult.sent()) {
            status = "FAILED";
            return new ForgotPasscodeResult(false, "Failed to send OTP. Please try again.");
        }

        String identityId = sendResult.identityId();
        log.info("ForgotPasscodeWorkflow: OTP sent to {} — waiting for OTP verification",
                sendResult.maskedMobile());

        // ── Step 2+3: OTP verify loop — up to MAX_OTP_ATTEMPTS retries ───────
        status = "WAITING_FOR_OTP";
        while (otpAttempts < MAX_OTP_ATTEMPTS) {
            otpSignalReceived = false;
            otpSignal = null;

            boolean gotOtp = Workflow.await(Duration.ofMinutes(10), () -> otpSignalReceived);

            if (!gotOtp) {
                status = "EXPIRED";
                log.info("ForgotPasscodeWorkflow: timed out waiting for OTP — mobile ****{}", maskSuffix(request.mobileNumber()));
                return new ForgotPasscodeResult(false, "OTP session expired. Please request a new OTP.");
            }

            status = "VERIFYING_OTP";
            log.info("ForgotPasscodeWorkflow: verifyOtp signal received (attempt {}/{}) — verifying",
                    otpAttempts + 1, MAX_OTP_ATTEMPTS);

            ForgotPasscodeActivity.VerifyOtpOutput verifyResult = activity.verifyOtp(
                    new ForgotPasscodeActivity.VerifyOtpInput(identityId, otpSignal.otp()));

            otpAttempts++;
            verifyOtpMessage = verifyResult.message();

            if (verifyResult.valid()) {
                break; // proceed to reset
            }

            log.warn("ForgotPasscodeWorkflow: OTP invalid (attempt {}/{}) — {}", otpAttempts, MAX_OTP_ATTEMPTS, verifyResult.message());

            if (otpAttempts >= MAX_OTP_ATTEMPTS) {
                status = "FAILED";
                verifyOtpMessage = "Maximum OTP attempts exceeded. Please request a new OTP.";
                log.warn("ForgotPasscodeWorkflow: max OTP attempts reached — mobile ****{}", maskSuffix(request.mobileNumber()));
                return new ForgotPasscodeResult(false, verifyOtpMessage);
            }

            // Allow retry — stay in WAITING_FOR_OTP with the failure message queryable
            status = "WAITING_FOR_OTP";
        }

        log.info("ForgotPasscodeWorkflow: OTP verified — waiting for new passcode");

        // ── Step 4: Wait for resetPasscode signal ────────────────────────────
        status = "WAITING_FOR_RESET";
        boolean gotReset = Workflow.await(Duration.ofMinutes(10), () -> resetSignalReceived);

        if (!gotReset) {
            status = "EXPIRED";
            log.info("ForgotPasscodeWorkflow: timed out waiting for reset — mobile ****{}", maskSuffix(request.mobileNumber()));
            return new ForgotPasscodeResult(false, "Session expired. Please start over.");
        }

        // ── Step 5: Reset PIN via activity ───────────────────────────────────
        status = "RESETTING";
        log.info("ForgotPasscodeWorkflow: resetPasscode signal received — resetting PIN");

        ForgotPasscodeActivity.ResetPinOutput resetResult = activity.resetPin(
                new ForgotPasscodeActivity.ResetPinInput(identityId, resetSignal.newPasscode()));

        status = resetResult.success() ? "COMPLETED" : "FAILED";
        log.info("ForgotPasscodeWorkflow: {} for mobile ****{}", status, maskSuffix(request.mobileNumber()));

        return new ForgotPasscodeResult(resetResult.success(), resetResult.message());
    }

    // ── Signal handlers ──────────────────────────────────────────────────────

    @Override
    public void verifyOtp(VerifyOtpSignal signal) {
        log.info("ForgotPasscodeWorkflow: verifyOtp signal received");
        this.otpSignal         = signal;
        this.otpSignalReceived = true;
    }

    @Override
    public void resetPasscode(ResetPasscodeSignal signal) {
        log.info("ForgotPasscodeWorkflow: resetPasscode signal received");
        this.resetSignal         = signal;
        this.resetSignalReceived = true;
    }

    @Override
    public String getStatus() { return status; }

    @Override
    public String getVerifyOtpMessage() { return verifyOtpMessage; }

    private String maskSuffix(String value) {
        if (value == null || value.length() < 4) return "****";
        return value.substring(value.length() - 4);
    }
}
