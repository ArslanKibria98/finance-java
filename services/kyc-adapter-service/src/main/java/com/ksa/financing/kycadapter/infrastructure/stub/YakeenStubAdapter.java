package com.ksa.financing.kycadapter.infrastructure.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stub adapter simulating the Yakeen government API for identity verification.
 * Yakeen provides demographics lookup and identity verification for Saudi nationals
 * and residents via the National Information Center (NIC).
 */
@Component
public class YakeenStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(YakeenStubAdapter.class);

    /**
     * Verify identity and retrieve demographics for a given national ID and date of birth.
     * Simulates a 300ms delay and returns mock demographic data.
     *
     * @param nationalId  the national ID to verify
     * @param dateOfBirth the date of birth for additional verification
     * @return a map containing demographic data
     */
    public Map<String, Object> verifyIdentity(String nationalId, LocalDate dateOfBirth) {
        log.info("Yakeen stub: Verifying identity for nationalId={}, dateOfBirth={}",
                maskId(nationalId), dateOfBirth);

        simulateDelay(300);

        Map<String, Object> demographics = new LinkedHashMap<>();
        demographics.put("fullNameAr", "\u0645\u062d\u0645\u062f \u0639\u0628\u062f\u0627\u0644\u0644\u0647 \u0627\u0644\u0631\u0627\u0634\u062f");
        demographics.put("fullNameEn", "Mohammed Abdullah Al-Rashed");
        demographics.put("gender", "MALE");
        demographics.put("nationality", "SAU");
        demographics.put("dateOfBirth", dateOfBirth != null ? dateOfBirth.toString() : null);
        demographics.put("addressCity", "Riyadh");
        demographics.put("addressRegion", "Riyadh Region");
        demographics.put("idExpiryDate", LocalDate.now().plusYears(2).toString());
        demographics.put("nationalId", nationalId);
        demographics.put("verified", true);

        log.info("Yakeen stub: Identity verified successfully for nationalId={}", maskId(nationalId));
        return demographics;
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Yakeen stub: Delay interrupted");
        }
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }
}
