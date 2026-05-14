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
        String idempotencyKey,
        String customerId,
        String applicationId,
        String contextType
    ) {
        public InitiateNafathCommand(UUID tenantId, String nationalId, String idempotencyKey) {
            this(tenantId, nationalId, idempotencyKey, null, null, null);
        }
    }

    record NafathInitiationResult(
        VerificationSession session,
        int randomNumber,
        String transactionId,
        Map<String, Object> nafathVerificationData
    ) {}
}
