package com.ksa.financing.onboarding.shared.facia;

import java.io.Serializable;
import java.util.Map;

public record FaciaDocumentResult(
        boolean accepted,
        String referenceId,
        String event,
        String declineReason,
        Map<String, Object> extractedData
) implements Serializable {
}
