package com.ksa.financing.onboarding.domain.port.in;

import com.ksa.financing.onboarding.domain.model.AdditionalInfoSignal;

public interface SubmitAdditionalInfoUseCase {
    SubmitAdditionalInfoResult submit(String workflowId, AdditionalInfoSignal signal);

    record SubmitAdditionalInfoResult(
        String workflowId,
        String status,
        String message
    ) {}
}
