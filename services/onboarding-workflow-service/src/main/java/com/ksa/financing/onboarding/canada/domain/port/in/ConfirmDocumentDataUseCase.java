package com.ksa.financing.onboarding.canada.domain.port.in;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.DocConfirmedSignal;

public interface ConfirmDocumentDataUseCase {

    ConfirmDocumentDataResult confirm(String workflowId, DocConfirmedSignal signal);

    record ConfirmDocumentDataResult(
            String workflowId,
            CanadaOnboardingStep currentStep,
            String message
    ) {}
}
