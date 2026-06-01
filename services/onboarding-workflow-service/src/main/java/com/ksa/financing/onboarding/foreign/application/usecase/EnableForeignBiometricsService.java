package com.ksa.financing.onboarding.foreign.application.usecase;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignBiometricsSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.port.in.EnableForeignBiometricsUseCase;
import org.springframework.stereotype.Service;

@Service
public class EnableForeignBiometricsService implements EnableForeignBiometricsUseCase {

    private final ForeignWorkflowClient client;

    public EnableForeignBiometricsService(ForeignWorkflowClient client) {
        this.client = client;
    }

    @Override
    public EnableForeignBiometricsResult submit(String workflowId, boolean enabled, boolean skipped,
                                                DeviceInfo deviceInfo) {
        client.stub(workflowId).biometricsSubmitted(new ForeignBiometricsSignal(enabled, skipped, deviceInfo));
        ForeignOnboardingState state = client.awaitStep(workflowId, ForeignOnboardingStep.COMPLETED, 30, 300);
        return new EnableForeignBiometricsResult(
                workflowId,
                state.getCurrentStep() != null ? state.getCurrentStep() : ForeignOnboardingStep.BIOMETRICS_SETUP,
                state.isBiometricsEnabled(),
                state.isBiometricsSkipped(),
                state.isOnboardingComplete(),
                skipped ? "Biometrics skipped — onboarding complete"
                        : "Biometrics " + (enabled ? "enabled" : "disabled") + " — onboarding complete");
    }
}
