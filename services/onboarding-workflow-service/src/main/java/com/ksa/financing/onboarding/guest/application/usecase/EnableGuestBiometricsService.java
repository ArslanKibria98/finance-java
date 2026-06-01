package com.ksa.financing.onboarding.guest.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.guest.domain.model.GuestBiometricsSignal;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.domain.port.in.EnableGuestBiometricsUseCase;
import org.springframework.stereotype.Service;

@Service
public class EnableGuestBiometricsService implements EnableGuestBiometricsUseCase {

    private final GuestWorkflowClient client;

    public EnableGuestBiometricsService(GuestWorkflowClient client) {
        this.client = client;
    }

    @Override
    public EnableGuestBiometricsResult submit(String workflowId, boolean enabled, boolean skipped,
                                              DeviceInfo deviceInfo) {
        client.stub(workflowId).biometricsSubmitted(new GuestBiometricsSignal(enabled, skipped, deviceInfo));
        GuestOnboardingState state = client.awaitStep(workflowId, GuestOnboardingStep.BIOMETRICS_SETUP, 20, 250);
        return new EnableGuestBiometricsResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : GuestOnboardingStep.BIOMETRICS_SETUP,
                state.isBiometricsEnabled(),
                state.isBiometricsSkipped(),
                skipped ? "Biometrics skipped"
                        : "Biometrics " + (enabled ? "enabled" : "disabled"));
    }
}
