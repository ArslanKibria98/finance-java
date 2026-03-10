package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.domain.model.NafathInitiateSignal;
import com.ksa.financing.onboarding.domain.model.OnboardingStep;
import com.ksa.financing.onboarding.domain.port.in.InitiateNafathUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import io.temporal.client.WorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that signals the running onboarding workflow to initiate Nafath
 * (national digital identity) verification.
 *
 * <p>After sending the signal, this service polls the workflow state until the
 * Nafath activity completes and the random number / session ID become available.
 * The mobile app displays the random number for the user to select in the
 * Nafath app on their device.</p>
 */
@Service
public class InitiateNafathService implements InitiateNafathUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitiateNafathService.class);

    private final WorkflowClient workflowClient;

    public InitiateNafathService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Override
    public InitiateNafathResult initiate(String workflowId, DeviceInfo deviceInfo) {
        log.info("Signaling Nafath initiation for workflow: {}", workflowId);

        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);

            NafathInitiateSignal signal = new NafathInitiateSignal(deviceInfo);
            workflow.nafathInitiate(signal);

            // Poll until Nafath is initiated (workflow runs Nafath activity after signal)
            // Brief poll with retry to give the workflow time to process the signal
            var state = workflow.getState();
            int maxRetries = 10;
            int retryCount = 0;
            while (state.getCurrentStep() != OnboardingStep.NAFATH_INITIATED
                    && state.getCurrentStep() != OnboardingStep.FAILED
                    && retryCount < maxRetries) {
                Thread.sleep(500);
                state = workflow.getState();
                retryCount++;
            }

            String status = state.getCurrentStep() != null ? state.getCurrentStep().name() : "UNKNOWN";
            log.info("Nafath initiation signal processed for workflow: {}, status: {}, randomNumber: {}",
                    workflowId, status, state.getNafathRandomNumber());

            return new InitiateNafathResult(
                    workflowId,
                    status,
                    state.getNafathRandomNumber(),
                    state.getNafathSessionId(),
                    state.getNafathTransactionId()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Interrupted while waiting for Nafath initiation for workflow: " + workflowId, e);
        } catch (Exception e) {
            log.error("Failed to signal Nafath initiation for workflow: {}", workflowId, e);
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to initiate Nafath for workflow: " + workflowId, e);
        }
    }
}
