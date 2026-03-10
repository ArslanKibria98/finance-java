package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.model.VerificationSession;
import com.ksa.financing.kycadapter.domain.port.in.VerifyMobileUseCase;
import com.ksa.islamic.orchestration.activity.kyc.MobileVerificationActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class MobileVerificationActivityImpl implements MobileVerificationActivity {

    private final VerifyMobileUseCase verifyMobileUseCase;

    @Override
    public MobileVerificationResult verifyMobile(MobileVerificationInput input) {
        log.info("Executing mobile verification for nationalId={}", input.nationalId());
        try {
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            VerificationSession session = verifyMobileUseCase.verify(
                    new VerifyMobileUseCase.VerifyMobileCommand(
                            tenantUuid,
                            input.mobileNumber(),
                            input.nationalId(),
                            Activity.getExecutionContext().getInfo().getActivityId()
                    )
            );

            boolean verified = session.getStatus() != null
                    && "COMPLETED".equalsIgnoreCase(session.getStatus().name());

            return new MobileVerificationResult(
                    verified,
                    session.getId() != null ? session.getId().toString() : null,
                    null
            );
        } catch (Exception e) {
            log.error("Mobile verification failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
