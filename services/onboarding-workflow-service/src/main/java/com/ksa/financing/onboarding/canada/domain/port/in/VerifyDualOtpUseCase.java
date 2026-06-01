package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface VerifyDualOtpUseCase {

    VerifyDualOtpResult verify(String workflowId, String mobileOtp, String emailOtp,
                               DeviceInfo deviceInfo);

    record VerifyDualOtpResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            String keycloakUserId,
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType,
            String message,
            String failureReason
    ) {}
}
