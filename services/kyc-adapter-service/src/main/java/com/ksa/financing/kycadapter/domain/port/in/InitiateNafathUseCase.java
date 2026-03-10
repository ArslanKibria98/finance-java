package com.ksa.financing.kycadapter.domain.port.in;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import java.util.Map;
import java.util.UUID;

public interface InitiateNafathUseCase {
    NafathInitiationResult initiate(InitiateNafathCommand command);
    VerificationSession checkStatus(UUID sessionId);

    record InitiateNafathCommand(
        UUID tenantId,
        String nationalId,
        String idempotencyKey
    ) {}

    record NafathInitiationResult(
        VerificationSession session,
        int randomNumber,
        String transactionId,
        Map<String, Object> nafathVerificationData
    ) {}
}
