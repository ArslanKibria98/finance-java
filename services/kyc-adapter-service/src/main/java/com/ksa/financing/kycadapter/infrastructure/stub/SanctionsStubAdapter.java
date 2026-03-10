package com.ksa.financing.kycadapter.infrastructure.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stub adapter simulating AML/sanctions screening against multiple databases.
 * Checks against UN Sanctions, SAMA (Saudi Central Bank) list, and local PEP
 * (Politically Exposed Persons) databases.
 *
 * For testing purposes, if the nationalId ends with "999", the adapter returns
 * a HIT result to simulate a positive match scenario.
 */
@Component
public class SanctionsStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(SanctionsStubAdapter.class);

    private static final List<String> SCREENED_DATABASES = List.of(
            "UN_SANCTIONS", "SAMA_LIST", "LOCAL_PEP"
    );

    /**
     * Screen an individual against sanctions and PEP databases.
     * Simulates a 100ms delay.
     *
     * @param fullName    the full name of the individual
     * @param nationalId  the national ID of the individual
     * @param nationality the nationality code (ISO 3166-1 alpha-3)
     * @return a map containing screening results
     */
    public Map<String, Object> screenIndividual(String fullName, String nationalId, String nationality) {
        log.info("Sanctions stub: Screening individual name={}, nationalId={}, nationality={}",
                maskName(fullName), maskId(nationalId), nationality);

        simulateDelay(100);

        boolean isHit = nationalId != null && nationalId.endsWith("999");

        Map<String, Object> result = new LinkedHashMap<>();
        if (isHit) {
            result.put("result", "HIT");
            result.put("matchCount", 1);
            result.put("matchDetails", List.of(Map.of(
                    "database", "SAMA_LIST",
                    "matchScore", 0.95,
                    "matchedName", fullName,
                    "listingDate", "2024-01-15"
            )));
            log.warn("Sanctions stub: HIT detected for nationalId={}", maskId(nationalId));
        } else {
            result.put("result", "CLEAR");
            result.put("matchCount", 0);
            result.put("matchDetails", List.of());
            log.info("Sanctions stub: CLEAR result for nationalId={}", maskId(nationalId));
        }

        result.put("screenedAt", Instant.now().toString());
        result.put("databases", SCREENED_DATABASES);
        result.put("fullName", fullName);
        result.put("nationalId", nationalId);
        result.put("nationality", nationality);

        return result;
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sanctions stub: Delay interrupted");
        }
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }

    private String maskName(String name) {
        if (name == null || name.length() < 3) return "***";
        return name.substring(0, 3) + "***";
    }
}
