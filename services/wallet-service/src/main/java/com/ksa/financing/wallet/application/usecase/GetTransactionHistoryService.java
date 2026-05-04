package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.TransferStatus;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.in.GetTransactionHistoryUseCase;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
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
    private final FineractSavingsPort fineractPort;
    private final RecipientLookupPort recipientLookupPort;

    @Override
    @Transactional(readOnly = true)
    public TransactionHistory getHistory(UUID walletId, int page, int size) {
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

                    String mappedType = mapType(type, matched, direction);
                    Counterparty counterparty = matched != null
                            ? buildCounterparty(walletId, matched, counterpartyCache)
                            : null;

                    items.add(new TransactionItem(
                            tx.transactionId() != null ? tx.transactionId().toString() : null,
                            mappedType,
                            direction,
                            tx.amount(),
                            tx.runningBalance(),
                            tx.reversed() ? "REVERSED" : "COMPLETED",
                            counterparty,
                            matched != null ? matched.getPurposeNote() : null,
                            matched != null ? matched.getTransferNumber() : null,
                            tx.transactionId() != null ? tx.transactionId().toString() : null,
                            parseDate(tx.date())));
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

        // Sort newest first
        items.sort((a, b) -> {
            Instant ai = a.timestamp() != null ? a.timestamp() : Instant.EPOCH;
            Instant bi = b.timestamp() != null ? b.timestamp() : Instant.EPOCH;
            return bi.compareTo(ai);
        });

        int total = items.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<TransactionItem> pageItems = items.subList(from, to);

        return new TransactionHistory(
                wallet.getId(),
                wallet.getWalletNumber(),
                currentBalance,
                currency,
                pageItems,
                page,
                size,
                total);
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
        UUID otherWalletId = t.getSourceWalletId().equals(currentWalletId)
                ? t.getDestinationWalletId() : t.getSourceWalletId();
        UUID otherCustomerId = t.getSourceWalletId().equals(currentWalletId)
                ? t.getDestinationCustomerId() : t.getSourceCustomerId();

        CounterpartyMeta meta = cache.computeIfAbsent(otherWalletId, id -> resolveCounterparty(id, otherCustomerId));

        return new Counterparty(
                otherWalletId,
                meta.walletNumber,
                otherCustomerId,
                meta.maskedName,
                meta.maskedMobile);
    }

    private CounterpartyMeta resolveCounterparty(UUID walletId, UUID customerId) {
        String walletNumber = null;
        try {
            walletNumber = walletRepository.findById(walletId).map(Wallet::getWalletNumber).orElse(null);
        } catch (Exception ignore) { }

        String maskedName = null;
        String maskedMobile = null;
        if (customerId != null) {
            try {
                var lookup = recipientLookupPort.lookupByMobile(""); // not used here
            } catch (Exception ignore) { }
        }
        // Try keycloak-id-based lookup is N/A here — we don't have the keycloakId.
        // Best-effort: skip name resolution (requires identity by customerId — not yet implemented).
        // For now show walletNumber + customerId only; extend identity lookup later.
        return new CounterpartyMeta(walletNumber, maskedName, maskedMobile);
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
