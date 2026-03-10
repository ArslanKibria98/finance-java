package com.ksa.financing.kycadapter.infrastructure.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Stub adapter simulating the Tahakuk government API for mobile ownership verification.
 * In production, this would be replaced with an actual HTTP client calling the Tahakuk service.
 */
@Component
public class TahakukStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(TahakukStubAdapter.class);

    /**
     * Verify mobile number ownership against a national ID.
     * Simulates a call to the Tahakuk government API with a 200ms delay.
     *
     * @param nationalId   the national ID of the individual
     * @param mobileNumber the mobile number to verify
     * @return a map containing verification results
     */
    public Map<String, Object> verifyMobileOwnership(String nationalId, String mobileNumber) {
        log.info("Tahakuk stub: Verifying mobile ownership for nationalId={}, mobileNumber={}",
                maskId(nationalId), maskMobile(mobileNumber));

        simulateDelay(200);

        Map<String, Object> result = Map.of(
                "verified", true,
                "carrier", "STC",
                "verifiedAt", Instant.now().toString(),
                "nationalId", nationalId,
                "mobileNumber", mobileNumber
        );

        log.info("Tahakuk stub: Mobile ownership verified successfully for nationalId={}", maskId(nationalId));
        return result;
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Tahakuk stub: Delay interrupted");
        }
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "***";
        return "***" + mobile.substring(mobile.length() - 4);
    }
}
