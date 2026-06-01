package com.ksa.financing.onboarding.guest.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;

public interface EnableGuestBiometricsUseCase {

    EnableGuestBiometricsResult submit(String workflowId, boolean enabled, boolean skipped,
                                       DeviceInfo deviceInfo);

    record EnableGuestBiometricsResult(
            String workflowId,
            GuestOnboardingStep currentStep,
            boolean biometricsEnabled,
            boolean skipped,
            String message
    ) {}
}
