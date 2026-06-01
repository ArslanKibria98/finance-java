package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.OnboardingRequest;
import com.ksa.financing.onboarding.domain.model.OnboardingState;
import com.ksa.financing.onboarding.domain.model.OnboardingStep;
import com.ksa.financing.onboarding.domain.port.in.StartOnboardingUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ksa.islamic.orchestration.common.TaskQueue;
import org.springframework.stereotype.Service;


@Service
public class StartOnboardingService implements StartOnboardingUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartOnboardingService.class);

    private final WorkflowClient workflowClient;

    public StartOnboardingService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Override
    public StartOnboardingResult start(OnboardingRequest request) {
        String workflowId = "onboarding-" + request.nationalId();

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TaskQueue.ONBOARDING_QUEUE)
                .build();

        boolean isResume = false;

        try {
            var workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, options);
            WorkflowClient.start(workflow::execute, request);
            log.info("Started new onboarding workflow: {}", workflowId);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.info("Resuming existing onboarding workflow: {}", workflowId);
            isResume = true;
        }

        // Poll workflow state to get OTP request ID and status
        try {
            CustomerOnboardingWorkflow stub = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);

            if (!isResume) {
                // Wait for workflow to process Step 1 (Tahakuk + OTP send)
                OnboardingState state = null;
                int maxRetries = 20;
                int retryCount = 0;
                while (retryCount < maxRetries) {
                    state = stub.getState();
                    if (state.getCurrentStep() != null
                            && state.getCurrentStep() != OnboardingStep.INITIATED) {
                        break;
                    }
                    Thread.sleep(500);
                    retryCount++;
                }

                if (state != null && state.getCurrentStep() == OnboardingStep.FAILED) {
                    return new StartOnboardingResult(
                            workflowId,
                            "FAILED",
                            OnboardingStep.FAILED,
                            null,
                            maskMobile(request.mobileNumber()),
                            state.getGlobalUid(),
                            state.getCustomerId(),
                            state.getFailureReason()
                    );
                }

                OnboardingStep currentStep = state != null ? state.getCurrentStep() : OnboardingStep.INITIATED;
                String status = currentStep != null ? currentStep.name() : "INITIATED";
                return new StartOnboardingResult(
                        workflowId,
                        status,
                        currentStep,
                        state != null ? state.getOtpRequestId() : null,
                        maskMobile(request.mobileNumber()),
                        state != null ? state.getGlobalUid() : null,
                        state != null ? state.getCustomerId() : null,
                        null
                );
            } else {
                // Resume: return current state
                OnboardingState state = stub.getState();
                OnboardingStep currentStep = state.getCurrentStep();
                String status = currentStep != null ? currentStep.name() : "INITIATED";
                return new StartOnboardingResult(
                        workflowId,
                        status,
                        currentStep,
                        state.getOtpRequestId(),
                        maskMobile(request.mobileNumber()),
                        state.getGlobalUid(),
                        state.getCustomerId(),
                        state.getFailureReason()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new StartOnboardingResult(
                    workflowId, "STARTED", OnboardingStep.INITIATED,
                    null, null, null, null, null
            );
        } catch (Exception e) {
            log.warn("Could not query workflow state: {}", e.getMessage());
            return new StartOnboardingResult(
                    workflowId,
                    isResume ? "RESUMING" : "STARTED",
                    OnboardingStep.INITIATED,
                    null, null, null, null, null
            );
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "****";
        }
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
