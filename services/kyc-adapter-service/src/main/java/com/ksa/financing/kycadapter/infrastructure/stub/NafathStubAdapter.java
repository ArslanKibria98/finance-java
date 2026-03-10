package com.ksa.financing.kycadapter.infrastructure.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub adapter simulating the Nafath government API for national identity verification.
 * Nafath is the Saudi digital identity service that requires user confirmation
 * of a random number displayed on their Nafath mobile app.
 *
 * Uses a ConcurrentHashMap to track session call counts and simulate
 * the user approving after 3 status check calls.
 */
@Component
public class NafathStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(NafathStubAdapter.class);

    private final ConcurrentHashMap<String, AtomicInteger> sessionCallCounts = new ConcurrentHashMap<>();

    /**
     * Initiate a Nafath verification session for a national ID.
     * Returns a provider session ID, random number, and initial PENDING status.
     *
     * @param nationalId the national ID to verify
     * @return a map containing session details
     */
    public Map<String, Object> initiateSession(String nationalId) {
        log.info("Nafath stub: Initiating session for nationalId={}", maskId(nationalId));

        String sessionId = UUID.randomUUID().toString();
        int randomNumber = 42;

        sessionCallCounts.put(sessionId, new AtomicInteger(0));

        log.info("Nafath stub: Session created sessionId={}, randomNumber={}", sessionId, randomNumber);

        return Map.of(
                "sessionId", sessionId,
                "randomNumber", randomNumber,
                "status", "PENDING_USER_ACTION",
                "nationalId", nationalId
        );
    }

    /**
     * Check the status of a Nafath verification session.
     * Simulates user approval after 3 status check calls.
     *
     * @param sessionId the provider session ID to check
     * @return a map containing the current session status
     */
    public Map<String, Object> checkSessionStatus(String sessionId) {
        log.info("Nafath stub: Checking session status for sessionId={}", sessionId);

        AtomicInteger counter = sessionCallCounts.computeIfAbsent(sessionId, k -> new AtomicInteger(0));
        int callCount = counter.incrementAndGet();

        String status;
        if (callCount >= 3) {
            status = "COMPLETED";
            log.info("Nafath stub: Session {} completed after {} checks (user approved)", sessionId, callCount);
        } else {
            status = "PENDING_USER_ACTION";
            log.info("Nafath stub: Session {} still pending, check {}/3", sessionId, callCount);
        }

        return Map.of(
                "sessionId", sessionId,
                "status", status,
                "callCount", callCount
        );
    }

    /**
     * Get the verification result from Nafath after session verification.
     * This is the third Nafath API (inquiry) that returns verified citizen demographics
     * including address fields and verification timestamp from the National Information
     * Center after the user confirms in the Nafath app.
     *
     * @param sessionId  the provider session ID for the verification
     * @param nationalId the national ID to fetch data for
     * @return a map containing the verified identity and address data
     */
    public Map<String, Object> getVerificationResult(String sessionId, String nationalId) {
        log.info("Nafath stub: Fetching verification result for sessionId={}, nationalId={}", sessionId, maskId(nationalId));

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("sessionId", sessionId);
        userData.put("nationalId", nationalId);
        userData.put("status", "VERIFIED");
        userData.put("fullNameAr", "محمد أحمد الخالد");
        userData.put("fullNameEn", "Mohammed Ahmed Al Khaled");
        userData.put("dateOfBirth", "1990-01-15");
        userData.put("dateOfBirthHijri", "1410-06-19");
        userData.put("gender", "MALE");
        userData.put("nationality", "SAU");
        userData.put("idExpiryDate", "2030-05-15");
        userData.put("idExpiryDateHijri", "1452-01-21");
        userData.put("addressCity", "Riyadh");
        userData.put("addressRegion", "Riyadh Region");
        userData.put("addressDistrict", "Al Olaya");
        userData.put("addressStreet", "King Fahd Road");
        userData.put("addressPostalCode", "12211");
        userData.put("addressBuildingNumber", "4521");
        userData.put("verifiedAt", Instant.now().toString());

        log.info("Nafath stub: Verification result fetched for sessionId={}, nationalId={}", sessionId, maskId(nationalId));
        return userData;
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }
}
