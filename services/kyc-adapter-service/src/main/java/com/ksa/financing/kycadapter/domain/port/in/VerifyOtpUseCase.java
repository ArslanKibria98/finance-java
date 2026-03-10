package com.ksa.financing.kycadapter.domain.port.in;

public interface VerifyOtpUseCase {
    VerifyOtpResult verify(VerifyOtpCommand command);

    record VerifyOtpCommand(String nationalId, String otpCode, String otpRequestId) {}
    record VerifyOtpResult(boolean verified, String failureReason) {}
}
