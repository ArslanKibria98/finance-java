package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.wallet.domain.model.IbftStatus;
import com.ksa.financing.wallet.domain.model.IbftTransaction;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.out.IbftTransactionRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Per-transfer settlement transitions, each in its OWN transaction (REQUIRES_NEW) so one
 * transfer's failure never rolls back the reconciliation of others. Status-guarded → idempotent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IbftSettlementService {

    private static final EnumSet<IbftStatus> RECONCILABLE = EnumSet.of(IbftStatus.SUBMITTED, IbftStatus.PROCESSING);

    private final IbftTransactionRepository ibftRepository;
    private final WalletRepository walletRepository;
    private final WalletHoldService holdService;

    /** Settlement success → release hold + withdraw (final debit) → COMPLETED. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeSettlement(UUID ibftId) {
        IbftTransaction tx = ibftRepository.findById(ibftId).orElse(null);
        if (tx == null || !RECONCILABLE.contains(tx.getStatus())) return;
        Wallet wallet = walletRepository.findById(tx.getWalletId()).orElseThrow();
        UUID debitMovementId = holdService.finalizeDebit(wallet, tx);
        tx.setDebitMovementId(debitMovementId);
        tx.setScotiaStatus("SETTLED");
        tx.setStatus(IbftStatus.COMPLETED);
        tx.setSettledAt(Instant.now());
        ibftRepository.save(tx);
        log.info("IBFT COMPLETED (settled) id={} amount={}", tx.getId(), tx.getAmount());
    }

    /** Settlement rejected → release hold → FAILED. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failSettlement(UUID ibftId, String code, String message) {
        IbftTransaction tx = ibftRepository.findById(ibftId).orElse(null);
        if (tx == null || !RECONCILABLE.contains(tx.getStatus())) return;
        Wallet wallet = walletRepository.findById(tx.getWalletId()).orElseThrow();
        UUID releaseMovementId = holdService.releaseHold(wallet, tx);
        tx.setReleaseMovementId(releaseMovementId);
        tx.setStatus(IbftStatus.FAILED);
        tx.setErrorCode(code);
        tx.setErrorMessage(message);
        tx.setFailedAt(Instant.now());
        ibftRepository.save(tx);
        log.info("IBFT FAILED (settlement rejected) id={} amount={}", tx.getId(), tx.getAmount());
    }

    /** Still pending → bump attempts + keep PROCESSING. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPending(UUID ibftId, String submissionStatus) {
        IbftTransaction tx = ibftRepository.findById(ibftId).orElse(null);
        if (tx == null || !RECONCILABLE.contains(tx.getStatus())) return;
        tx.setStatus(IbftStatus.PROCESSING);
        tx.setScotiaStatus(submissionStatus);
        tx.setInquiryAttempts(tx.getInquiryAttempts() + 1);
        tx.setLastInquiredAt(Instant.now());
        ibftRepository.save(tx);
    }
}
