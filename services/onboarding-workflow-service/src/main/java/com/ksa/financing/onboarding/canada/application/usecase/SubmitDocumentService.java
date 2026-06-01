package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.DocSubmittedSignal;
import com.ksa.financing.onboarding.canada.domain.port.in.SubmitDocumentUseCase;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SubmitDocumentService implements SubmitDocumentUseCase {

    private final CanadaWorkflowClient client;

    public SubmitDocumentService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public SubmitDocumentResult submit(String workflowId, String documentImageBase64, DeviceInfo deviceInfo) {
        // Counter-based polling — workflow loops on FACIA decline so user can retry.
        CanadaOnboardingState before = client.getState(workflowId);
        int beforeAttempts = before.getDocumentAttempts();

        client.stub(workflowId).documentSubmitted(new DocSubmittedSignal(documentImageBase64, deviceInfo));
        CanadaOnboardingState state = client.awaitDocumentAttempt(workflowId, beforeAttempts, 60, 1000);
        if (state.getCurrentStep() == CanadaOnboardingStep.FAILED) {
            return new SubmitDocumentResult(workflowId, CanadaOnboardingStep.FAILED,
                    state.getFaciaDocumentReferenceId(), Map.of(),
                    "Document verification failed", state.getFailureReason());
        }
        if (state.getFailureReason() != null
                && state.getCurrentStep() != CanadaOnboardingStep.DOC_VERIFIED) {
            // FACIA declined this attempt but workflow stays alive — user can retry.
            return new SubmitDocumentResult(workflowId, CanadaOnboardingStep.DOC_SELECTED,
                    null, Map.of(),
                    "Document verification declined", state.getFailureReason());
        }
        return new SubmitDocumentResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.DOC_VERIFIED,
                state.getFaciaDocumentReferenceId(),
                state.getExtractedData(),
                "Document verified",
                null);
    }
}
