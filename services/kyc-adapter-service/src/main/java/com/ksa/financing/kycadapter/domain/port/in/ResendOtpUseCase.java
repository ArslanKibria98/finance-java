package com.ksa.financing.kycadapter.domain.port.in;

public interface ResendOtpUseCase {
    ResendOtpResult resend(ResendOtpCommand command);

    record ResendOtpCommand(String nationalId, String mobileNumber, String otpRequestId) {}
    record ResendOtpResult(String newOtpRequestId, boolean sent, int remainingAttempts) {}
}
