package com.ksa.financing.onboarding.canada.application.usecase;

import com.ksa.financing.onboarding.canada.domain.model.BiometricsSignal;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.port.in.EnableBiometricsUseCase;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import org.springframework.stereotype.Service;

@Service
public class EnableBiometricsService implements EnableBiometricsUseCase {

    private final CanadaWorkflowClient client;

    public EnableBiometricsService(CanadaWorkflowClient client) {
        this.client = client;
    }

    @Override
    public EnableBiometricsResult submit(String workflowId, boolean enabled, boolean skipped,
                                         DeviceInfo deviceInfo) {
        client.stub(workflowId).biometricsSubmitted(new BiometricsSignal(enabled, skipped, deviceInfo));
        CanadaOnboardingState state = client.awaitStep(workflowId, CanadaOnboardingStep.COMPLETED, 30, 300);
        return new EnableBiometricsResult(workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : CanadaOnboardingStep.BIOMETRICS_SETUP,
                state.isBiometricsEnabled(),
                state.isBiometricsSkipped(),
                skipped ? "Biometrics skipped — onboarding complete"
                        : "Biometrics " + (enabled ? "enabled" : "disabled") + " — onboarding complete");
    }
}
