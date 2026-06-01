package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

public interface VerifyForeignDualOtpUseCase {

    VerifyForeignOtpResult verify(String workflowId, String mobileOtp, String emailOtp,
                                  DeviceInfo deviceInfo);

    record VerifyForeignOtpResult(
            String workflowId,
            ForeignOnboardingStep currentStep,
            String keycloakUserId,
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType,
            String message,
            String failureReason
    ) {}
}
