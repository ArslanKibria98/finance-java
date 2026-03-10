package com.ksa.financing.onboarding.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface AcceptTermsUseCase {
    AcceptTermsResult accept(String workflowId, boolean accepted, DeviceInfo deviceInfo);

    record AcceptTermsResult(
        String workflowId,
        String status,
        String message
    ) {}
}
