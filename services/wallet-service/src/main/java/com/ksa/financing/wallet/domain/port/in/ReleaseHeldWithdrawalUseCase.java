package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.WalletWithdrawal;

import java.util.UUID;

/**
 * Compliance officer / admin action — release a HELD_AML withdrawal.
 * <p>
 * Resumes the SAGA from VALIDATED → debit Fineract → submit to bank rails.
 * Records the releasing user + reason for SAMA audit.
 */
public interface ReleaseHeldWithdrawalUseCase {

    WalletWithdrawal release(ReleaseCommand command);

    record ReleaseCommand(
            UUID tenantId,
            UUID withdrawalId,
            UUID releasedBy,
            String reason
    ) {}
}
