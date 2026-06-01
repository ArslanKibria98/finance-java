package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingRequest;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.workflow.ForeignOnboardingWorkflow;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
public class ForeignWorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(ForeignWorkflowClient.class);

    private final WorkflowClient workflowClient;
    private final OnboardingSessionRecorder recorder;
    private final String taskQueue;

    public ForeignWorkflowClient(WorkflowClient workflowClient,
                                 OnboardingSessionRecorder recorder,
                                 @Value("${foreign.onboarding.task-queue:foreign-onboarding-queue}") String taskQueue) {
        this.workflowClient = workflowClient;
        this.recorder = recorder;
        this.taskQueue = taskQueue;
    }

    /** Workflow ID = {@code foreign-onboarding-{sha256(email).first16hex}}. */
    public String workflowIdFor(String email) {
        String key = email == null ? "" : email.trim().toLowerCase();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return "foreign-onboarding-" + sb;
        } catch (NoSuchAlgorithmException e) {
            return "foreign-onboarding-" + Integer.toHexString(key.hashCode());
        }
    }

    public String start(ForeignOnboardingRequest request) {
        String workflowId = workflowIdFor(request.email());
        recorder.recordSessionStart(workflowId, OnboardingSessionRecorder.FlowType.FOREIGN,
                parseTenantId(request.tenantId()), request.email(), request.mobileNumber(), null);
        try {
            ForeignOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    ForeignOnboardingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(taskQueue)
                            .build());
            WorkflowClient.start(workflow::execute, request);
            log.info("Foreign onboarding workflow started: workflowId={}", workflowId);
            return workflowId;
        } catch (WorkflowExecutionAlreadyStarted dup) {
            // Same email retried /initiate while a workflow already exists. Terminate
            // the stale run and start fresh so the user gets a new OTP cycle (matches
            // mobile-app retry expectations). If termination fails we just return the
            // existing workflowId — caller can recover by resuming with prior OTPs.
            log.warn("Foreign workflow already started for workflowId={} — terminating and restarting", workflowId);
            try {
                WorkflowStub stale = workflowClient.newUntypedWorkflowStub(workflowId);
                stale.terminate("Restarting onboarding from /initiate");
            } catch (Exception term) {
                log.warn("Failed to terminate stale Foreign workflow {}: {}", workflowId, term.getMessage());
                return workflowId;
            }
            ForeignOnboardingWorkflow fresh = workflowClient.newWorkflowStub(
                    ForeignOnboardingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(taskQueue)
                            .build());
            WorkflowClient.start(fresh::execute, request);
            log.info("Foreign onboarding workflow restarted: workflowId={}", workflowId);
            return workflowId;
        }
    }

    private static UUID parseTenantId(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    public ForeignOnboardingWorkflow stub(String workflowId) {
        return workflowClient.newWorkflowStub(ForeignOnboardingWorkflow.class, workflowId);
    }

    public ForeignOnboardingState getState(String workflowId) {
        try {
            return stub(workflowId).getState();
        } catch (Exception e) {
            log.warn("Failed to query foreign workflow state for {}: {}", workflowId, e.getMessage());
            ForeignOnboardingState empty = new ForeignOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }

    /**
     * Poll until {@code passportAttempts} on the workflow state increases past the
     * provided baseline (i.e. the latest passport upload has been processed by the
     * workflow loop), or the workflow terminates (FAILED). Used by upload-passport
     * so a FACIA decline does NOT have to kill the workflow — the user can retry.
     */
    public ForeignOnboardingState awaitPassportAttempt(String workflowId, int beforeAttempts,
                                                       int maxRetries, long sleepMillis) {
        return awaitAttempt(workflowId, ForeignOnboardingState::getPassportAttempts, beforeAttempts,
                maxRetries, sleepMillis);
    }

    public ForeignOnboardingState awaitSelfieAttempt(String workflowId, int beforeAttempts,
                                                     int maxRetries, long sleepMillis) {
        return awaitAttempt(workflowId, ForeignOnboardingState::getSelfieAttempts, beforeAttempts,
                maxRetries, sleepMillis);
    }

    public ForeignOnboardingState awaitOtpAttempt(String workflowId, int beforeAttempts,
                                                  int maxRetries, long sleepMillis) {
        return awaitAttempt(workflowId, ForeignOnboardingState::getOtpAttempts, beforeAttempts,
                maxRetries, sleepMillis);
    }

    private ForeignOnboardingState awaitAttempt(String workflowId,
                                                java.util.function.ToIntFunction<ForeignOnboardingState> counter,
                                                int baseline,
                                                int maxRetries, long sleepMillis) {
        try {
            ForeignOnboardingWorkflow stub = stub(workflowId);
            for (int i = 0; i < maxRetries; i++) {
                ForeignOnboardingState s = stub.getState();
                if (counter.applyAsInt(s) > baseline) return s;
                if (s.getCurrentStep() == ForeignOnboardingStep.FAILED) return s;
                Thread.sleep(sleepMillis);
            }
            return stub.getState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ForeignOnboardingState empty = new ForeignOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        } catch (Exception e) {
            log.warn("Could not query foreign workflow state for {}: {}", workflowId, e.getMessage());
            ForeignOnboardingState empty = new ForeignOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }

    public ForeignOnboardingState awaitStep(String workflowId, ForeignOnboardingStep expectedStep,
                                            int maxRetries, long sleepMillis) {
        try {
            ForeignOnboardingWorkflow stub = stub(workflowId);
            for (int i = 0; i < maxRetries; i++) {
                ForeignOnboardingState s = stub.getState();
                if (s.getCurrentStep() != null
                        && s.getCurrentStep().ordinal() >= expectedStep.ordinal()) {
                    return s;
                }
                if (s.getCurrentStep() == ForeignOnboardingStep.FAILED) return s;
                Thread.sleep(sleepMillis);
            }
            return stub.getState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ForeignOnboardingState empty = new ForeignOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        } catch (Exception e) {
            log.warn("Could not query foreign workflow state for {}: {}", workflowId, e.getMessage());
            ForeignOnboardingState empty = new ForeignOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }
}
