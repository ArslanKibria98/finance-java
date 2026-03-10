package com.ksa.financing.kycadapter.application.mapper;

import com.ksa.financing.kycadapter.application.dto.VerificationSessionResponse;
import com.ksa.financing.kycadapter.domain.model.VerificationSession;

public class KycMapper {

    private KycMapper() {}

    public static VerificationSessionResponse toResponse(VerificationSession s) {
        return new VerificationSessionResponse(
            s.getId(),
            s.getSessionNumber(),
            s.getVerificationType() != null ? s.getVerificationType().name() : null,
            s.getProvider() != null ? s.getProvider().name() : null,
            s.getStatus() != null ? s.getStatus().name() : null,
            s.getResult() != null ? s.getResult().name() : null,
            s.getInitiatedAt(),
            s.getCompletedAt()
        );
    }
}
