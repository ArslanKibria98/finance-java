package com.ksa.financing.onboarding.guest.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;

public interface VerifyGuestDualOtpUseCase {

    VerifyGuestOtpResult verify(String workflowId, String mobileOtp, String emailOtp,
                                DeviceInfo deviceInfo);

    record VerifyGuestOtpResult(
            String workflowId,
            GuestOnboardingStep currentStep,
            String keycloakUserId,
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType,
            boolean onboardingComplete,
            String message,
            String failureReason
    ) {}
}
