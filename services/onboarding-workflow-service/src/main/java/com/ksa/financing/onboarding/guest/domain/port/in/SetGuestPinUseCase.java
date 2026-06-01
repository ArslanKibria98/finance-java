package com.ksa.financing.onboarding.guest.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;

public interface SetGuestPinUseCase {

    SetGuestPinResult setPin(String workflowId, String pin, String confirmPin,
                             DeviceInfo deviceInfo);

    record SetGuestPinResult(
            String workflowId,
            GuestOnboardingStep currentStep,
            boolean onboardingComplete,
            String message,
            String failureReason
    ) {}
}
