package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LookupRecipientUseCase {

    /**
     * Resolve recipient by mobile number OR IBAN. Detects format automatically:
     * - Starts with '+' or all-digit string → treat as mobile.
     * - Starts with 2-letter country code + digits (e.g. SA...) → treat as IBAN.
     */
    Optional<RecipientView> lookup(UUID tenantId, String accountNumber);

    /**
     * Bulk presence check: for each mobile number, returns whether that mobile is
     * registered in our system with an active wallet (i.e. it can receive funds).
     * Used for "find friends with wallet" type flows.
     */
    List<RecipientCheck> checkRecipientsByMobile(List<String> mobileNumbers);

    record RecipientCheck(
            boolean found,
            UUID walletId,
            String walletNumber,
            String iban,
            String maskedName,
            String maskedMobile,
            String currency,
            WalletStatus walletStatus,
            String userStatus,
            boolean canReceive,
            String reason
    ) {}

    record RecipientView(
            UUID walletId,
            String walletNumber,
            String iban,
            String maskedName,
            String maskedMobile,
            String currency,
            WalletStatus walletStatus,
            String userStatus,
            boolean canReceive,
            String reason
    ) {}
}
