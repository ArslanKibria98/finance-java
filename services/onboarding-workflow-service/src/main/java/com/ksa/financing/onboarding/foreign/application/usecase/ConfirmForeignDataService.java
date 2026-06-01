package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignDataConfirmedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.port.in.ConfirmForeignDataUseCase;
import org.springframework.stereotype.Service;

@Service
public class ConfirmForeignDataService implements ConfirmForeignDataUseCase {

    private final ForeignWorkflowClient client;

    public ConfirmForeignDataService(ForeignWorkflowClient client) {
        this.client = client;
    }

    @Override
    public ConfirmForeignDataResult confirm(String workflowId, ForeignDataConfirmedSignal signal) {
        client.stub(workflowId).dataConfirmed(signal);
        ForeignOnboardingState state = client.awaitStep(workflowId, ForeignOnboardingStep.DATA_CONFIRMED, 20, 250);
        return new ConfirmForeignDataResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : ForeignOnboardingStep.DATA_CONFIRMED,
                "Passport data confirmed");
    }
}
