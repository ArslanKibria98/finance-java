package com.ksa.financing.kycadapter.domain.port.in;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface VerifyIdentityUseCase {
    VerifyIdentityResult verify(VerifyIdentityCommand command);

    record VerifyIdentityCommand(
        UUID tenantId,
        String nationalId,
        LocalDate dateOfBirth,
        String idempotencyKey
    ) {}

    record VerifyIdentityResult(
        VerificationSession session,
        Map<String, Object> demographics
    ) {}
}
