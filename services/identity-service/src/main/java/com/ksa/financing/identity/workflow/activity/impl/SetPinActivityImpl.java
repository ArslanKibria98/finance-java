package com.ksa.financing.identity.workflow.activity.impl;

import com.ksa.financing.identity.domain.port.in.SetPinUseCase;
import com.ksa.islamic.orchestration.activity.identity.SetPinActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SetPinActivityImpl implements SetPinActivity {

    private final SetPinUseCase setPinUseCase;

    @Override
    public SetPinResult setPin(SetPinInput input) {
        log.info("Setting app PIN for nationalId={}", input.nationalId());
        try {
            SetPinUseCase.SetPinResult result = setPinUseCase.setPin(
                    new SetPinUseCase.SetPinCommand(
                            input.keycloakUserId(),
                            input.nationalId(),
                            input.pin(),
                            input.tenantId()
                    )
            );

            return new SetPinResult(
                    result.pinSet(),
                    result.message()
            );
        } catch (Exception e) {
            log.error("Set PIN activity failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
