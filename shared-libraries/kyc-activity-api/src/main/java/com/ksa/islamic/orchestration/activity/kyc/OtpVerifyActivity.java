package com.ksa.islamic.orchestration.activity.kyc;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for verifying the OTP code submitted by the customer.
 *
 * After the OTP is sent via OtpSendActivity, this activity verifies the code entered
 * by the customer against the KYC Adapter's OTP verification endpoint. The otpRequestId
 * from the send step is used to correlate the verification with the original OTP request.
 *
 * If verification fails (wrong code, expired OTP), the failureReason provides details
 * that can be communicated back to the customer.
 */
@ActivityInterface
public interface OtpVerifyActivity {

    @ActivityMethod
    OtpVerifyResult verifyOtp(OtpVerifyInput input);

    record OtpVerifyInput(
        String nationalId,
        String otpCode,
        String otpRequestId
    ) {}

    record OtpVerifyResult(
        boolean verified,
        String failureReason
    ) {}
}
