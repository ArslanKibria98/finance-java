package com.ksa.financing.onboarding.shared.facia;

import java.io.Serializable;

public record FaciaFaceMatchResult(
        boolean match,
        String referenceId,
        Double similarityScore,
        String similarityStatus,
        String failureReason
) implements Serializable {
}
