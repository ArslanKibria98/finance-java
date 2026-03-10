package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.AdditionalInfoSignal;
import com.ksa.financing.onboarding.domain.port.in.SubmitAdditionalInfoUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import io.temporal.client.WorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that signals the running onboarding workflow with the customer's
 * additional information (employment, salary, and banking details).
 *
 * <p>After this signal, the workflow proceeds to the final automated steps:
 * profile update, salary assignment, wallet creation, and welcome notification.</p>
 */
@Service
public class SubmitAdditionalInfoService implements SubmitAdditionalInfoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitAdditionalInfoService.class);

    private final WorkflowClient workflowClient;

    public SubmitAdditionalInfoService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Override
    public SubmitAdditionalInfoResult submit(String workflowId, AdditionalInfoSignal signal) {
        log.info("Signaling additional info submission for workflow: {}", workflowId);

        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);

            workflow.additionalInfoSubmitted(signal);

            var state = workflow.getState();
            String status = state.getCurrentStep() != null ? state.getCurrentStep().name() : "UNKNOWN";

            log.info("Additional info signal sent for workflow: {}, status: {}", workflowId, status);
            return new SubmitAdditionalInfoResult(workflowId, status, "Additional info submitted");
        } catch (Exception e) {
            log.error("Failed to signal additional info for workflow: {}", workflowId, e);
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to submit additional info for workflow: " + workflowId, e);
        }
    }
}
