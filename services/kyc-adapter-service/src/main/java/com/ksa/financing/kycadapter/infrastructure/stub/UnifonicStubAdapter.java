package com.ksa.financing.kycadapter.infrastructure.stub;

import com.ksa.financing.kycadapter.infrastructure.middleware.MiddlewareApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Unifonic SMS / OTP adapter.
 *
 * Routes through middleware-third-party (UNIFONIC_SEND_OTP) so each OTP
 * send is persisted in client_request_test for audit and replay.
 *
 * Public signature is preserved; the boolean return reflects whether the
 * middleware call succeeded.
 */
@Component
public class UnifonicStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(UnifonicStubAdapter.class);

    private static final String API_CODE = "UNIFONIC_SEND_OTP";

    private final MiddlewareApiClient middlewareApiClient;

    public UnifonicStubAdapter(MiddlewareApiClient middlewareApiClient) {
        this.middlewareApiClient = middlewareApiClient;
    }

    public boolean sendSms(String mobileNumber, String otpCode) {
        String maskedMobile = maskMobile(mobileNumber);
        log.info("Unifonic via middleware: sending OTP to mobile={}", maskedMobile);

        var requestBody = Map.<String, Object>of(
                "recipient", mobileNumber,
                "body", "Your OTP is " + otpCode,
                "appSid", "kyc-adapter-service"
        );

        try {
            middlewareApiClient.invokeAsMap(API_CODE, requestBody, null, mobileNumber, null);
            log.info("*** UNIFONIC: OTP for {} is {} ***", maskedMobile, otpCode);
            return true;
        } catch (Exception ex) {
            log.error("Unifonic OTP send failed for mobile={}, error={}", maskedMobile, ex.getMessage());
            return false;
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "***";
        return "***" + mobile.substring(mobile.length() - 4);
    }
}
