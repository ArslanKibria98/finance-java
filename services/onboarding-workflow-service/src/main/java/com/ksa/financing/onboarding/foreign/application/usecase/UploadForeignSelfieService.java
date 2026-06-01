package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignSelfieSubmittedSignal;
import com.ksa.financing.onboarding.foreign.domain.port.in.UploadForeignSelfieUseCase;
import org.springframework.stereotype.Service;

@Service
public class UploadForeignSelfieService implements UploadForeignSelfieUseCase {

    private final ForeignWorkflowClient client;

    public UploadForeignSelfieService(ForeignWorkflowClient client) {
        this.client = client;
    }

    @Override
    public UploadSelfieResult submit(String workflowId, String selfieImageBase64, DeviceInfo deviceInfo) {
        // Counter-based polling — workflow may loop on FACIA face-mismatch without
        // killing the workflow, so we wait for selfieAttempts to bump.
        ForeignOnboardingState before = client.getState(workflowId);
        int beforeAttempts = before.getSelfieAttempts();

        client.stub(workflowId).selfieSubmitted(new ForeignSelfieSubmittedSignal(selfieImageBase64, deviceInfo));
        ForeignOnboardingState state = client.awaitSelfieAttempt(workflowId, beforeAttempts, 60, 1000);
        if (state.getCurrentStep() == ForeignOnboardingStep.FAILED) {
            return new UploadSelfieResult(workflowId, ForeignOnboardingStep.FAILED,
                    state.getFaciaFaceMatchReferenceId(), state.getFaceMatchScore(),
                    state.getCustomerId(), state.getWalletId(),
                    "Selfie verification failed", state.getFailureReason());
        }
        if (state.getFailureReason() != null
                && state.getCurrentStep() != ForeignOnboardingStep.SELFIE_VERIFIED) {
            // Face match declined — user retries selfie. Workflow still alive.
            return new UploadSelfieResult(workflowId, ForeignOnboardingStep.DATA_CONFIRMED,
                    state.getFaciaFaceMatchReferenceId(), state.getFaceMatchScore(),
                    null, null,
                    "Selfie verification declined", state.getFailureReason());
        }
        return new UploadSelfieResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : ForeignOnboardingStep.SELFIE_VERIFIED,
                state.getFaciaFaceMatchReferenceId(),
                state.getFaceMatchScore(),
                state.getCustomerId(),
                state.getWalletId(),
                "Selfie verified, customer + wallet created",
                null);
    }
}
