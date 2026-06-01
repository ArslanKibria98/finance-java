package com.ksa.financing.onboarding.shared.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.io.Serializable;

@ActivityInterface
public interface DualOtpActivity {

    @ActivityMethod
    OtpSendResult sendMobileOtp(String mobileNumber);

    @ActivityMethod
    OtpSendResult sendEmailOtp(String email);

    @ActivityMethod
    OtpVerifyResult verifyMobileOtp(String otpRequestId, String mobileNumber, String otpCode);

    @ActivityMethod
    OtpVerifyResult verifyEmailOtp(String otpRequestId, String email, String otpCode);

    record OtpSendResult(boolean sent, String otpRequestId, String maskedTarget,
                         String failureReason) implements Serializable {}

    record OtpVerifyResult(boolean verified, String failureReason) implements Serializable {}
}
