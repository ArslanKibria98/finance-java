package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.MovementType;
import com.ksa.financing.wallet.domain.model.TransactionPurpose;
import com.ksa.financing.wallet.domain.model.TransferStatus;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletMovement;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.in.GetTransactionHistoryUseCase;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.WalletMovementRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.domain.port.out.WalletTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a unified transaction history per wallet by merging:
 *  - Fineract savings transactions (real money movement + running balance)
 *  - wallet_transfers rows (counterparty info, transferNumber, status)
 *
 * Counterparty wallet/customer info is resolved via WalletRepository + identity-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetTransactionHistoryService implements GetTransactionHistoryUseCase {

    private final WalletRepository walletRepository;
    private final WalletTransferRepository transferRepository;
    private final WalletMovementRepository movementRepository;
    private final FineractSavingsPort fineractPort;
    private final RecipientLookupPort recipientLookupPort;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionItem> getHistory(UUID walletId, int page, int size) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", walletId.toString()));

        // Index transfers by Fineract transferId AND timestamp for matching
        List<WalletTransfer> sent = transferRepository.findBySourceWallet(walletId);
        List<WalletTransfer> received = transferRepository.findByDestinationWallet(walletId);
        Map<String, WalletTransfer> byFineractRef = new HashMap<>();
        for (WalletTransfer t : sent) {
            if (t.getFineractTransferId() != null) byFineractRef.put(t.getFineractTransferId() + ":OUT", t);
        }
        for (WalletTransfer t : received) {
            if (t.getFineractTransferId() != null) byFineractRef.put(t.getFineractTransferId() + ":IN", t);
        }

        // Resolve counterparty wallet metadata cache (per UUID)
        Map<UUID, CounterpartyMeta> counterpartyCache = new HashMap<>();
        // Track matched transfers per request (avoid double-matching)
        java.util.Set<UUID> matchedTransferIds = new java.util.HashSet<>();

        // wallet_movements provide context for non-transfer movements
        // (loan disbursements, repayments, top-ups, withdrawals, fees, etc.)
        List<WalletMovement> movements = movementRepository.findByWalletId(walletId);
        java.util.Set<UUID> matchedMovementIds = new java.util.HashSet<>();

        // Single lookup for the wallet's own customer (used for non-transfer counterparties).
        String holderMaskedMobile = lookupMaskedMobile(wallet.getCustomerId());

        List<TransactionItem> items = new ArrayList<>();
        BigDecimal currentBalance = wallet.getAvailableBalance() != null
                ? wallet.getAvailableBalance() : BigDecimal.ZERO;
        String currency = wallet.getCurrency();

        if (wallet.getFineractSavingsAccountId() != null) {
            try {
                FineractSavingsPort.SavingsAccountInfo info = fineractPort.getAccountInfo(wallet.getFineractSavingsAccountId());
                if (info.availableBalance() != null) currentBalance = info.availableBalance();
                if (info.currency() != null) currency = info.currency();
            } catch (Exception ex) {
                log.warn("Fineract balance fetch failed for walletId={}: {}", walletId, ex.getMessage());
            }

            try {
                List<FineractSavingsPort.SavingsTransaction> fineractTxs =
                        fineractPort.getTransactions(wallet.getFineractSavingsAccountId());

                for (FineractSavingsPort.SavingsTransaction tx : fineractTxs) {
                    String type = tx.transactionType() != null ? tx.transactionType() : "OTHER";
                    String direction = isCredit(type) ? "CREDIT" : "DEBIT";

                    // Try to match this Fineract tx with a wallet_transfer row.
                    // Fineract creates 2 entries per /accounttransfers (1 withdrawal on src, 1 deposit on dst)
                    // We don't have direct Fineract txId↔transferId mapping, so we pair by
                    // amount + direction using the most recent unmatched transfer in the same direction.
                    WalletTransfer matched = matchByAmountDirection(
                            sent, received, tx.amount(), direction, matchedTransferIds);

                    String mappedType;
                    Counterparty counterparty;
                    String purposeNote;
                    String transferNumber;
                    // Fineract returns date-only (no time), so its value collapses every
                    // tx on the same day to 00:00:00Z. Prefer the persisted Instant from
                    // wallet_transfers / wallet_movements when we have a match.
                    Instant timestamp = parseDate(tx.date());

                    if (matched != null) {
                        mappedType = "CREDIT".equals(direction) ? "TRANSFER_IN" : "TRANSFER_OUT";
                        counterparty = buildCounterparty(walletId, matched, counterpartyCache);
                        purposeNote = matched.getPurposeNote();
                        transferNumber = matched.getTransferNumber();
                        Instant transferTs = pickTransferTimestamp(matched);
                        if (transferTs != null) timestamp = transferTs;
                    } else {
                        WalletMovement movementMatch = matchMovement(
                                movements, tx.amount(), direction, matchedMovementIds);
                        if (movementMatch != null) {
                            mappedType = mapPurposeToType(movementMatch.getPurpose(), type);
                            purposeNote = movementMatch.getDescription();
                            transferNumber = movementMatch.getMovementNumber();
                            counterparty = buildExternalCounterparty(wallet, movementMatch, holderMaskedMobile);
                            if (movementMatch.getCreatedAt() != null) timestamp = movementMatch.getCreatedAt();
                        } else {
                            mappedType = mapType(type, null, direction);
                            purposeNote = null;
                            transferNumber = null;
                            counterparty = null;
                        }
                    }

                    items.add(new TransactionItem(
                            tx.transactionId() != null ? tx.transactionId().toString() : null,
                            mappedType,
                            direction,
                            tx.amount(),
                            tx.runningBalance(),
                            tx.reversed() ? "REVERSED" : "COMPLETED",
                            counterparty,
                            purposeNote,
                            transferNumber,
                            tx.transactionId() != null ? tx.transactionId().toString() : null,
                            timestamp));
                }
            } catch (Exception ex) {
                log.warn("Fineract transactions fetch failed walletId={}: {}", walletId, ex.getMessage());
            }
        }

        // Append PENDING / FAILED transfers (these may not be in Fineract yet)
        for (WalletTransfer t : sent) {
            if (t.getStatus() == TransferStatus.COMPLETED) continue;
            items.add(buildPendingTransferItem(walletId, t, "DEBIT", "TRANSFER_OUT", counterpartyCache));
        }
        for (WalletTransfer t : received) {
            if (t.getStatus() == TransferStatus.COMPLETED) continue;
            items.add(buildPendingTransferItem(walletId, t, "CREDIT", "TRANSFER_IN", counterpartyCache));
        }

        // Sort newest first. Fineract only stores date (not time), so same-day txs collide
        // on timestamp — break ties with fineractTransactionId DESC (numeric IDs ascend with time).
        items.sort((a, b) -> {
            Instant ai = a.timestamp() != null ? a.timestamp() : Instant.EPOCH;
            Instant bi = b.timestamp() != null ? b.timestamp() : Instant.EPOCH;
            int cmp = bi.compareTo(ai);
            if (cmp != 0) return cmp;
            return Long.compare(parseTxIdOrZero(b.fineractTransactionId()), parseTxIdOrZero(a.fineractTransactionId()));
        });

        int total = items.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<TransactionItem> pageItems = new ArrayList<>(items.subList(from, to));

        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        boolean first = page == 0;
        boolean last = to >= total;
        PageMetadata metadata = new PageMetadata(page, size, total, totalPages, first, last, pageItems.isEmpty());
        return new PageResponse<>(pageItems, metadata);
    }

    private TransactionItem buildPendingTransferItem(UUID walletId, WalletTransfer t, String direction,
                                                     String type, Map<UUID, CounterpartyMeta> cache) {
        return new TransactionItem(
                t.getId().toString(),
                type,
                direction,
                t.getAmount(),
                null,
                t.getStatus() != null ? t.getStatus().name() : null,
                buildCounterparty(walletId, t, cache),
                t.getPurposeNote(),
                t.getTransferNumber(),
                t.getFineractTransferId(),
                t.getInitiatedAt());
    }

    private boolean isCredit(String fineractType) {
        if (fineractType == null) return false;
        String lc = fineractType.toLowerCase();
        return lc.contains("deposit") || lc.contains("interest posting") || lc.contains("refund");
    }

    private String mapType(String fineractType, WalletTransfer matched, String direction) {
        if (matched != null) {
            return "CREDIT".equals(direction) ? "TRANSFER_IN" : "TRANSFER_OUT";
        }
        if (fineractType == null) return "OTHER";
        String lc = fineractType.toLowerCase();
        if (lc.contains("deposit")) return "TOP_UP";
        if (lc.contains("withdrawal")) return "WITHDRAWAL";
        if (lc.contains("fee")) return "FEE";
        if (lc.contains("interest")) return "INTEREST";
        return "OTHER";
    }

    private WalletMovement matchMovement(List<WalletMovement> movements,
                                         BigDecimal amount,
                                         String direction,
                                         java.util.Set<UUID> matchedMovementIds) {
        MovementType wanted = "CREDIT".equals(direction) ? MovementType.CREDIT : MovementType.DEBIT;
        for (WalletMovement m : movements) {
            if (matchedMovementIds.contains(m.getId())) continue;
            if (m.getMovementType() != wanted) continue;
            if (m.getAmount() == null || m.getAmount().compareTo(amount) != 0) continue;
            matchedMovementIds.add(m.getId());
            return m;
        }
        return null;
    }

    private String mapPurposeToType(TransactionPurpose purpose, String fineractType) {
        if (purpose == null) return mapType(fineractType, null, null);
        return switch (purpose) {
            case TOP_UP -> "TOP_UP";
            case LOAN_PROCEEDS -> "LOAN_DISBURSEMENT";
            case INSTALLMENT_PAYMENT -> "LOAN_REPAYMENT";
            case EARLY_SETTLEMENT -> "LOAN_SETTLEMENT";
            case FEE_DEDUCTION, TRANSFER_FEE -> "FEE";
            case REFUND -> "REFUND";
            case REVERSAL -> "REVERSAL";
            case WITHDRAWAL -> "WITHDRAWAL";
            case ADJUSTMENT -> "ADJUSTMENT";
            case TRANSFER_OUT -> "TRANSFER_OUT";
            case TRANSFER_IN -> "TRANSFER_IN";
        };
    }

    /**
     * Counterparty for non-transfer movements (loan disbursement, top-up, withdrawal, fee, etc.).
     * No external "source wallet" exists in this domain for these movements (funds originate from
     * Fineract loan account / bank rails / fee accounts), so walletId / walletNumber / customerId
     * are populated from the holder wallet itself — keeps the field shape filled and traceable
     * back to the wallet that the movement landed on. maskedName carries the source label.
     */
    private Counterparty buildExternalCounterparty(Wallet wallet, WalletMovement movement, String maskedMobile) {
        String label = externalCounterpartyLabel(movement);
        if (label == null) return null;
        return new Counterparty(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getCustomerId(),
                label,
                maskedMobile);
    }

    private String lookupMaskedMobile(UUID customerId) {
        if (customerId == null) return null;
        try {
            return recipientLookupPort.lookupByCustomerId(customerId)
                    .map(RecipientLookupPort.UserLookup::maskedMobile)
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Identity lookup failed for customerId={}: {}", customerId, ex.getMessage());
            return null;
        }
    }

    private String externalCounterpartyLabel(WalletMovement movement) {
        TransactionPurpose purpose = movement.getPurpose();
        if (purpose == null) return movement.getDescription();
        return switch (purpose) {
            case LOAN_PROCEEDS -> "Islamic Financing";
            case INSTALLMENT_PAYMENT, EARLY_SETTLEMENT -> "Loan Repayment";
            case TOP_UP -> "Wallet Top-Up";
            case WITHDRAWAL -> "Bank Withdrawal";
            case FEE_DEDUCTION, TRANSFER_FEE -> "Service Fee";
            case REFUND -> "Refund";
            case REVERSAL -> "Reversal";
            case ADJUSTMENT -> "Adjustment";
            case TRANSFER_OUT, TRANSFER_IN -> null;
        };
    }

    /**
     * Best-effort match: find the most recent unmatched COMPLETED transfer
     * with matching amount + direction. Mutates the given set to mark matched.
     */
    private WalletTransfer matchByAmountDirection(List<WalletTransfer> sent,
                                                  List<WalletTransfer> received,
                                                  BigDecimal amount,
                                                  String direction,
                                                  java.util.Set<UUID> matchedTransferIds) {
        List<WalletTransfer> candidates = "DEBIT".equals(direction) ? sent : received;
        for (WalletTransfer t : candidates) {
            if (t.getStatus() != TransferStatus.COMPLETED) continue;
            if (matchedTransferIds.contains(t.getId())) continue;
            if (t.getAmount().compareTo(amount) == 0) {
                matchedTransferIds.add(t.getId());
                return t;
            }
        }
        return null;
    }

    private Counterparty buildCounterparty(UUID currentWalletId, WalletTransfer t,
                                           Map<UUID, CounterpartyMeta> cache) {
        boolean viewerIsSender = t.getSourceWalletId().equals(currentWalletId);
        UUID otherWalletId = viewerIsSender ? t.getDestinationWalletId() : t.getSourceWalletId();
        UUID otherCustomerId = viewerIsSender ? t.getDestinationCustomerId() : t.getSourceCustomerId();

        CounterpartyMeta meta = cache.computeIfAbsent(otherWalletId, id -> resolveCounterparty(id, otherCustomerId));

        // Prefer the masked name frozen at transfer time over a live identity
        // lookup (which can return a null name even when the user exists).
        String storedName = viewerIsSender ? t.getRecipientMaskedName() : t.getSenderMaskedName();
        String maskedName = storedName != null ? storedName : meta.maskedName;

        return new Counterparty(
                otherWalletId,
                meta.walletNumber,
                otherCustomerId,
                maskedName,
                meta.maskedMobile);
    }

    private CounterpartyMeta resolveCounterparty(UUID walletId, UUID customerId) {
        String walletNumber = null;
        String maskedName = null;
        try {
            var w = walletRepository.findById(walletId).orElse(null);
            if (w != null) {
                walletNumber = w.getWalletNumber();
                maskedName = w.getMaskedName();
            }
        } catch (Exception ignore) { }

        // maskedMobile still comes from identity-service (it isn't stored on the
        // wallet); maskedName never does — it stays whatever the wallet holds.
        String maskedMobile = null;
        if (customerId != null) {
            try {
                var lookup = recipientLookupPort.lookupByCustomerId(customerId).orElse(null);
                if (lookup != null) {
                    maskedMobile = lookup.maskedMobile();
                }
            } catch (Exception ex) {
                log.warn("Counterparty identity lookup failed customerId={}: {}", customerId, ex.getMessage());
            }
        }
        return new CounterpartyMeta(walletNumber, maskedName, maskedMobile);
    }

    private long parseTxIdOrZero(String id) {
        if (id == null) return 0L;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private Instant pickTransferTimestamp(WalletTransfer t) {
        if (t.getCompletedAt() != null) return t.getCompletedAt();
        if (t.getInitiatedAt() != null) return t.getInitiatedAt();
        return t.getCreatedAt();
    }

    private Instant parseDate(String iso) {
        if (iso == null) return null;
        try {
            return LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private record CounterpartyMeta(String walletNumber, String maskedName, String maskedMobile) {}
}
