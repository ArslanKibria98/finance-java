package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

public interface UploadForeignSelfieUseCase {

    UploadSelfieResult submit(String workflowId, String selfieImageBase64, DeviceInfo deviceInfo);

    record UploadSelfieResult(
            String workflowId,
            ForeignOnboardingStep currentStep,
            String faciaReferenceId,
            Double faceMatchScore,
            String customerId,
            String walletId,
            String message,
            String failureReason
    ) {}
}
