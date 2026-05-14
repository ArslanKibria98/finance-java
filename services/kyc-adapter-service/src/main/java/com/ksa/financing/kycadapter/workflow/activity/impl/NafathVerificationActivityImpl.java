package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.port.in.InitiateNafathUseCase;
import com.ksa.islamic.orchestration.activity.kyc.NafathVerificationActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class NafathVerificationActivityImpl implements NafathVerificationActivity {

    private final InitiateNafathUseCase initiateNafathUseCase;

    @Override
    public NafathInitiationResult initiateNafath(NafathInitiationInput input) {
        log.info("Initiating Nafath verification for nationalId={}", input.nationalId());
        try {
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            InitiateNafathUseCase.NafathInitiationResult result = initiateNafathUseCase.initiate(
                    new InitiateNafathUseCase.InitiateNafathCommand(
                            tenantUuid,
                            input.nationalId(),
                            Activity.getExecutionContext().getInfo().getActivityId(),
                            input.customerId(),
                            input.applicationId(),
                            input.contextType() != null ? input.contextType() : "ONBOARDING"
                    )
            );

            return new NafathInitiationResult(
                    result.session().getId() != null ? result.session().getId().toString() : null,
                    result.transactionId(),
                    result.randomNumber(),
                    true,
                    result.nafathVerificationData()
            );
        } catch (Exception e) {
            log.error("Nafath initiation failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
