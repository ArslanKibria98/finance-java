package com.ksa.financing.kycadapter.domain.port.in;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import java.util.UUID;

public interface ScreenSanctionsUseCase {
    SanctionsScreeningResult screen(ScreenSanctionsCommand command);

    record ScreenSanctionsCommand(
        UUID tenantId,
        String fullName,
        String nationalId,
        String nationality,
        String idempotencyKey
    ) {}

    record SanctionsScreeningResult(
        VerificationSession session,
        String screeningStatus,
        boolean hit
    ) {}
}
