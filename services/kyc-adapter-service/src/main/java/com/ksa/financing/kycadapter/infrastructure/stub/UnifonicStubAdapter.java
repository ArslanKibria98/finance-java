package com.ksa.financing.kycadapter.infrastructure.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub adapter simulating the Unifonic SMS API for OTP delivery.
 * In production, this would be replaced with an actual HTTP client calling the Unifonic service.
 *
 * OTP state (code, attempts, expiry) is now managed in the otp_verifications database table.
 * This adapter is only responsible for SMS delivery.
 */
@Component
public class UnifonicStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(UnifonicStubAdapter.class);

    /**
     * Send an OTP code to the given mobile number via SMS.
     * In production, this calls the Unifonic REST API.
     *
     * @param mobileNumber the mobile number to send the SMS to
     * @param otpCode      the OTP code to include in the SMS
     * @return true if SMS was sent successfully
     */
    public boolean sendSms(String mobileNumber, String otpCode) {
        String maskedMobile = maskMobile(mobileNumber);
        log.info("Unifonic stub: Sending OTP SMS to mobile={}", maskedMobile);

        simulateDelay(200);

        // Stub always succeeds — log the OTP for dev/test visibility
        log.info("*** UNIFONIC STUB: OTP for {} is {} ***", maskedMobile, otpCode);

        log.info("Unifonic stub: SMS sent successfully to mobile={}", maskedMobile);
        return true;
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Unifonic stub: Delay interrupted");
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "***";
        return "***" + mobile.substring(mobile.length() - 4);
    }
}
