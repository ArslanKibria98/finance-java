package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingRequest;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.workflow.CanadaOnboardingWorkflow;
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

/**
 * Thin wrapper around Temporal {@link WorkflowClient} for the Canada onboarding workflow.
 *
 * <p>Centralises workflow ID generation, start, signal lookup, and query — so use case
 * services don't each duplicate this Temporal boilerplate.</p>
 */
@Component
public class CanadaWorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(CanadaWorkflowClient.class);

    private final WorkflowClient workflowClient;
    private final OnboardingSessionRecorder recorder;
    private final String taskQueue;

    public CanadaWorkflowClient(WorkflowClient workflowClient,
                                OnboardingSessionRecorder recorder,
                                @Value("${canada.onboarding.task-queue:canada-onboarding-queue}") String taskQueue) {
        this.workflowClient = workflowClient;
        this.recorder = recorder;
        this.taskQueue = taskQueue;
    }

    /** Workflow ID = {@code canada-onboarding-{sha256(email).first16hex}}. */
    public String workflowIdFor(String email) {
        String key = email == null ? "" : email.trim().toLowerCase();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return "canada-onboarding-" + sb;
        } catch (NoSuchAlgorithmException e) {
            return "canada-onboarding-" + Integer.toHexString(key.hashCode());
        }
    }

    /** Starts a new workflow asynchronously and returns the workflow ID. */
    public String start(CanadaOnboardingRequest request) {
        String workflowId = workflowIdFor(request.email());
        // Persist the session row BEFORE starting the workflow so any failure during start
        // still leaves an audit trail.
        recorder.recordSessionStart(workflowId, OnboardingSessionRecorder.FlowType.CANADA,
                parseTenantId(request.tenantId()), request.email(), request.mobileNumber(), null);
        try {
            CanadaOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CanadaOnboardingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(taskQueue)
                            .build());
            WorkflowClient.start(workflow::execute, request);
            log.info("Canada onboarding workflow started: workflowId={}", workflowId);
            return workflowId;
        } catch (WorkflowExecutionAlreadyStarted dup) {
            // Same email retried /initiate while a workflow already exists. Terminate
            // the stale run and start fresh so the user gets a new OTP cycle.
            log.warn("Canada workflow already started for workflowId={} — terminating and restarting", workflowId);
            try {
                WorkflowStub stale = workflowClient.newUntypedWorkflowStub(workflowId);
                stale.terminate("Restarting onboarding from /initiate");
            } catch (Exception term) {
                log.warn("Failed to terminate stale Canada workflow {}: {}", workflowId, term.getMessage());
                return workflowId;
            }
            CanadaOnboardingWorkflow fresh = workflowClient.newWorkflowStub(
                    CanadaOnboardingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(taskQueue)
                            .build());
            WorkflowClient.start(fresh::execute, request);
            log.info("Canada onboarding workflow restarted: workflowId={}", workflowId);
            return workflowId;
        }
    }

    private static UUID parseTenantId(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    /** Returns a typed stub bound to an existing workflow execution. */
    public CanadaOnboardingWorkflow stub(String workflowId) {
        return workflowClient.newWorkflowStub(CanadaOnboardingWorkflow.class, workflowId);
    }

    /** Reads the current workflow state via the {@code getState} query. */
    public CanadaOnboardingState getState(String workflowId) {
        try {
            return stub(workflowId).getState();
        } catch (Exception e) {
            log.warn("Failed to query Canada workflow state for {}: {}", workflowId, e.getMessage());
            CanadaOnboardingState empty = new CanadaOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }

    public CanadaOnboardingState awaitOtpAttempt(String workflowId, int beforeAttempts,
                                                 int maxRetries, long sleepMillis) {
        return awaitAttempt(workflowId, CanadaOnboardingState::getOtpAttempts, beforeAttempts, maxRetries, sleepMillis);
    }

    public CanadaOnboardingState awaitDocumentAttempt(String workflowId, int beforeAttempts,
                                                      int maxRetries, long sleepMillis) {
        return awaitAttempt(workflowId, CanadaOnboardingState::getDocumentAttempts, beforeAttempts, maxRetries, sleepMillis);
    }

    public CanadaOnboardingState awaitSelfieAttempt(String workflowId, int beforeAttempts,
                                                    int maxRetries, long sleepMillis) {
        return awaitAttempt(workflowId, CanadaOnboardingState::getSelfieAttempts, beforeAttempts, maxRetries, sleepMillis);
    }

    private CanadaOnboardingState awaitAttempt(String workflowId,
                                               java.util.function.ToIntFunction<CanadaOnboardingState> counter,
                                               int baseline,
                                               int maxRetries, long sleepMillis) {
        try {
            CanadaOnboardingWorkflow stub = stub(workflowId);
            for (int i = 0; i < maxRetries; i++) {
                CanadaOnboardingState s = stub.getState();
                if (counter.applyAsInt(s) > baseline) return s;
                if (s.getCurrentStep() == com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep.FAILED) return s;
                Thread.sleep(sleepMillis);
            }
            return stub.getState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CanadaOnboardingState empty = new CanadaOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        } catch (Exception e) {
            log.warn("Could not query Canada workflow state for {}: {}", workflowId, e.getMessage());
            CanadaOnboardingState empty = new CanadaOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }

    /**
     * Waits up to {@code maxRetries × sleepMillis} for the workflow to reach {@code expectedStep}
     * (or a later step / FAILED), then returns the current state.
     */
    public CanadaOnboardingState awaitStep(String workflowId,
                                           com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep expectedStep,
                                           int maxRetries, long sleepMillis) {
        try {
            CanadaOnboardingWorkflow stub = stub(workflowId);
            for (int i = 0; i < maxRetries; i++) {
                CanadaOnboardingState state = stub.getState();
                if (state.getCurrentStep() != null
                        && state.getCurrentStep().ordinal() >= expectedStep.ordinal()) {
                    return state;
                }
                if (state.getCurrentStep() == com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep.FAILED) {
                    return state;
                }
                Thread.sleep(sleepMillis);
            }
            return stub.getState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            CanadaOnboardingState empty = new CanadaOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        } catch (Exception e) {
            log.warn("Could not query Canada workflow state for {}: {}", workflowId, e.getMessage());
            CanadaOnboardingState empty = new CanadaOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }
}
