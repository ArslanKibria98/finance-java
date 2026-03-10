package com.ksa.financing.wallet.workflow.activity.impl;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import com.ksa.islamic.orchestration.activity.wallet.WalletCreationActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class WalletCreationActivityImpl implements WalletCreationActivity {

    private final CreateWalletUseCase createWalletUseCase;

    @Override
    public WalletCreationResult createWallet(WalletCreationInput input) {
        log.info("Creating wallet for customerId={}", input.customerId());
        try {
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            Wallet wallet = createWalletUseCase.create(
                    new CreateWalletUseCase.CreateWalletCommand(
                            tenantUuid,
                            UUID.fromString(input.customerId()),
                            input.currency() != null ? input.currency() : "SAR"
                    )
            );

            return new WalletCreationResult(
                    wallet.getId().toString(),
                    true
            );
        } catch (Exception e) {
            log.error("Wallet creation failed for customerId={}: {}", input.customerId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
