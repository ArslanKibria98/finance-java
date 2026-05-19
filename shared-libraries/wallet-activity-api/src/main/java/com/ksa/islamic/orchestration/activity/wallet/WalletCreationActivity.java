package com.ksa.islamic.orchestration.activity.wallet;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for creating a customer wallet.
 *
 * Creates a digital wallet for the newly onboarded customer via the Wallet Service.
 * The wallet is initialized with SAR currency by default for KSA operations.
 *
 * Note: The wallet may also be created asynchronously via a Kafka event from the
 * Customer Service. This activity serves as an explicit/fallback creation mechanism.
 * If the wallet already exists (HTTP 409), the existing walletId is returned.
 */
@ActivityInterface
public interface WalletCreationActivity {

    @ActivityMethod
    WalletCreationResult createWallet(WalletCreationInput input);

    record WalletCreationInput(
        String customerId,
        String tenantId,
        String currency,
        String iban,
        String fullName
    ) {}

    record WalletCreationResult(
        String walletId,
        boolean created
    ) {}
}
