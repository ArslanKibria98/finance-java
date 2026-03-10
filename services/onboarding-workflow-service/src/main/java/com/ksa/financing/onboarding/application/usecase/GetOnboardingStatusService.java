package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.OnboardingState;
import com.ksa.financing.onboarding.domain.model.OnboardingStep;
import com.ksa.financing.onboarding.domain.port.in.GetOnboardingStatusUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that queries the full onboarding workflow state via Temporal's query mechanism.
 *
 * <p>Returns the complete {@link OnboardingState} including current step, workflow metadata,
 * Nafath session info, and any failure reason. If the workflow cannot be queried (e.g., it
 * has already completed or the ID is invalid), returns an error state with the failure reason.</p>
 */
@Service
public class GetOnboardingStatusService implements GetOnboardingStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetOnboardingStatusService.class);

    private final WorkflowClient workflowClient;

    public GetOnboardingStatusService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Override
    public OnboardingState getStatus(String workflowId) {
        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);
            OnboardingState state = workflow.getState();
            log.debug("Retrieved state for workflow: {}, step: {}", workflowId,
                    state.getCurrentStep());
            return state;
        } catch (WorkflowNotFoundException e) {
            log.info("No workflow found for workflowId: {} — returning NOT_STARTED state", workflowId);
            OnboardingState notStartedState = new OnboardingState();
            notStartedState.setWorkflowId(workflowId);
            notStartedState.setCurrentStep(OnboardingStep.INITIATED);
            return notStartedState;
        } catch (Exception e) {
            log.error("Failed to query workflow state: {}", workflowId, e);
            OnboardingState errorState = new OnboardingState();
            errorState.setWorkflowId(workflowId);
            errorState.setCurrentStep(OnboardingStep.FAILED);
            errorState.setFailureReason("Unable to query workflow: " + e.getMessage());
            return errorState;
        }
    }
}
