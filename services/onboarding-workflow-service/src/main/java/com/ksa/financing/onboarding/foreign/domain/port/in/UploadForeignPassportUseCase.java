package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

import java.util.Map;

public interface UploadForeignPassportUseCase {

    UploadPassportResult submit(String workflowId, String passportImageBase64, DeviceInfo deviceInfo);

    record UploadPassportResult(
            String workflowId,
            ForeignOnboardingStep currentStep,
            String faciaReferenceId,
            Map<String, Object> extractedData,
            String message,
            String failureReason
    ) {}
}
