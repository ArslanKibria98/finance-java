package com.ksa.financing.kycadapter.infrastructure.stub;

import com.ksa.financing.kycadapter.infrastructure.middleware.MiddlewareApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nafath identity verification adapter.
 *
 * All three methods now route through middleware-third-party so each
 * Nafath interaction (initiate / check-status / inquiry) lands in
 * client_request_{test|dev|prod}.
 *
 *   initiateSession()         -> NAFATH_INITIATE
 *   checkSessionStatus()      -> NAFATH_CHECK_STATUS
 *   getVerificationResult()   -> NAFATH_CHECK_STATUS (full citizen payload)
 *
 * The per-session "approve after 3 polls" simulation is preserved locally
 * so onboarding test flows still get a deterministic transition through
 * PENDING_USER_ACTION → COMPLETED while middleware logs each poll.
 */
@Component
public class NafathStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(NafathStubAdapter.class);

    private static final String API_INITIATE = "NAFATH_INITIATE";
    private static final String API_CHECK_STATUS = "NAFATH_CHECK_STATUS";

    private final ConcurrentHashMap<String, AtomicInteger> sessionCallCounts = new ConcurrentHashMap<>();
    private final MiddlewareApiClient middlewareApiClient;

    public NafathStubAdapter(MiddlewareApiClient middlewareApiClient) {
        this.middlewareApiClient = middlewareApiClient;
    }

    public Map<String, Object> initiateSession(String nationalId) {
        return initiateSession(nationalId, null, null, null);
    }

    public Map<String, Object> initiateSession(String nationalId, String customerId,
                                                String applicationId, String contextType) {
        log.info("Nafath via middleware: initiating session for nationalId={}", maskId(nationalId));

        var requestBody = Map.<String, Object>of(
                "nationalId", nationalId,
                "service", "OpenAccount"
        );

        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_INITIATE, requestBody, nationalId, null, null,
                customerId, applicationId, contextType);

        // Tahaquq/Nafath upstream returns {transId, random}; downstream expects
        // {sessionId, randomNumber, status, nationalId}.
        String sessionId = asString(upstream.get("transId"), upstream.get("sessionId"));
        Object random = upstream.getOrDefault("random", upstream.get("randomNumber"));

        sessionCallCounts.put(sessionId, new AtomicInteger(0));

        Map<String, Object> result = new LinkedHashMap<>(upstream);
        result.put("sessionId", sessionId);
        result.put("randomNumber", coerceToInteger(random));
        result.putIfAbsent("status", "PENDING_USER_ACTION");
        result.putIfAbsent("nationalId", nationalId);

        log.info("Nafath session initiated: sessionId={}", sessionId);
        return result;
    }

    public Map<String, Object> checkSessionStatus(String sessionId) {
        return checkSessionStatus(sessionId, null, null, null);
    }

    public Map<String, Object> checkSessionStatus(String sessionId, String customerId,
                                                   String applicationId, String contextType) {
        log.info("Nafath via middleware: checking status for sessionId={}", sessionId);

        var requestBody = Map.<String, Object>of("transId", sessionId);
        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_CHECK_STATUS, requestBody, null, null, sessionId,
                customerId, applicationId, contextType);

        AtomicInteger counter = sessionCallCounts.computeIfAbsent(sessionId, k -> new AtomicInteger(0));
        int callCount = counter.incrementAndGet();

        // Preserve local "approve after 3 polls" simulation so onboarding flows
        // get a deterministic PENDING→COMPLETED transition during tests.
        String status = callCount >= 3 ? "COMPLETED" : "PENDING_USER_ACTION";

        Map<String, Object> result = new LinkedHashMap<>(upstream);
        result.put("sessionId", sessionId);
        result.put("status", status);
        result.put("callCount", callCount);

        log.info("Nafath status: sessionId={}, check={}/3, status={}", sessionId, callCount, status);
        return result;
    }

    public Map<String, Object> getVerificationResult(String sessionId, String nationalId) {
        return getVerificationResult(sessionId, nationalId, null, null, null);
    }

    public Map<String, Object> getVerificationResult(String sessionId, String nationalId,
                                                      String customerId, String applicationId,
                                                      String contextType) {
        log.info("Nafath via middleware: fetching verification result for sessionId={}, nationalId={}",
                sessionId, maskId(nationalId));

        var requestBody = Map.<String, Object>of(
                "transId", sessionId,
                "nationalId", nationalId
        );
        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_CHECK_STATUS, requestBody, nationalId, null, sessionId,
                customerId, applicationId, contextType);

        // Build the demographic envelope expected by downstream consumers.
        // Pass through whatever Nafath returned and back-fill any keys the
        // KYC service relies on for backwards compatibility.
        Map<String, Object> userData = new LinkedHashMap<>(upstream);
        userData.put("sessionId", sessionId);
        userData.putIfAbsent("nationalId", nationalId);
        userData.putIfAbsent("status", "VERIFIED");
        userData.putIfAbsent("fullNameAr",
                joinNonBlank(upstream.get("firstName"), upstream.get("secondName"),
                        upstream.get("thirdName"), upstream.get("lastName")));
        userData.putIfAbsent("fullNameEn",
                joinNonBlank(upstream.get("englishFirstName"), upstream.get("englishSecondName"),
                        upstream.get("englishThirdName"), upstream.get("englishLastName")));
        userData.putIfAbsent("dateOfBirth", upstream.get("dateOfBirthG"));
        userData.putIfAbsent("dateOfBirthHijri", upstream.get("dateOfBirthH"));
        userData.putIfAbsent("gender", upstream.get("gender"));
        userData.putIfAbsent("nationality",
                upstream.get("nationalityCode") != null ? "SAU" : null);
        userData.putIfAbsent("idExpiryDate", upstream.get("iqamaExpiryDateG"));
        userData.putIfAbsent("idExpiryDateHijri", upstream.get("iqamaExpiryDateH"));

        // National address is an array — flatten the first entry for legacy keys
        var addresses = upstream.get("nationalAddress");
        if (addresses instanceof java.util.List<?> list && !list.isEmpty()
                && list.get(0) instanceof Map<?, ?> firstAddr) {
            @SuppressWarnings("unchecked")
            var addr = (Map<String, Object>) firstAddr;
            userData.putIfAbsent("addressCity", addr.get("city"));
            userData.putIfAbsent("addressRegion", addr.get("regionName"));
            userData.putIfAbsent("addressDistrict", addr.get("district"));
            userData.putIfAbsent("addressStreet", addr.get("streetName"));
            userData.putIfAbsent("addressPostalCode", addr.get("postCode"));
            userData.putIfAbsent("addressBuildingNumber", addr.get("buildingNumber"));
        }

        userData.putIfAbsent("verifiedAt", Instant.now().toString());
        return userData;
    }

    private String asString(Object... candidates) {
        for (Object c : candidates) {
            if (c != null && !c.toString().isBlank()) return c.toString();
        }
        return null;
    }

    /** Nafath/Tahaquq sandbox returns `random` as a JSON string ("42"); live provider may
     *  return a number. Normalise to Integer so downstream casts cannot blow up. */
    private Integer coerceToInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            log.warn("Could not coerce '{}' to Integer, defaulting to 0", value);
            return 0;
        }
    }

    private String joinNonBlank(Object... parts) {
        var sb = new StringBuilder();
        for (Object p : parts) {
            if (p == null) continue;
            String s = p.toString().trim();
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(s);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }
}
