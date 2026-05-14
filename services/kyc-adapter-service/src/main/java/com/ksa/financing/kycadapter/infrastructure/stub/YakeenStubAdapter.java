package com.ksa.financing.kycadapter.infrastructure.stub;

import com.ksa.financing.kycadapter.infrastructure.middleware.MiddlewareApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Yakeen / Absher identity verification adapter.
 *
 * Routes through middleware-third-party (ABSHER_VERIFY_IDENTITY). The
 * demographic envelope expected by downstream callers is preserved by
 * back-filling well-known keys from whatever the upstream payload returns.
 */
@Component
public class YakeenStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(YakeenStubAdapter.class);

    private static final String API_CODE = "ABSHER_VERIFY_IDENTITY";

    private final MiddlewareApiClient middlewareApiClient;

    public YakeenStubAdapter(MiddlewareApiClient middlewareApiClient) {
        this.middlewareApiClient = middlewareApiClient;
    }

    public Map<String, Object> verifyIdentity(String nationalId, LocalDate dateOfBirth) {
        log.info("Yakeen via middleware: verifying nationalId={}, dob={}",
                maskId(nationalId), dateOfBirth);

        var requestBody = Map.<String, Object>of(
                "nationalId", nationalId,
                "dateOfBirth", dateOfBirth != null ? dateOfBirth.toString() : ""
        );

        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_CODE, requestBody, nationalId, null, null);

        Map<String, Object> demographics = new LinkedHashMap<>(upstream);
        demographics.putIfAbsent("nationalId", nationalId);
        demographics.putIfAbsent("dateOfBirth", dateOfBirth != null ? dateOfBirth.toString() : null);
        demographics.putIfAbsent("verified", true);
        demographics.putIfAbsent("fullNameAr", upstream.get("fullName"));
        demographics.putIfAbsent("fullNameEn", upstream.get("fullNameEn"));
        demographics.putIfAbsent("gender", upstream.get("gender"));
        demographics.putIfAbsent("nationality", upstream.getOrDefault("nationality", "SAU"));
        demographics.putIfAbsent("addressCity", upstream.get("city"));
        demographics.putIfAbsent("addressRegion", upstream.get("region"));
        demographics.putIfAbsent("idExpiryDate",
                upstream.getOrDefault("idExpiryDate", LocalDate.now().plusYears(2).toString()));

        log.info("Yakeen result: nationalId={}, verified={}",
                maskId(nationalId), demographics.get("verified"));
        return demographics;
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }
}
