package com.ksa.financing.kycadapter.infrastructure.stub;

import com.ksa.financing.kycadapter.infrastructure.middleware.MiddlewareApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PEP / sanctions screening adapter.
 *
 * Routes through middleware-third-party (SAFEWATCH_SCAN_SESSION) so each
 * screening is logged in client_request_test.
 *
 * NID-suffix test triggers are preserved locally so onboarding integration
 * tests continue to get deterministic outcomes regardless of upstream
 * mock variability:
 *   - NID ending "888": PEP detected → EDD_REQUIRED
 *   - NID ending "777": Sanctions match → BLOCK
 *   - NID ending "666": Low confidence → FLAG
 *   - All others: CLEAR
 */
@Component
public class PepScreeningStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(PepScreeningStubAdapter.class);

    private static final String API_CODE = "SAFEWATCH_SCAN_SESSION";

    private static final List<String> SCREENED_DATABASES = List.of(
            "GLOBAL_PEP", "LOCAL_PEP", "UN_SANCTIONS", "OFAC", "SAMA_LIST"
    );

    private final MiddlewareApiClient middlewareApiClient;

    public PepScreeningStubAdapter(MiddlewareApiClient middlewareApiClient) {
        this.middlewareApiClient = middlewareApiClient;
    }

    public Map<String, Object> screenPep(String fullName, String nationalId,
                                          String nationality, String dateOfBirth) {
        log.info("PEP via middleware: screening name={}, nationalId={}, nationality={}",
                maskName(fullName), maskId(nationalId), nationality);

        var requestBody = Map.<String, Object>of(
                "fullName", fullName != null ? fullName : "",
                "nationalId", nationalId != null ? nationalId : "",
                "nationality", nationality != null ? nationality : "",
                "dateOfBirth", dateOfBirth != null ? dateOfBirth : ""
        );

        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_CODE, requestBody, nationalId, null, null);

        Map<String, Object> result = new LinkedHashMap<>(upstream);
        applyLocalDecision(result, fullName, nationalId);
        result.put("screenedAt", Instant.now().toString());
        result.put("databases", SCREENED_DATABASES);
        return result;
    }

    private void applyLocalDecision(Map<String, Object> result, String fullName, String nationalId) {
        if (nationalId != null && nationalId.endsWith("888")) {
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
        } else if (nationalId != null && nationalId.endsWith("777")) {
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
        } else if (nationalId != null && nationalId.endsWith("666")) {
            result.put("decision", "FLAG");
            result.put("pepDetected", false);
            result.put("confidenceScore", 0.45);
            result.put("matchCount", 1);
            result.put("matchDetails", List.of(Map.of(
                    "database", "UN_SANCTIONS",
                    "matchScore", 0.45,
                    "matchedName", (fullName != null ? fullName : "") + " (partial)",
                    "listingDate", "2022-11-20"
            )));
        } else {
            result.put("decision", "CLEAR");
            result.put("pepDetected", false);
            result.put("confidenceScore", 0.0);
            result.put("matchCount", 0);
            result.put("matchDetails", List.of());
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
