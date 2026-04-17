package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.ForgotPasscodeUseCase;
import com.ksa.financing.identity.workflow.ForgotPasscodeWorkflow;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import com.ksa.islamic.orchestration.common.TaskQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates the forgot passcode flow via a Temporal workflow.
 *
 * <p>send-otp  → starts {@link ForgotPasscodeWorkflow} asynchronously (workflowId keyed on mobile)
 * <p>verify-otp → signals the running workflow with OTP, polls for WAITING_FOR_RESET or FAILED
 * <p>reset      → signals the running workflow with new passcode, waits for completion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasscodeWorkflowService implements ForgotPasscodeUseCase {

    private final WorkflowClient workflowClient;

    // ── sendOtp ──────────────────────────────────────────────────────────────

    @Override
    public SendOtpResult sendOtp(SendOtpCommand command) {
        String workflowId = workflowIdFor(command.mobileNumber());

        log.info("Starting ForgotPasscodeWorkflow workflowId={} for mobile ****{}",
                workflowId, maskSuffix(command.mobileNumber()));

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TaskQueue.IDENTITY_QUEUE)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(15))
                // TERMINATE_IF_RUNNING: allows "resend OTP" without the old session blocking
                .setWorkflowIdReusePolicy(
                        io.temporal.api.enums.v1.WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_TERMINATE_IF_RUNNING)
                .build();

        ForgotPasscodeWorkflow workflow = workflowClient.newWorkflowStub(ForgotPasscodeWorkflow.class, options);

        // Start async — the workflow will send OTP via activity, then wait for the verifyOtp signal
        WorkflowClient.start(workflow::execute,
                new ForgotPasscodeWorkflow.ForgotPasscodeWorkflowRequest(command.mobileNumber()));

        String maskedMobile = "****" + maskSuffix(command.mobileNumber());
        return new SendOtpResult(true, maskedMobile, workflowId);
    }

    // ── verifyOtp ────────────────────────────────────────────────────────────

    @Override
    public VerifyOtpResult verifyOtp(VerifyOtpCommand command) {
        String workflowId = workflowIdFor(command.mobileNumber());
        log.info("Sending verifyOtp signal to workflowId={}", workflowId);

        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);

        // Send OTP verification signal
        try {
            stub.signal("verifyOtp", new ForgotPasscodeWorkflow.VerifyOtpSignal(command.otp()));
        } catch (Exception e) {
            log.warn("ForgotPasscodeWorkflow not found for workflowId={}: {}", workflowId, e.getMessage());
            throw new BusinessException(
                    ErrorCodes.Identity.SESSION_INVALID,
                    "No active OTP session found. Please request a new OTP.");
        }

        // Poll workflow status until OTP activity completes (typically < 1 second)
        for (int i = 0; i < 20; i++) {
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

            String status;
            try {
                status = stub.query("getStatus", String.class);
            } catch (Exception e) {
                log.warn("Could not query workflow status (workflow may have completed): {}", e.getMessage());
                break;
            }

            if ("WAITING_FOR_RESET".equals(status)) {
                log.info("OTP verified for workflowId={}", workflowId);
                return new VerifyOtpResult(true, "OTP verified successfully.");
            }

            // WAITING_FOR_OTP after a failed attempt = wrong OTP but retry is allowed
            if ("WAITING_FOR_OTP".equals(status)) {
                String msg = null;
                try {
                    msg = stub.query("getVerifyOtpMessage", String.class);
                } catch (Exception e) {
                    log.debug("Could not query verifyOtpMessage: {}", e.getMessage());
                }
                // Only throw if there's a failure message (i.e., a previous attempt failed)
                if (msg != null && !msg.isBlank()) {
                    log.warn("OTP invalid for workflowId={} (retry allowed): {}", workflowId, msg);
                    throwOtpBusinessException(msg);
                }
                // Otherwise still waiting for first signal — keep polling
            }

            if ("FAILED".equals(status) || "EXPIRED".equals(status)) {
                String msg = null;
                try {
                    msg = stub.query("getVerifyOtpMessage", String.class);
                } catch (Exception e) {
                    log.debug("Could not query verifyOtpMessage (workflow may be closed): {}", e.getMessage());
                }
                log.warn("OTP verification failed for workflowId={}: {}", workflowId, msg);
                throwOtpBusinessException(msg);
            }
        }

        throw new BusinessException(
                ErrorCodes.Identity.SESSION_INVALID,
                "OTP verification timed out. Please try again.");
    }

    /**
     * Maps the OTP failure message from the workflow to the appropriate specific error code.
     * Uses KYC error codes so the localized message from errors.properties is shown (not generic "Validation failed").
     */
    private void throwOtpBusinessException(String workflowMessage) {
        if (workflowMessage != null && workflowMessage.toLowerCase().contains("expired")) {
            throw new BusinessException(ErrorCodes.Kyc.OTP_EXPIRED, workflowMessage);
        }
        if (workflowMessage != null && workflowMessage.toLowerCase().contains("maximum")) {
            throw new BusinessException(ErrorCodes.Kyc.OTP_MAX_ATTEMPTS, workflowMessage);
        }
        if (workflowMessage != null && workflowMessage.toLowerCase().contains("no active")) {
            throw new BusinessException(ErrorCodes.Kyc.OTP_REQUEST_NOT_FOUND, workflowMessage);
        }
        throw new BusinessException(
                ErrorCodes.Kyc.OTP_INVALID,
                workflowMessage != null ? workflowMessage : "Invalid OTP. Please try again.");
    }

    // ── resetPasscode ────────────────────────────────────────────────────────

    @Override
    public ResetPasscodeResult resetPasscode(ResetPasscodeCommand command) {
        if (!command.newPasscode().equals(command.confirmPasscode())) {
            throw new BusinessException(
                    ErrorCodes.Identity.PASSCODE_MISMATCH,
                    "New passcode and confirm passcode do not match");
        }

        String workflowId = workflowIdFor(command.mobileNumber());
        log.info("Sending resetPasscode signal to workflowId={}", workflowId);

        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);

        try {
            stub.signal("resetPasscode",
                    new ForgotPasscodeWorkflow.ResetPasscodeSignal(command.newPasscode()));
        } catch (Exception e) {
            log.warn("ForgotPasscodeWorkflow not found or already completed: workflowId={} error={}",
                    workflowId, e.getMessage());
            throw new BusinessException(
                    ErrorCodes.Identity.SESSION_INVALID,
                    "No active session found. Please verify your OTP first.");
        }

        // Wait for workflow to complete (PIN reset activity is < 5 s normally)
        try {
            ForgotPasscodeWorkflow.ForgotPasscodeResult result =
                    stub.getResult(30, TimeUnit.SECONDS, ForgotPasscodeWorkflow.ForgotPasscodeResult.class);

            if (!result.success()) {
                throw new BusinessException(ErrorCodes.Identity.RESET_FAILED, result.message());
            }
            return new ResetPasscodeResult(true, result.message());

        } catch (WorkflowFailedException e) {
            log.warn("ForgotPasscodeWorkflow failed for workflowId={}: {}", workflowId, e.getMessage());
            String cause = e.getCause() != null ? e.getCause().getMessage() : "Passcode reset failed.";
            throw new BusinessException(ErrorCodes.Identity.RESET_FAILED, cause);
        } catch (TimeoutException e) {
            log.warn("ForgotPasscodeWorkflow result timed out for workflowId={}", workflowId);
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Passcode reset timed out. Please try again.");
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Deterministic workflowId — one active session per mobile at a time. Digits only for consistency. */
    private String workflowIdFor(String mobileNumber) {
        String digits = mobileNumber == null ? "" : mobileNumber.replaceAll("[^0-9]", "");
        return "forgot-passcode-" + digits;
    }

    private String maskSuffix(String value) {
        if (value == null || value.length() < 4) return "****";
        return value.substring(value.length() - 4);
    }
}
