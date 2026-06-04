package com.ksa.financing.onboarding.shared.sms;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * Testing-only endpoint to send an OTP over real SMS via Twilio.
 *
 * <p>Generates a random 6-digit code, stores it in Redis (one-time, TTL'd) and delivers
 * it through {@link TwilioSmsClient}. Lets us verify SMS delivery to a real handset
 * without driving the full onboarding workflow. The onboarding {@code SecurityConfig}
 * permits all requests, so no JWT is required.</p>
 */
@RestController
@RequestMapping("/api/v1/onboarding/sms")
public class SmsOtpTestController {

    private static final Logger log = LoggerFactory.getLogger(SmsOtpTestController.class);
    private static final String OTP_KEY_PREFIX = "onboarding:otp:mobile:test:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(5);

    private final TwilioSmsClient sms;
    private final StringRedisTemplate redis;
    private final boolean testMode;

    public SmsOtpTestController(TwilioSmsClient sms, StringRedisTemplate redis,
                                @Value("${twilio.test-mode:false}") boolean testMode) {
        this.sms = sms;
        this.redis = redis;
        this.testMode = testMode;
    }

    public record SendOtpRequest(
            @NotBlank @Pattern(regexp = "^\\+[0-9]{7,15}$",
                    message = "mobileNumber must be E.164 format, e.g. +9665XXXXXXXX")
            String mobileNumber) {}

    public record SendOtpResponse(boolean sent, String requestId, String maskedMobile,
                                  String providerStatus, String providerSid,
                                  String otp, String errorMessage, String timestamp) {}

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String requestId = "sms-otp-" + UUID.randomUUID();
        String code = String.valueOf(100000 + RANDOM.nextInt(900000)); // 6 digits

        redis.opsForValue().set(OTP_KEY_PREFIX + requestId, code, TTL);

        // Test mode: no real provider call — return the OTP so the flow can be
        // verified end-to-end without a paid Twilio number. Never enable in prod.
        if (testMode) {
            log.info("[SMS TEST-MODE] OTP for {} (requestId={}) = {}",
                    mask(request.mobileNumber()), requestId, code);
            return ResponseEntity.ok(new SendOtpResponse(
                    true, requestId, mask(request.mobileNumber()),
                    "SIMULATED", null, code, null,
                    java.time.Instant.now().toString()));
        }

        String body = "Your KSA Financing verification code is: " + code
                + ". It expires in " + TTL.toMinutes() + " minutes.";
        TwilioSmsClient.SmsResult result = sms.send(request.mobileNumber(), body);

        if (!result.sent()) {
            redis.delete(OTP_KEY_PREFIX + requestId);
            log.warn("Test SMS OTP not delivered to {} (requestId={}): {}",
                    mask(request.mobileNumber()), requestId, result.errorMessage());
        }

        return ResponseEntity.ok(new SendOtpResponse(
                result.sent(), requestId, mask(request.mobileNumber()),
                result.status(), result.providerSid(), null, result.errorMessage(),
                java.time.Instant.now().toString()));
    }

    private static String mask(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
