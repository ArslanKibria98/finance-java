package com.ksa.islamic.orchestration.activity.kyc;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for sending OTP (One-Time Password) to the customer's verified mobile number.
 *
 * After Tahakuk mobile verification confirms the mobile number belongs to the national ID,
 * this activity sends an OTP via the KYC Adapter's OTP service. The OTP must be verified
 * by the customer before onboarding can proceed. The returned otpRequestId is used to
 * correlate the verification attempt in the subsequent OtpVerifyActivity call.
 */
@ActivityInterface
public interface OtpSendActivity {

    @ActivityMethod
    OtpSendResult sendOtp(OtpSendInput input);

    record OtpSendInput(
        String nationalId,
        String mobileNumber
    ) {}

    record OtpSendResult(
        String otpRequestId,
        boolean sent,
        String maskedMobile
    ) {}
}
