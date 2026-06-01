package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.DocSelectedSignal;
import com.ksa.financing.onboarding.canada.domain.model.DocumentType;
import com.ksa.financing.onboarding.canada.domain.port.in.SelectDocumentUseCase;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import org.springframework.stereotype.Service;

@Service
public class SelectDocumentService implements SelectDocumentUseCase {

    private final CanadaWorkflowClient client;

    public SelectDocumentService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public SelectDocumentResult select(String workflowId, DocumentType documentType, DeviceInfo deviceInfo) {
        client.stub(workflowId).documentSelected(new DocSelectedSignal(documentType, deviceInfo));
        CanadaOnboardingState state = client.awaitStep(workflowId, CanadaOnboardingStep.DOC_SELECTED, 20, 250);
        return new SelectDocumentResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.DOC_SELECTED,
                state.getDocumentType(),
                "Document type recorded: " + documentType);
    }
}
