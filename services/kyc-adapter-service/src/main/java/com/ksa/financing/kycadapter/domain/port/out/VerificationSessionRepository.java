package com.ksa.financing.kycadapter.domain.port.out;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import java.util.Optional;
import java.util.UUID;

public interface VerificationSessionRepository {
    VerificationSession save(VerificationSession session);
    Optional<VerificationSession> findById(UUID id);
    Optional<VerificationSession> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    Optional<VerificationSession> findBySessionNumber(UUID tenantId, String sessionNumber);
}
