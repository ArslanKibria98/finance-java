package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.SelfieSubmittedSignal;
import com.ksa.financing.onboarding.canada.domain.port.in.SubmitSelfieUseCase;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import org.springframework.stereotype.Service;

@Service
public class SubmitSelfieService implements SubmitSelfieUseCase {

    private final CanadaWorkflowClient client;

    public SubmitSelfieService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public SubmitSelfieResult submit(String workflowId, String selfieImageBase64, DeviceInfo deviceInfo) {
        CanadaOnboardingState before = client.getState(workflowId);
        int beforeAttempts = before.getSelfieAttempts();

        client.stub(workflowId).selfieSubmitted(new SelfieSubmittedSignal(selfieImageBase64, deviceInfo));
        CanadaOnboardingState state = client.awaitSelfieAttempt(workflowId, beforeAttempts, 60, 1000);
        if (state.getCurrentStep() == CanadaOnboardingStep.FAILED) {
            return new SubmitSelfieResult(workflowId, CanadaOnboardingStep.FAILED,
                    state.getFaciaFaceMatchReferenceId(), state.getFaceMatchScore(),
                    state.getCustomerId(), state.getWalletId(),
                    "Selfie verification failed", state.getFailureReason());
        }
        if (state.getFailureReason() != null
                && state.getCurrentStep() != CanadaOnboardingStep.SELFIE_VERIFIED) {
            return new SubmitSelfieResult(workflowId, CanadaOnboardingStep.DOC_CONFIRMED,
                    state.getFaciaFaceMatchReferenceId(), state.getFaceMatchScore(),
                    null, null,
                    "Selfie verification declined", state.getFailureReason());
        }
        return new SubmitSelfieResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.SELFIE_VERIFIED,
                state.getFaciaFaceMatchReferenceId(),
                state.getFaceMatchScore(),
                state.getCustomerId(),
                state.getWalletId(),
                "Selfie verified, customer + wallet created",
                null);
    }
}
