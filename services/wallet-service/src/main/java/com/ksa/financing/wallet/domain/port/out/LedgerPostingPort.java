package com.ksa.financing.wallet.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Output port for posting double-entry GL journal entries to ledger-service
 * for external fund transfers.
 *
 * Best-effort: implementations return the created journal entry id, or {@code null}
 * if posting failed (the transfer itself must not fail because of a GL hiccup).
 */
public interface LedgerPostingPort {

    enum Direction { OUTBOUND, INBOUND }

    /**
     * Post a balanced 2-line entry for an external transfer.
     *   OUTBOUND: Dr Consumer Wallet / Cr Scotia RTP Clearing
     *   INBOUND : Dr Scotia RTP Clearing / Cr Consumer Wallet
     *
     * @return ledger journal entry id (UUID string) or {@code null} on failure.
     */
    String postExternalTransfer(UUID tenantId,
                                UUID transferId,
                                String transferNumber,
                                Direction direction,
                                BigDecimal amount,
                                String idempotencyKey,
                                UUID createdBy);

    /**
     * Post a balanced 2-line entry for an IBFT leg.
     *   OUTBOUND (hold/settle): Dr Consumer Wallet / Cr IBFT Settlement Clearing
     *   INBOUND (release):      Dr IBFT Settlement Clearing / Cr Consumer Wallet
     */
    String postIbft(UUID tenantId,
                    UUID ibftId,
                    String ibftNumber,
                    Direction direction,
                    BigDecimal amount,
                    String idempotencyKey,
                    UUID createdBy);
}
