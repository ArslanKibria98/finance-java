package com.ksa.financing.onboarding.guest.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.domain.model.GuestPinSignal;
import com.ksa.financing.onboarding.guest.domain.port.in.SetGuestPinUseCase;
import org.springframework.stereotype.Service;

@Service
public class SetGuestPinService implements SetGuestPinUseCase {

    private final GuestWorkflowClient client;

    public SetGuestPinService(GuestWorkflowClient client) {
        this.client = client;
    }

    @Override
    public SetGuestPinResult setPin(String workflowId, String pin, String confirmPin,
                                    DeviceInfo deviceInfo) {
        if (pin == null || !pin.equals(confirmPin)) {
            return new SetGuestPinResult(workflowId, GuestOnboardingStep.BIOMETRICS_SETUP,
                    false, null, "PIN and confirmPin do not match");
        }
        client.stub(workflowId).pinSubmitted(new GuestPinSignal(pin, confirmPin, deviceInfo));
        GuestOnboardingState state = client.awaitStep(workflowId, GuestOnboardingStep.COMPLETED, 30, 300);
        return new SetGuestPinResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : GuestOnboardingStep.PIN_SETUP,
                state.isOnboardingComplete(),
                "PIN set — guest onboarding complete (full KYC still pending)",
                state.getFailureReason());
    }
}
