package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.port.in.SendOtpUseCase;
import com.ksa.islamic.orchestration.activity.kyc.OtpSendActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OtpSendActivityImpl implements OtpSendActivity {

    private final SendOtpUseCase sendOtpUseCase;

    @Override
    public OtpSendResult sendOtp(OtpSendInput input) {
        log.info("Sending OTP for nationalId={}", input.nationalId());
        try {
            SendOtpUseCase.SendOtpResult result = sendOtpUseCase.send(
                    new SendOtpUseCase.SendOtpCommand(
                            input.nationalId(),
                            input.mobileNumber(),
                            Activity.getExecutionContext().getInfo().getActivityId()
                    )
            );

            return new OtpSendResult(
                    result.otpRequestId(),
                    result.sent(),
                    result.maskedMobile()
            );
        } catch (Exception e) {
            log.error("OTP send failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
