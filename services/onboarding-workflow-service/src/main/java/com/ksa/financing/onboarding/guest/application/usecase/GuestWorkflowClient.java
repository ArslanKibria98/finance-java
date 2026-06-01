package com.ksa.financing.onboarding.guest.application.usecase;

import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingRequest;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.workflow.GuestOnboardingWorkflow;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
public class GuestWorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(GuestWorkflowClient.class);

    private final WorkflowClient workflowClient;
    private final OnboardingSessionRecorder recorder;
    private final String taskQueue;

    public GuestWorkflowClient(WorkflowClient workflowClient,
                               OnboardingSessionRecorder recorder,
                               @Value("${guest.onboarding.task-queue:guest-onboarding-queue}") String taskQueue) {
        this.workflowClient = workflowClient;
        this.recorder = recorder;
        this.taskQueue = taskQueue;
    }

    /** Workflow ID = {@code guest-onboarding-{sha256(email).first16hex}}. */
    public String workflowIdFor(String email) {
        String key = email == null ? "" : email.trim().toLowerCase();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return "guest-onboarding-" + sb;
        } catch (NoSuchAlgorithmException e) {
            return "guest-onboarding-" + Integer.toHexString(key.hashCode());
        }
    }

    public String start(GuestOnboardingRequest request) {
        String workflowId = workflowIdFor(request.email());
        recorder.recordSessionStart(workflowId, OnboardingSessionRecorder.FlowType.GUEST,
                parseTenantId(request.tenantId()), request.email(), request.mobileNumber(), null);
        GuestOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                GuestOnboardingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(taskQueue)
                        .build());
        WorkflowClient.start(workflow::execute, request);
        log.info("Guest workflow started: workflowId={}", workflowId);
        return workflowId;
    }

    private static UUID parseTenantId(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    public GuestOnboardingWorkflow stub(String workflowId) {
        return workflowClient.newWorkflowStub(GuestOnboardingWorkflow.class, workflowId);
    }

    public GuestOnboardingState getState(String workflowId) {
        try {
            return stub(workflowId).getState();
        } catch (Exception e) {
            log.warn("Failed to query guest workflow state for {}: {}", workflowId, e.getMessage());
            GuestOnboardingState empty = new GuestOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }

    public GuestOnboardingState awaitStep(String workflowId, GuestOnboardingStep expectedStep,
                                          int maxRetries, long sleepMillis) {
        try {
            GuestOnboardingWorkflow stub = stub(workflowId);
            for (int i = 0; i < maxRetries; i++) {
                GuestOnboardingState s = stub.getState();
                if (s.getCurrentStep() != null
                        && s.getCurrentStep().ordinal() >= expectedStep.ordinal()) {
                    return s;
                }
                if (s.getCurrentStep() == GuestOnboardingStep.FAILED) return s;
                Thread.sleep(sleepMillis);
            }
            return stub.getState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            GuestOnboardingState empty = new GuestOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        } catch (Exception e) {
            log.warn("Could not query guest workflow state for {}: {}", workflowId, e.getMessage());
            GuestOnboardingState empty = new GuestOnboardingState();
            empty.setWorkflowId(workflowId);
            return empty;
        }
    }
}
