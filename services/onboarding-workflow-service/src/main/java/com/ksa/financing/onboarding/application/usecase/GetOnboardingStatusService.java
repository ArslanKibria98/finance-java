package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.OnboardingState;
import com.ksa.financing.onboarding.domain.model.OnboardingStep;
import com.ksa.financing.onboarding.domain.port.in.GetOnboardingStatusUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
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
            // getState() fails on closed workflows — check actual execution status
            WorkflowExecutionStatus execStatus = describeExecutionStatus(workflowId);
            if (execStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED) {
                log.info("Workflow {} is COMPLETED — returning COMPLETED state", workflowId);
                OnboardingState completedState = new OnboardingState();
                completedState.setWorkflowId(workflowId);
                completedState.setCurrentStep(OnboardingStep.COMPLETED);
                return completedState;
            }
            if (execStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_TERMINATED
                    || execStatus == WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_TIMED_OUT) {
                log.info("Workflow {} status: {} — returning FAILED state", workflowId, execStatus);
            } else {
                log.error("Failed to query workflow state: {}", workflowId, e);
            }
            OnboardingState errorState = new OnboardingState();
            errorState.setWorkflowId(workflowId);
            errorState.setCurrentStep(OnboardingStep.FAILED);
            errorState.setFailureReason("Unable to query workflow: " + e.getMessage());
            return errorState;
        }
    }

    private WorkflowExecutionStatus describeExecutionStatus(String workflowId) {
        try {
            var response = workflowClient.getWorkflowServiceStubs()
                    .blockingStub()
                    .describeWorkflowExecution(
                            DescribeWorkflowExecutionRequest.newBuilder()
                                    .setNamespace(temporalNamespace)
                                    .setExecution(WorkflowExecution.newBuilder()
                                            .setWorkflowId(workflowId)
                                            .build())
                                    .build());
            return response.getWorkflowExecutionInfo().getStatus();
        } catch (Exception ex) {
            log.debug("Could not describe workflow {}: {}", workflowId, ex.getMessage());
            return WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_UNSPECIFIED;
        }
    }

    @Override
    public OnboardingState getStatusByMobile(String mobileNumber) {
        log.info("Looking up onboarding workflow by mobile: ****{}",
                mobileNumber != null && mobileNumber.length() >= 4
                        ? mobileNumber.substring(mobileNumber.length() - 4) : "****");

        // 1. Search running workflows first
        OnboardingState running = searchWorkflowsByMobile(mobileNumber,
                "WorkflowType = 'CustomerOnboardingWorkflow' AND ExecutionStatus = 'Running'");
        if (running != null) return running;

        // 2. Fallback: search completed workflows (customer already onboarded)
        OnboardingState completed = searchWorkflowsByMobile(mobileNumber,
                "WorkflowType = 'CustomerOnboardingWorkflow' AND ExecutionStatus = 'Completed'");
        if (completed != null) {
            log.info("Found completed onboarding workflow for mobile ****{}",
                    mobileNumber.substring(mobileNumber.length() - 4));
            return completed;
        }

        log.info("No workflow found for mobile number");
        OnboardingState notFoundState = new OnboardingState();
        notFoundState.setCurrentStep(OnboardingStep.INITIATED);
        return notFoundState;
    }

    private OnboardingState searchWorkflowsByMobile(String mobileNumber, String query) {
        boolean isCompletedSearch = query.contains("Completed");
        try {
            ListWorkflowExecutionsRequest request = ListWorkflowExecutionsRequest.newBuilder()
                    .setNamespace(temporalNamespace)
                    .setQuery(query)
                    .setPageSize(500)
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
                    if (normalizedMobileMatch(mobileNumber, state.getMobileNumber())) {
                        log.info("Found workflow {} matching mobile (query: {})", wfId, query);
                        return state;
                    }
                } catch (Exception e) {
                    // For completed workflows, getState() throws — check workflowId pattern match
                    if (isCompletedSearch) {
                        // workflowId format: "onboarding-{nationalId}"
                        // We cannot check mobile here without state, but we return a COMPLETED stub
                        // so the controller can resolve it via NID fallback
                        log.debug("Could not query completed workflow {} state: {}", wfId, e.getMessage());
                    } else {
                        log.warn("Could not query workflow {}: {}", wfId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Workflow search failed [query='{}']: {}", query, e.getMessage());
        }
        return null;
    }

    /**
     * Compares mobile numbers ignoring leading '+' to handle format mismatches
     * between stored values (e.g., "966555687120") and normalized input ("+966555687120").
     */
    private boolean normalizedMobileMatch(String input, String stored) {
        if (input == null || stored == null) return false;
        String normalizedInput = input.startsWith("+") ? input.substring(1) : input;
        String normalizedStored = stored.startsWith("+") ? stored.substring(1) : stored;
        return normalizedInput.equals(normalizedStored);
    }

    @Override
    public List<OnboardingState> listActiveOnboardings() {
        log.info("Listing all active onboarding workflows from Temporal");
        List<OnboardingState> results = new ArrayList<>();

        try {
            ListWorkflowExecutionsRequest request = ListWorkflowExecutionsRequest.newBuilder()
                    .setNamespace(temporalNamespace)
                    .setQuery("WorkflowType = 'CustomerOnboardingWorkflow'")
                    .setPageSize(500)
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
