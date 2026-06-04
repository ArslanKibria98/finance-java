package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.wallet.domain.model.IbftTransaction;
import com.ksa.financing.wallet.domain.model.MovementType;
import com.ksa.financing.wallet.domain.model.TransactionPurpose;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletMovement;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.LedgerPostingPort;
import com.ksa.financing.wallet.domain.port.out.WalletMovementRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wallet ledger operations for IBFT — Fineract true hold + reserved-balance projection +
 * HOLD / FINAL-DEBIT / RELEASE movements + GL postings. Source of truth = Fineract.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletHoldService {

    private final WalletRepository walletRepository;
    private final WalletMovementRepository movementRepository;
    private final FineractSavingsPort fineractPort;
    private final LedgerPostingPort ledgerPostingPort;

    public record HoldResult(Long fineractHoldTxnId, UUID holdMovementId, UUID ledgerEntryId) {}

    /** Block the amount in Fineract, mirror available→reserved, record HOLD movement + GL. */
    public HoldResult placeHold(Wallet wallet, BigDecimal amount, UUID ibftId, String ibftNumber, String key) {
        Long holdTxnId = fineractPort.hold(wallet.getFineractSavingsAccountId(), amount, "IBFT-HOLD:" + ibftNumber);

        BigDecimal before = nz(wallet.getAvailableBalance());
        BigDecimal after = before.subtract(amount);
        wallet.setAvailableBalance(after);
        wallet.setReservedBalance(nz(wallet.getReservedBalance()).add(amount));
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        WalletMovement m = movement(wallet, MovementType.DEBIT, TransactionPurpose.IBFT_HOLD, amount,
                before, after, ibftId, "IBFT hold " + ibftNumber, key + ":HOLD", "HLD");
        UUID ledgerEntryId = parse(ledgerPostingPort.postIbft(wallet.getTenantId(), ibftId, ibftNumber,
                LedgerPostingPort.Direction.OUTBOUND, amount, key + ":HOLD", null));
        return new HoldResult(holdTxnId, m.getId(), ledgerEntryId);
    }

    /** Settlement success: release the Fineract hold then withdraw → money truly leaves. */
    public UUID finalizeDebit(Wallet wallet, IbftTransaction tx) {
        if (tx.getFineractHoldTxnId() != null) {
            fineractPort.releaseHold(wallet.getFineractSavingsAccountId(), tx.getFineractHoldTxnId(),
                    "IBFT-RELEASE-HOLD:" + tx.getIbftNumber());
        }
        fineractPort.withdraw(wallet.getFineractSavingsAccountId(), tx.getAmount(), "IBFT-DEBIT:" + tx.getIbftNumber());

        // reserved → gone; available already reduced at hold time
        wallet.setReservedBalance(nz(wallet.getReservedBalance()).subtract(tx.getAmount()));
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        BigDecimal avail = nz(wallet.getAvailableBalance());
        WalletMovement m = movement(wallet, MovementType.DEBIT, TransactionPurpose.IBFT_DEBIT, tx.getAmount(),
                avail, avail, tx.getId(), "IBFT settled " + tx.getIbftNumber(),
                tx.getIdempotencyKey() + ":DEBIT", "DBT");
        return m.getId();
    }

    /** Settlement failure / create-submit failure: release the Fineract hold → funds restored. */
    public UUID releaseHold(Wallet wallet, IbftTransaction tx) {
        if (tx.getFineractHoldTxnId() != null) {
            fineractPort.releaseHold(wallet.getFineractSavingsAccountId(), tx.getFineractHoldTxnId(),
                    "IBFT-RELEASE:" + tx.getIbftNumber());
        }
        BigDecimal before = nz(wallet.getAvailableBalance());
        BigDecimal after = before.add(tx.getAmount());
        wallet.setAvailableBalance(after);
        wallet.setReservedBalance(nz(wallet.getReservedBalance()).subtract(tx.getAmount()));
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        WalletMovement m = movement(wallet, MovementType.CREDIT, TransactionPurpose.IBFT_RELEASE, tx.getAmount(),
                before, after, tx.getId(), "IBFT released " + tx.getIbftNumber(),
                tx.getIdempotencyKey() + ":RELEASE", "RLS");
        // reverse the hold GL entry
        ledgerPostingPort.postIbft(wallet.getTenantId(), tx.getId(), tx.getIbftNumber(),
                LedgerPostingPort.Direction.INBOUND, tx.getAmount(), tx.getIdempotencyKey() + ":RELEASE", null);
        return m.getId();
    }

    private WalletMovement movement(Wallet w, MovementType type, TransactionPurpose purpose, BigDecimal amount,
                                    BigDecimal before, BigDecimal after, UUID refId, String desc,
                                    String key, String suffix) {
        WalletMovement m = new WalletMovement();
        m.setTenantId(w.getTenantId());
        m.setWalletId(w.getId());
        m.setMovementNumber("MOV" + System.currentTimeMillis() + "-" + suffix);
        m.setMovementType(type);
        m.setPurpose(purpose);
        m.setAmount(amount);
        m.setBalanceBefore(before);
        m.setBalanceAfter(after);
        m.setReferenceType("IBFT");
        m.setReferenceId(refId);
        m.setDescription(desc);
        m.setIdempotencyKey(key);
        m.setCreatedAt(Instant.now());
        return movementRepository.save(m);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private UUID parse(String s) { try { return s != null ? UUID.fromString(s) : null; } catch (Exception e) { return null; } }
}
