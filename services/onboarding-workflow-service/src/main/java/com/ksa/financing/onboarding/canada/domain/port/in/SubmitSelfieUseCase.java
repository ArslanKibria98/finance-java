package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface SubmitSelfieUseCase {

    SubmitSelfieResult submit(String workflowId, String selfieImageBase64, DeviceInfo deviceInfo);

    record SubmitSelfieResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            String faciaReferenceId,
            Double faceMatchScore,
            String customerId,
            String walletId,
            String message,
            String failureReason
    ) {}
}
