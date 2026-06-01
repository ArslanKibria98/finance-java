package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.port.in.GetCanadaOnboardingStatusUseCase;
import org.springframework.stereotype.Service;

@Service
public class GetCanadaOnboardingStatusService implements GetCanadaOnboardingStatusUseCase {

    private final CanadaWorkflowClient client;

    public GetCanadaOnboardingStatusService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public CanadaOnboardingState getStatus(String workflowId) {
        CanadaOnboardingState state = client.getState(workflowId);
        if (state.getCurrentStep() == null) {
            state.setCurrentStep(CanadaOnboardingStep.INITIATED);
        }
        if (state.getWorkflowId() == null) {
            state.setWorkflowId(workflowId);
        }
        return state;
    }

    @Override
    public CanadaOnboardingState getStatusByEmail(String email) {
        return getStatus(client.workflowIdFor(email));
    }
}
