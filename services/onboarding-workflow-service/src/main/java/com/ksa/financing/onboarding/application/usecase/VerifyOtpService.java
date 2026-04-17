package com.ksa.financing.onboarding.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.domain.model.OtpVerifiedSignal;
import com.ksa.financing.onboarding.domain.port.in.VerifyOtpUseCase;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that signals the running onboarding workflow that OTP verification has completed.
 *
 * <p>This service does NOT call the OTP verify activity directly. The REST controller
 * orchestrates the full flow: (1) call KYC Adapter to verify OTP, (2) call IDS to create
 * Keycloak user, (3) then invoke this service to signal the workflow with the keycloakUserId.
 * JWT tokens are returned by the controller from step 2.</p>
 */
@Service
public class VerifyOtpService implements VerifyOtpUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyOtpService.class);

    private final WorkflowClient workflowClient;

    public VerifyOtpService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Override
    public VerifyOtpResult verify(String workflowId, String nationalId, String otpCode,
                                   String otpRequestId, String keycloakUserId, DeviceInfo deviceInfo) {
        log.info("Signaling OTP verified for workflow: {}", workflowId);

        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);
            OtpVerifiedSignal signal = new OtpVerifiedSignal(nationalId, keycloakUserId, deviceInfo);
            workflow.otpVerified(signal);

            // Poll state to confirm signal was processed
            var state = workflow.getState();
            String status = state.getCurrentStep() != null ? state.getCurrentStep().name() : "UNKNOWN";

            log.info("OTP verified signal sent for workflow: {}, current step: {}", workflowId, status);
            return new VerifyOtpResult(workflowId, status, null, null, 0);
        } catch (Exception e) {
            log.error("Failed to signal OTP verified for workflow: {}", workflowId, e);
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to signal OTP verification for workflow: " + workflowId, e);
        }
    }
}
