package com.ksa.financing.onboarding.domain.port.in;

import com.ksa.financing.onboarding.domain.model.OnboardingRequest;
import com.ksa.financing.onboarding.domain.model.OnboardingStep;

public interface StartOnboardingUseCase {
    StartOnboardingResult start(OnboardingRequest request);

    record StartOnboardingResult(
        String workflowId,
        String status,
        OnboardingStep currentStep,
        String otpRequestId,
        String maskedMobile,
        String globalUid,
        String customerId,
        String failureReason
    ) {}
}
