package com.ksa.financing.customer.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PepAnswerResponse(
        UUID customerId,
        boolean pepFlag,
        String pepStatus,
        String message,
        String timestamp
) {
    public static PepAnswerResponse of(UUID customerId, boolean pepFlag, String pepStatus) {
        return new PepAnswerResponse(
                customerId,
                pepFlag,
                pepStatus,
                "PEP answers submitted successfully",
                Instant.now().toString()
        );
    }
}
