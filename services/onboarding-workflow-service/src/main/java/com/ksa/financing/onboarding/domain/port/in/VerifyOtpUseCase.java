package com.ksa.financing.onboarding.domain.port.in;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

public interface VerifyOtpUseCase {
    VerifyOtpResult verify(String workflowId, String nationalId, String otpCode, String otpRequestId, String keycloakUserId, DeviceInfo deviceInfo);

    record VerifyOtpResult(
        String workflowId,
        String status,
        String accessToken,
        String refreshToken,
        long expiresIn
    ) {}
}
