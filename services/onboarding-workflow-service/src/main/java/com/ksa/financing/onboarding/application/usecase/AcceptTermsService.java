package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.domain.model.TermsAcceptedSignal;
import com.ksa.financing.onboarding.domain.port.in.AcceptTermsUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import io.temporal.client.WorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that signals the running onboarding workflow that the customer has
 * accepted (or declined) the terms and conditions.
 *
 * <p>Sends a {@link TermsAcceptedSignal} containing the acceptance flag and
 * device trust information. If the customer declines, the workflow will
 * transition to a terminal state.</p>
 */
@Service
public class AcceptTermsService implements AcceptTermsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AcceptTermsService.class);

    private final WorkflowClient workflowClient;

    public AcceptTermsService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Override
    public AcceptTermsResult accept(String workflowId, boolean accepted, DeviceInfo deviceInfo) {
        log.info("Signaling terms acceptance for workflow: {} accepted: {}", workflowId, accepted);

        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);

            TermsAcceptedSignal signal = new TermsAcceptedSignal(accepted, deviceInfo);
            workflow.termsAccepted(signal);

            var state = workflow.getState();
            String status = state.getCurrentStep() != null ? state.getCurrentStep().name() : "UNKNOWN";
            String message = accepted ? "Terms accepted" : "Terms declined";

            log.info("Terms acceptance signal sent for workflow: {}, status: {}", workflowId, status);
            return new AcceptTermsResult(workflowId, status, message);
        } catch (Exception e) {
            log.error("Failed to signal terms acceptance for workflow: {}", workflowId, e);
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to signal terms acceptance for workflow: " + workflowId, e);
        }
    }
}
