package com.ksa.financing.onboarding.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface InitiateNafathUseCase {
    InitiateNafathResult initiate(String workflowId, DeviceInfo deviceInfo);

    record InitiateNafathResult(
        String workflowId,
        String status,
        int nafathRandomNumber,
        String nafathSessionId,
        String transactionId
    ) {}
}
