package com.ksa.financing.onboarding.guest.domain.port.in;

import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingRequest;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;

public interface StartGuestOnboardingUseCase {

    StartGuestResult start(GuestOnboardingRequest request);

    record StartGuestResult(
            String workflowId,
            GuestOnboardingStep currentStep,
            String mobileOtpRequestId,
            String emailOtpRequestId,
            String maskedMobile,
            String maskedEmail,
            String status,
            String failureReason
    ) {}
}
