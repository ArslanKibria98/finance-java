package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.port.in.ScreenPepUseCase;
import com.ksa.islamic.orchestration.activity.kyc.PepScreeningActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class PepScreeningActivityImpl implements PepScreeningActivity {

    private final ScreenPepUseCase screenPepUseCase;

    @Override
    public PepScreeningResult screenPep(PepScreeningInput input) {
        log.info("PEP screening for nationalId={}", input.nationalId());
        try {
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            ScreenPepUseCase.PepScreeningResult result = screenPepUseCase.screen(
                    new ScreenPepUseCase.ScreenPepCommand(
                            tenantUuid,
                            input.fullName(),
                            input.nationalId(),
                            input.nationality(),
                            input.dateOfBirth(),
                            Activity.getExecutionContext().getInfo().getActivityId()
                    )
            );

            return new PepScreeningResult(
                    result.decision(),
                    result.pepDetected(),
                    result.confidenceScore(),
                    result.matchCount(),
                    result.matchDetails()
            );
        } catch (Exception e) {
            log.error("PEP screening failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
