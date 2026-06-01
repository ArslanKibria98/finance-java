package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignPassportSubmittedSignal;
import com.ksa.financing.onboarding.foreign.domain.port.in.UploadForeignPassportUseCase;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UploadForeignPassportService implements UploadForeignPassportUseCase {

    private final ForeignWorkflowClient client;

    public UploadForeignPassportService(ForeignWorkflowClient client) {
        this.client = client;
    }

    @Override
    public UploadPassportResult submit(String workflowId, String passportImageBase64, DeviceInfo deviceInfo) {
        // Snapshot attempt count BEFORE signaling so we can detect when THIS upload
        // is processed (workflow loops on FACIA decline → step does not advance,
        // only the counter does).
        ForeignOnboardingState before = client.getState(workflowId);
        int beforeAttempts = before.getPassportAttempts();

        client.stub(workflowId).passportSubmitted(new ForeignPassportSubmittedSignal(passportImageBase64, deviceInfo));

        ForeignOnboardingState state = client.awaitPassportAttempt(workflowId, beforeAttempts, 60, 1000);
        if (state.getCurrentStep() == ForeignOnboardingStep.FAILED) {
            return new UploadPassportResult(workflowId, ForeignOnboardingStep.FAILED,
                    state.getFaciaPassportReferenceId(), Map.of(),
                    "Passport verification failed", state.getFailureReason());
        }
        // FACIA declined but workflow stays alive — user can retry the same step.
        if (state.getFailureReason() != null
                && state.getCurrentStep() != ForeignOnboardingStep.PASSPORT_UPLOADED) {
            return new UploadPassportResult(workflowId, ForeignOnboardingStep.OTP_VERIFIED,
                    null, Map.of(),
                    "Passport verification declined", state.getFailureReason());
        }
        return new UploadPassportResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : ForeignOnboardingStep.PASSPORT_UPLOADED,
                state.getFaciaPassportReferenceId(),
                state.getExtractedData(),
                "Passport verified",
                null);
    }
}
