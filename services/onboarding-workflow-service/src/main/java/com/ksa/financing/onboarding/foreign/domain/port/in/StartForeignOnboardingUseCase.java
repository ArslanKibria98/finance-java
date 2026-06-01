package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingRequest;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

public interface StartForeignOnboardingUseCase {

    StartForeignResult start(ForeignOnboardingRequest request);

    record StartForeignResult(
            String workflowId,
            ForeignOnboardingStep currentStep,
            String mobileOtpRequestId,
            String emailOtpRequestId,
            String maskedMobile,
            String maskedEmail,
            String status,
            String failureReason
    ) {}
}
