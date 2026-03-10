package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.port.in.ScreenSanctionsUseCase;
import com.ksa.islamic.orchestration.activity.kyc.SanctionsScreeningActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SanctionsScreeningActivityImpl implements SanctionsScreeningActivity {

    private final ScreenSanctionsUseCase screenSanctionsUseCase;

    @Override
    public SanctionsScreeningResult screenSanctions(SanctionsScreeningInput input) {
        log.info("Screening sanctions for nationalId={}", input.nationalId());
        try {
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            ScreenSanctionsUseCase.SanctionsScreeningResult result = screenSanctionsUseCase.screen(
                    new ScreenSanctionsUseCase.ScreenSanctionsCommand(
                            tenantUuid,
                            input.fullName(),
                            input.nationalId(),
                            input.nationality(),
                            Activity.getExecutionContext().getInfo().getActivityId()
                    )
            );

            return new SanctionsScreeningResult(
                    result.screeningStatus(),
                    result.hit() ? 1 : 0,
                    !result.hit()
            );
        } catch (Exception e) {
            log.error("Sanctions screening failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
