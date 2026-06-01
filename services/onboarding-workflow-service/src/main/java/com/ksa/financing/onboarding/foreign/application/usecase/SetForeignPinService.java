package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignPinSignal;
import com.ksa.financing.onboarding.foreign.domain.port.in.SetForeignPinUseCase;
import org.springframework.stereotype.Service;

@Service
public class SetForeignPinService implements SetForeignPinUseCase {

    private final ForeignWorkflowClient client;

    public SetForeignPinService(ForeignWorkflowClient client) {
        this.client = client;
    }

    @Override
    public SetForeignPinResult setPin(String workflowId, String pin, String confirmPin,
                                      DeviceInfo deviceInfo) {
        if (pin == null || !pin.equals(confirmPin)) {
            return new SetForeignPinResult(workflowId, ForeignOnboardingStep.SELFIE_VERIFIED,
                    null, "PIN and confirmPin do not match");
        }
        client.stub(workflowId).pinSubmitted(new ForeignPinSignal(pin, confirmPin, deviceInfo));
        ForeignOnboardingState state = client.awaitStep(workflowId, ForeignOnboardingStep.PIN_SETUP, 20, 250);
        return new SetForeignPinResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : ForeignOnboardingStep.PIN_SETUP,
                "PIN set successfully",
                state.getFailureReason());
    }
}
