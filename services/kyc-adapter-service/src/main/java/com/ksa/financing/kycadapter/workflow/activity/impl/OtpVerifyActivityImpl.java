package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.port.in.VerifyOtpUseCase;
import com.ksa.islamic.orchestration.activity.kyc.OtpVerifyActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OtpVerifyActivityImpl implements OtpVerifyActivity {

    private final VerifyOtpUseCase verifyOtpUseCase;

    @Override
    public OtpVerifyResult verifyOtp(OtpVerifyInput input) {
        log.info("Verifying OTP for nationalId={}", input.nationalId());
        try {
            VerifyOtpUseCase.VerifyOtpResult result = verifyOtpUseCase.verify(
                    new VerifyOtpUseCase.VerifyOtpCommand(
                            input.nationalId(),
                            input.otpCode(),
                            input.otpRequestId()
                    )
            );

            return new OtpVerifyResult(
                    result.verified(),
                    result.failureReason()
            );
        } catch (Exception e) {
            log.error("OTP verification failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
