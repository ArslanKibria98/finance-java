package com.ksa.financing.kycadapter.domain.port.in;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import java.util.UUID;

public interface ScreenPepUseCase {
    PepScreeningResult screen(ScreenPepCommand command);

    record ScreenPepCommand(
        UUID tenantId,
        String fullName,
        String nationalId,
        String nationality,
        String dateOfBirth,
        String idempotencyKey
    ) {}

    record PepScreeningResult(
        VerificationSession session,
        String decision,        // CLEAR, FLAG, HOLD, BLOCK, EDD_REQUIRED
        boolean pepDetected,
        double confidenceScore,
        int matchCount,
        String matchDetails
    ) {}
}
