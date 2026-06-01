package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.util.Map;

public interface SubmitDocumentUseCase {

    SubmitDocumentResult submit(String workflowId, String documentImageBase64, DeviceInfo deviceInfo);

    record SubmitDocumentResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            String faciaReferenceId,
            Map<String, Object> extractedData,
            String message,
            String failureReason
    ) {}
}
