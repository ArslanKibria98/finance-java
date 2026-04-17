package com.ksa.financing.identity.workflow.activity.impl;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import com.ksa.financing.identity.workflow.activity.ForgotPasscodeActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

/**
 * NOT a Spring bean — instantiated manually in TemporalWorkerConfig.
 * OTP stored in DB (user_identity_mapping.reset_otp / reset_otp_expiry).
 */
@RequiredArgsConstructor
@Slf4j
public class ForgotPasscodeActivityImpl implements ForgotPasscodeActivity {

    private static final String PIN_ATTRIBUTE   = "app_pin";
    private static final long   OTP_TTL_SECONDS = 600; // 10 minutes

    private final UserIdentityRepository userIdentityRepository;
    private final KeycloakAdapterPort    keycloakAdapter;
    private final String                 realm;

    // ── 1. sendOtp ───────────────────────────────────────────────────────────

    @Override
    public SendOtpOutput sendOtp(SendOtpInput input) {
        log.info("Forgot passcode: sending OTP to mobile ****{}", maskSuffix(input.mobileNumber()));

        String normalizedMobile = normalizeMobile(input.mobileNumber());
        UserIdentity identity = userIdentityRepository.findByMobileNumber(normalizedMobile)
                .orElseThrow(() -> Activity.wrap(
                        new IllegalArgumentException("No account found for the given mobile number")));

        // TODO: Replace with real random OTP + SMS (Unifonic) — static for dev/testing
        String otp     = "469310";
        Instant expiry = Instant.now().plusSeconds(OTP_TTL_SECONDS);

        userIdentityRepository.saveOtp(identity.getId(), otp, expiry);

        log.info("[DEV] Passcode reset OTP for mobile ****{}: {} (expires in {}s)",
                maskSuffix(input.mobileNumber()), otp, OTP_TTL_SECONDS);

        return new SendOtpOutput(true, "****" + maskSuffix(input.mobileNumber()),
                identity.getId().toString());
    }

    // ── 2. verifyOtp ─────────────────────────────────────────────────────────

    @Override
    public VerifyOtpOutput verifyOtp(VerifyOtpInput input) {
        UUID identityId = UUID.fromString(input.identityId());
        log.info("Forgot passcode: verifying OTP for identityId={}", identityId);

        UserIdentity identity = userIdentityRepository.findById(identityId).orElse(null);

        if (identity == null || identity.getResetOtp() == null || identity.getResetOtp().isBlank()) {
            log.warn("No active OTP found for identityId={}", identityId);
            return new VerifyOtpOutput(false, "No active OTP found. Please request a new OTP.");
        }

        if (identity.getResetOtpExpiry() != null && Instant.now().isAfter(identity.getResetOtpExpiry())) {
            userIdentityRepository.clearOtp(identityId);
            return new VerifyOtpOutput(false, "OTP has expired. Please request a new OTP.");
        }

        if (!identity.getResetOtp().equals(input.userProvidedOtp())) {
            log.warn("Invalid OTP for identityId={}", identityId);
            return new VerifyOtpOutput(false, "Invalid OTP. Please try again.");
        }

        // OTP verified — clear from DB (one-time use)
        userIdentityRepository.clearOtp(identityId);
        log.info("OTP verified successfully for identityId={}", identityId);
        return new VerifyOtpOutput(true, "OTP verified successfully.");
    }

    // ── 3. resetPin ──────────────────────────────────────────────────────────

    @Override
    public ResetPinOutput resetPin(ResetPinInput input) {
        UUID identityId = UUID.fromString(input.identityId());
        log.info("Forgot passcode: resetting PIN for identityId={}", identityId);

        UserIdentity identity = userIdentityRepository.findById(identityId).orElse(null);
        if (identity == null) {
            return new ResetPinOutput(false, "User not found.");
        }

        keycloakAdapter.setUserAttribute(realm, identity.getKeycloakUserId(), PIN_ATTRIBUTE,
                input.newPasscode());

        log.info("PIN reset successfully for identityId={}", identityId);
        return new ResetPinOutput(true, "Passcode reset successfully.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String normalizeMobile(String mobile) {
        if (mobile == null) return mobile;
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.startsWith("966")) return "+" + digits;
        if (digits.startsWith("05"))  return "+966" + digits.substring(1);
        if (digits.startsWith("5"))   return "+966" + digits;
        return mobile;
    }

    private String maskSuffix(String value) {
        if (value == null || value.length() < 4) return "****";
        return value.substring(value.length() - 4);
    }
}
