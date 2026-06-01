package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface SetCanadaPinUseCase {

    SetCanadaPinResult setPin(String workflowId, String pin, String confirmPin,
                              DeviceInfo deviceInfo);

    record SetCanadaPinResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            String message,
            String failureReason
    ) {}
}
