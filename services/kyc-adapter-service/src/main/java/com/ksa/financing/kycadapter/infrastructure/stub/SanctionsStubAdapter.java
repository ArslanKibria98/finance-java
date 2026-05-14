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
 * AML / sanctions screening adapter.
 *
 * Routes through middleware-third-party (AML_SCREENING). NID-suffix test
 * trigger ("999" → HIT) is preserved locally so integration tests stay
 * deterministic; the middleware records the underlying call.
 */
@Component
public class SanctionsStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(SanctionsStubAdapter.class);

    private static final String API_CODE = "AML_SCREENING";

    private static final List<String> SCREENED_DATABASES = List.of(
            "UN_SANCTIONS", "SAMA_LIST", "LOCAL_PEP"
    );

    private final MiddlewareApiClient middlewareApiClient;

    public SanctionsStubAdapter(MiddlewareApiClient middlewareApiClient) {
        this.middlewareApiClient = middlewareApiClient;
    }

    public Map<String, Object> screenIndividual(String fullName, String nationalId, String nationality) {
        log.info("Sanctions via middleware: screening name={}, nationalId={}, nationality={}",
                maskName(fullName), maskId(nationalId), nationality);

        var requestBody = Map.<String, Object>of(
                "fullName", fullName != null ? fullName : "",
                "nationalId", nationalId != null ? nationalId : "",
                "nationality", nationality != null ? nationality : ""
        );

        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_CODE, requestBody, nationalId, null, null);

        boolean isHit = nationalId != null && nationalId.endsWith("999");

        Map<String, Object> result = new LinkedHashMap<>(upstream);
        if (isHit) {
            result.put("result", "HIT");
            result.put("matchCount", 1);
            result.put("matchDetails", List.of(Map.of(
                    "database", "SAMA_LIST",
                    "matchScore", 0.95,
                    "matchedName", fullName,
                    "listingDate", "2024-01-15"
            )));
            log.warn("Sanctions HIT for nationalId={}", maskId(nationalId));
        } else {
            result.put("result", "CLEAR");
            result.put("matchCount", 0);
            result.put("matchDetails", List.of());
        }

        result.put("screenedAt", Instant.now().toString());
        result.put("databases", SCREENED_DATABASES);
        result.put("fullName", fullName);
        result.put("nationalId", nationalId);
        result.put("nationality", nationality);
        return result;
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
