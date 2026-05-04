package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletStatus;

import java.util.Optional;
import java.util.UUID;

public interface LookupRecipientUseCase {

    Optional<RecipientView> lookup(String mobileNumber);

    record RecipientView(
            UUID walletId,
            String walletNumber,
            String maskedName,
            String maskedMobile,
            String currency,
            WalletStatus walletStatus,
            String userStatus,
            boolean canReceive,
            String reason
    ) {}
}
