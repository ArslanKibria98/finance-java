package com.ksa.financing.kycadapter.infrastructure.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stub adapter simulating PEP (Politically Exposed Person) screening.
 *
 * Test triggers based on NID suffix:
 * - NID ending "888": PEP detected, confidence 0.85 → EDD_REQUIRED
 * - NID ending "777": High confidence match, confidence 0.96 → BLOCK
 * - NID ending "666": Low confidence match, confidence 0.45 → FLAG
 * - All others: CLEAR, no PEP detected
 */
@Component
public class PepScreeningStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(PepScreeningStubAdapter.class);

    private static final List<String> SCREENED_DATABASES = List.of(
            "GLOBAL_PEP", "LOCAL_PEP", "UN_SANCTIONS", "OFAC", "SAMA_LIST"
    );

    public Map<String, Object> screenPep(String fullName, String nationalId,
                                          String nationality, String dateOfBirth) {
        log.info("PEP stub: Screening individual name={}, nationalId={}, nationality={}",
                maskName(fullName), maskId(nationalId), nationality);

        simulateDelay(150);

        Map<String, Object> result = new LinkedHashMap<>();

        if (nationalId != null && nationalId.endsWith("888")) {
            // PEP detected → EDD required
            result.put("decision", "EDD_REQUIRED");
            result.put("pepDetected", true);
            result.put("confidenceScore", 0.85);
            result.put("matchCount", 1);
            result.put("matchDetails", List.of(Map.of(
                    "database", "GLOBAL_PEP",
                    "matchScore", 0.85,
                    "matchedName", fullName,
                    "pepCategory", "FAMILY_MEMBER",
                    "listingDate", "2023-06-01"
            )));
            log.warn("PEP stub: PEP detected for nationalId={} → EDD_REQUIRED", maskId(nationalId));

        } else if (nationalId != null && nationalId.endsWith("777")) {
            // High confidence sanctions match → BLOCK
            result.put("decision", "BLOCK");
            result.put("pepDetected", false);
            result.put("confidenceScore", 0.96);
            result.put("matchCount", 1);
            result.put("matchDetails", List.of(Map.of(
                    "database", "OFAC",
                    "matchScore", 0.96,
                    "matchedName", fullName,
                    "listingDate", "2024-03-10"
            )));
            log.warn("PEP stub: BLOCK for nationalId={} (confidence=0.96)", maskId(nationalId));

        } else if (nationalId != null && nationalId.endsWith("666")) {
            // Low confidence match → FLAG
            result.put("decision", "FLAG");
            result.put("pepDetected", false);
            result.put("confidenceScore", 0.45);
            result.put("matchCount", 1);
            result.put("matchDetails", List.of(Map.of(
                    "database", "UN_SANCTIONS",
                    "matchScore", 0.45,
                    "matchedName", fullName + " (partial)",
                    "listingDate", "2022-11-20"
            )));
            log.info("PEP stub: FLAG for nationalId={} (confidence=0.45)", maskId(nationalId));

        } else {
            // CLEAR
            result.put("decision", "CLEAR");
            result.put("pepDetected", false);
            result.put("confidenceScore", 0.0);
            result.put("matchCount", 0);
            result.put("matchDetails", List.of());
            log.info("PEP stub: CLEAR result for nationalId={}", maskId(nationalId));
        }

        result.put("screenedAt", Instant.now().toString());
        result.put("databases", SCREENED_DATABASES);
        return result;
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("PEP stub: Delay interrupted");
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
