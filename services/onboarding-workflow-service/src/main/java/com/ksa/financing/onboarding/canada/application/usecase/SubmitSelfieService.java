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
        CanadaOnboardingState state = client.awaitSelfieAttempt(workflowId, beforeAttempts, 120, 1000);
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
        // Face match passed. selfieAttempts bumps BEFORE the workflow runs the
        // create-customer + create-wallet activities and advances to SELFIE_VERIFIED,
        // so the attempt-await above returns while step is still DOC_CONFIRMED and
        // customerId/walletId are null. Await the step transition so the response
        // carries the real advanced state.
        state = client.awaitStep(workflowId, CanadaOnboardingStep.SELFIE_VERIFIED, 120, 1000);
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
