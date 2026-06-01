package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingRequest;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;

public interface StartCanadaOnboardingUseCase {

    StartCanadaOnboardingResult start(CanadaOnboardingRequest request);

    record StartCanadaOnboardingResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            String mobileOtpRequestId,
            String emailOtpRequestId,
            String maskedMobile,
            String maskedEmail,
            String status,
            String failureReason
    ) {}
}
