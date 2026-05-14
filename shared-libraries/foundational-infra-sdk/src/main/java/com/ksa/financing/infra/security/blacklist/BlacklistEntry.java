package com.ksa.financing.infra.security.blacklist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Blacklist record stored in Redis. Value is hashed (SHA-256) — no PII in keys.
 * The original value is encrypted server-side and not exposed via this DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BlacklistEntry(
        BlacklistType type,
        String valueHash,
        String reason,
        Instant blockedAt,
        Instant expiresAt
) {

    @JsonIgnore
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
