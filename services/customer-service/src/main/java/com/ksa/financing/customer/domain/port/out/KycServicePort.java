package com.ksa.financing.customer.domain.port.out;

import java.util.List;
import java.util.Map;

/**
 * Output port for fetching KYC verification data from KYC Adapter Service.
 * Retrieves Yakeen identity data, Nafath status, and verification sessions.
 */
public interface KycServicePort {

    /**
     * Get cached Yakeen identity data (names EN/AR, DOB, address, nationality).
     */
    Map<String, Object> getYakeenData(String nationalId, String accessToken);

    /**
     * Get all verification sessions for a customer (Nafath, Yakeen, Tahakuk, etc.)
     */
    List<Map<String, Object>> getVerificationSessions(String nationalId, String accessToken);
}
