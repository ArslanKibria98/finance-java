package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.DocConfirmedSignal;
import com.ksa.financing.onboarding.canada.domain.port.in.ConfirmDocumentDataUseCase;
import org.springframework.stereotype.Service;

@Service
public class ConfirmDocumentDataService implements ConfirmDocumentDataUseCase {

    private final CanadaWorkflowClient client;

    public ConfirmDocumentDataService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public ConfirmDocumentDataResult confirm(String workflowId, DocConfirmedSignal signal) {
        client.stub(workflowId).documentConfirmed(signal);
        CanadaOnboardingState state = client.awaitStep(workflowId, CanadaOnboardingStep.DOC_CONFIRMED, 20, 250);
        return new ConfirmDocumentDataResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.DOC_CONFIRMED,
                "Document data confirmed");
    }
}
