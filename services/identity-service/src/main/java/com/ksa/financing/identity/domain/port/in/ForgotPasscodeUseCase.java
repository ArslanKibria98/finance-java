package com.ksa.financing.identity.domain.port.in;

public interface ForgotPasscodeUseCase {

    SendOtpResult sendOtp(SendOtpCommand command);

    VerifyOtpResult verifyOtp(VerifyOtpCommand command);

    ResetPasscodeResult resetPasscode(ResetPasscodeCommand command);

    // ── Commands ─────────────────────────────────────────────────────────────

    record SendOtpCommand(
        String mobileNumber
    ) {}

    record VerifyOtpCommand(
        String mobileNumber,
        String otp
    ) {}

    record ResetPasscodeCommand(
        String mobileNumber,
        String newPasscode,
        String confirmPasscode
    ) {}

    // ── Results ──────────────────────────────────────────────────────────────

    record SendOtpResult(
        boolean sent,
        String maskedMobile,
        String workflowId
    ) {}

    record VerifyOtpResult(
        boolean valid,
        String message
    ) {}

    record ResetPasscodeResult(
        boolean success,
        String message
    ) {}
}
