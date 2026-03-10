package com.ksa.financing.kycadapter.domain.port.in;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import java.util.UUID;

public interface VerifyMobileUseCase {
    VerificationSession verify(VerifyMobileCommand command);

    record VerifyMobileCommand(
        UUID tenantId,
        String mobileNumber,
        String nationalId,
        String idempotencyKey
    ) {}
}
