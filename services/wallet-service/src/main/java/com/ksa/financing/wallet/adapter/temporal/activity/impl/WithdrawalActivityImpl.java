package com.ksa.financing.wallet.adapter.temporal.activity.impl;

import com.ksa.financing.wallet.adapter.temporal.activity.WithdrawalActivity;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.model.WithdrawalStatus;
import com.ksa.financing.wallet.domain.port.out.BankRailsPort;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.WalletWithdrawalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalActivityImpl implements WithdrawalActivity {

    private final FineractSavingsPort fineractPort;
    private final BankRailsPort bankRailsPort;
    private final WalletWithdrawalRepository repository;

    @Override
    public String debitFineract(Long savingsId, BigDecimal totalDebit, String externalRef) {
        Long txn = fineractPort.withdraw(savingsId, totalDebit, externalRef);
        return txn != null ? txn.toString() : null;
    }

    @Override
    public String refundFineract(Long savingsId, BigDecimal totalDebit, String externalRef) {
        Long txn = fineractPort.deposit(savingsId, totalDebit, externalRef);
        return txn != null ? txn.toString() : null;
    }

    @Override
    public BankRailsResult submitToBankRails(BankRailsInput input) {
        BankRailsPort.SubmissionReceipt receipt = bankRailsPort.submit(
                new BankRailsPort.SubmissionRequest(
                        input.withdrawalNumber(),
                        input.idempotencyKey(),
                        input.beneficiaryName(),
                        input.destinationIban(),
                        input.destinationBankCode(),
                        input.amount(),
                        input.currency(),
                        input.purposeCode(),
                        input.narrative()));
        return new BankRailsResult(
                receipt.accepted(),
                receipt.bankReference(),
                receipt.sarieReference(),
                receipt.rejectionCode(),
                receipt.rejectionMessage());
    }

    @Override
    @Transactional
    public void markStatus(UUID withdrawalId, String status,
                           String bankRef, String sarieRef,
                           String errorCode, String errorMessage) {
        WalletWithdrawal w = repository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalStateException("Withdrawal not found: " + withdrawalId));
        WithdrawalStatus newStatus = WithdrawalStatus.valueOf(status);
        w.setStatus(newStatus);
        if (bankRef != null) w.setBankReference(bankRef);
        if (sarieRef != null) w.setSarieReference(sarieRef);
        if (errorCode != null) w.setErrorCode(errorCode);
        if (errorMessage != null) w.setErrorMessage(errorMessage);

        Instant now = Instant.now();
        switch (newStatus) {
            case DEBITED        -> w.setDebitedAt(now);
            case BANK_SUBMITTED -> w.setBankSubmittedAt(now);
            case COMPLETED      -> w.setCompletedAt(now);
            case FAILED         -> w.setFailedAt(now);
            case COMPENSATED    -> w.setCompensatedAt(now);
            default -> { /* no timestamp for other states */ }
        }
        repository.save(w);
        log.info("Withdrawal {} → {}", withdrawalId, newStatus);
    }
}
