package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

public interface EnableForeignBiometricsUseCase {

    EnableForeignBiometricsResult submit(String workflowId, boolean enabled, boolean skipped,
                                         DeviceInfo deviceInfo);

    record EnableForeignBiometricsResult(
            String workflowId,
            ForeignOnboardingStep currentStep,
            boolean biometricsEnabled,
            boolean skipped,
            boolean onboardingComplete,
            String message
    ) {}
}
