package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.port.in.GetForeignStatusUseCase;
import org.springframework.stereotype.Service;

@Service
public class GetForeignStatusService implements GetForeignStatusUseCase {

    private final ForeignWorkflowClient client;

    public GetForeignStatusService(ForeignWorkflowClient client) {
        this.client = client;
    }

    @Override
    public ForeignOnboardingState getStatus(String workflowId) {
        ForeignOnboardingState s = client.getState(workflowId);
        if (s.getCurrentStep() == null) s.setCurrentStep(ForeignOnboardingStep.INITIATED);
        if (s.getWorkflowId() == null) s.setWorkflowId(workflowId);
        return s;
    }

    @Override
    public ForeignOnboardingState getStatusByEmail(String email) {
        return getStatus(client.workflowIdFor(email));
    }
}
