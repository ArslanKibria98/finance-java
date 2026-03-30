package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.OnboardingState;
import com.ksa.financing.onboarding.domain.model.OnboardingStep;
import com.ksa.financing.onboarding.domain.port.in.GetOnboardingStatusUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GetOnboardingStatusService implements GetOnboardingStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetOnboardingStatusService.class);

    private final WorkflowClient workflowClient;

    @Value("${temporal.namespace:default}")
    private String temporalNamespace;

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

    @Override
    public List<OnboardingState> listActiveOnboardings() {
        log.info("Listing all active onboarding workflows from Temporal");
        List<OnboardingState> results = new ArrayList<>();

        try {
            ListWorkflowExecutionsRequest request = ListWorkflowExecutionsRequest.newBuilder()
                    .setNamespace(temporalNamespace)
                    .setQuery("WorkflowType = 'CustomerOnboardingWorkflow' AND ExecutionStatus = 'Running'")
                    .setPageSize(200)
                    .build();

            ListWorkflowExecutionsResponse response = workflowClient.getWorkflowServiceStubs()
                    .blockingStub()
                    .listWorkflowExecutions(request);

            for (WorkflowExecutionInfo info : response.getExecutionsList()) {
                String wfId = info.getExecution().getWorkflowId();
                try {
                    CustomerOnboardingWorkflow wf = workflowClient.newWorkflowStub(
                            CustomerOnboardingWorkflow.class, wfId);
                    OnboardingState state = wf.getState();
                    results.add(state);
                } catch (Exception e) {
                    log.warn("Could not query workflow {}: {}", wfId, e.getMessage());
                    OnboardingState errorState = new OnboardingState();
                    errorState.setWorkflowId(wfId);
                    errorState.setCurrentStep(OnboardingStep.INITIATED);
                    errorState.setFailureReason("Query failed: " + e.getMessage());
                    results.add(errorState);
                }
            }

            log.info("Found {} active onboarding workflows", results.size());
        } catch (Exception e) {
            log.error("Failed to list workflows from Temporal: {}", e.getMessage(), e);
        }

        return results;
    }
}
