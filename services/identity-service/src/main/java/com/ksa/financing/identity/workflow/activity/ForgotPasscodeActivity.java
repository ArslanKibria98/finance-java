package com.ksa.financing.identity.workflow.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Two-step forgot passcode activities:
 * 1. sendOtp    — look up user, generate OTP, store in DB, send via SMS
 * 2. verifyOtp  — verify OTP only (no passcode change)
 * 3. resetPin   — reset PIN after OTP was already verified
 */
@ActivityInterface
public interface ForgotPasscodeActivity {

    @ActivityMethod
    SendOtpOutput sendOtp(SendOtpInput input);

    @ActivityMethod
    VerifyOtpOutput verifyOtp(VerifyOtpInput input);

    @ActivityMethod
    ResetPinOutput resetPin(ResetPinInput input);

    // ── Input / Output records ───────────────────────────────────────────────

    record SendOtpInput(String mobileNumber) {}

    record SendOtpOutput(boolean sent, String maskedMobile, String identityId) {}

    record VerifyOtpInput(String identityId, String userProvidedOtp) {}

    record VerifyOtpOutput(boolean valid, String message) {}

    record ResetPinInput(String identityId, String newPasscode) {}

    record ResetPinOutput(boolean success, String message) {}
}
