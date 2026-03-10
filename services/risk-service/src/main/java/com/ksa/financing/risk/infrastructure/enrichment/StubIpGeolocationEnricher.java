package com.ksa.financing.risk.infrastructure.enrichment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub IP geolocation enricher — returns default values.
 * Replace with real IP geolocation provider (MaxMind, ip-api, etc.) in production.
 */
@Component
@Slf4j
public class StubIpGeolocationEnricher {

    public String resolveCountry(String ipAddress) {
        log.debug("Stub IP geolocation: resolving country for IP={}", ipAddress);
        return "SA"; // default: Saudi Arabia
    }

    public String resolveCity(String ipAddress) {
        log.debug("Stub IP geolocation: resolving city for IP={}", ipAddress);
        return "Riyadh"; // default
    }
}
