package com.ksa.financing.onboarding.foreign.domain.port.in;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignDataConfirmedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

public interface ConfirmForeignDataUseCase {

    ConfirmForeignDataResult confirm(String workflowId, ForeignDataConfirmedSignal signal);

    record ConfirmForeignDataResult(
            String workflowId,
            ForeignOnboardingStep currentStep,
            String message
    ) {}
}
