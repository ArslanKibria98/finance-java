package com.ksa.financing.kycadapter.domain.port.in;

public interface SendOtpUseCase {
    SendOtpResult send(SendOtpCommand command);

    record SendOtpCommand(String nationalId, String mobileNumber, String idempotencyKey) {}
    record SendOtpResult(String otpRequestId, boolean sent, String maskedMobile) {}
}
