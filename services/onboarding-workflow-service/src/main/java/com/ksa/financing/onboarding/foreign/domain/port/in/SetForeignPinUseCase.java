package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

public interface SetForeignPinUseCase {

    SetForeignPinResult setPin(String workflowId, String pin, String confirmPin,
                               DeviceInfo deviceInfo);

    record SetForeignPinResult(
            String workflowId,
            ForeignOnboardingStep currentStep,
            String message,
            String failureReason
    ) {}
}
