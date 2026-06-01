package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.DocumentType;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface SelectDocumentUseCase {

    SelectDocumentResult select(String workflowId, DocumentType documentType, DeviceInfo deviceInfo);

    record SelectDocumentResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            DocumentType selectedType,
            String message
    ) {}
}
