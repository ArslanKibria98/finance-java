package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface EnableBiometricsUseCase {

    EnableBiometricsResult submit(String workflowId, boolean enabled, boolean skipped,
                                  DeviceInfo deviceInfo);

    record EnableBiometricsResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            boolean biometricsEnabled,
            boolean skipped,
            String message
    ) {}
}
