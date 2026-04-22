package com.ksa.financing.ledger.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoaConfigurationMapping(
        UUID id,
        UUID tenantId,
        UUID profileId,
        UUID coaFieldId,
        UUID accountId,
        Boolean mandatoryOverride,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version
) {
    public static CoaConfigurationMapping create(
            UUID tenantId,
            UUID profileId,
            UUID coaFieldId,
            UUID accountId,
            Boolean mandatoryOverride,
            String notes
    ) {
        var now = LocalDateTime.now();
        return new CoaConfigurationMapping(
                UUID.randomUUID(),
                tenantId,
                profileId,
                coaFieldId,
                accountId,
                mandatoryOverride,
                notes,
                now,
                now,
                1
        );
    }

    public CoaConfigurationMapping withAccountId(UUID newAccountId) {
        return new CoaConfigurationMapping(
                id,
                tenantId,
                profileId,
                coaFieldId,
                newAccountId,
                mandatoryOverride,
                notes,
                createdAt,
                LocalDateTime.now(),
                version
        );
    }
}
