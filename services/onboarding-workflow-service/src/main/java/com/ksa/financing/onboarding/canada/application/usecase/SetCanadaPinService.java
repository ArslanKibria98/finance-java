package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.CanadaSetPinSignal;
import com.ksa.financing.onboarding.canada.domain.port.in.SetCanadaPinUseCase;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import org.springframework.stereotype.Service;

@Service
public class SetCanadaPinService implements SetCanadaPinUseCase {

    private final CanadaWorkflowClient client;

    public SetCanadaPinService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public SetCanadaPinResult setPin(String workflowId, String pin, String confirmPin,
                                     DeviceInfo deviceInfo) {
        if (pin == null || !pin.equals(confirmPin)) {
            return new SetCanadaPinResult(workflowId, CanadaOnboardingStep.SELFIE_VERIFIED,
                    null, "PIN and confirmPin do not match");
        }
        client.stub(workflowId).pinSubmitted(new CanadaSetPinSignal(pin, confirmPin, deviceInfo));
        CanadaOnboardingState state = client.awaitStep(workflowId, CanadaOnboardingStep.PIN_SETUP, 20, 250);
        return new SetCanadaPinResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.PIN_SETUP,
                "PIN set successfully",
                state.getFailureReason());
    }
}
