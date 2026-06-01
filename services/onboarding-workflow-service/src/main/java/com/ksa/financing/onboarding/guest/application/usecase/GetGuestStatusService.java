package com.ksa.financing.onboarding.guest.application.usecase;

import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.domain.port.in.GetGuestStatusUseCase;
import org.springframework.stereotype.Service;

@Service
public class GetGuestStatusService implements GetGuestStatusUseCase {

    private final GuestWorkflowClient client;

    public GetGuestStatusService(GuestWorkflowClient client) {
        this.client = client;
    }

    @Override
    public GuestOnboardingState getStatus(String workflowId) {
        GuestOnboardingState s = client.getState(workflowId);
        if (s.getCurrentStep() == null) s.setCurrentStep(GuestOnboardingStep.INITIATED);
        if (s.getWorkflowId() == null) s.setWorkflowId(workflowId);
        return s;
    }

    @Override
    public GuestOnboardingState getStatusByEmail(String email) {
        return getStatus(client.workflowIdFor(email));
    }
}
