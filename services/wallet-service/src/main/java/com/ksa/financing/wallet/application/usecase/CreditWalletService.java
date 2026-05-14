package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.MovementType;
import com.ksa.financing.wallet.domain.model.TransactionPurpose;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletMovement;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.CreditWalletUseCase;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.WalletMovementRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Credits a wallet's Fineract savings account and records a wallet movement.
 * Used for loan disbursement → wallet flow (replaces IBAN bank transfer).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditWalletService implements CreditWalletUseCase {

    private final WalletRepository walletRepository;
    private final WalletMovementRepository movementRepository;
    private final FineractSavingsPort fineractPort;

    @Override
    @Transactional
    public CreditResult credit(CreditCommand command) {
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "Credit amount must be positive: " + command.amount());
        }

        var existing = movementRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            WalletMovement m = existing.get();
            Wallet w = walletRepository.findById(m.getWalletId())
                    .orElseThrow(() -> NotFoundException.forEntity("Wallet", m.getWalletId().toString()));
            log.info("Idempotent credit hit: movementId={} walletId={}", m.getId(), w.getId());
            return new CreditResult(
                    w.getId(),
                    w.getWalletNumber(),
                    w.getFineractSavingsAccountId(),
                    null,
                    m.getAmount(),
                    m.getBalanceAfter(),
                    m.getId(),
                    "DUPLICATE"
            );
        }

        Wallet wallet = walletRepository.findByCustomerId(command.tenantId(), command.customerId())
                .orElseThrow(() -> NotFoundException.forEntity(
                        "Wallet", "customer=" + command.customerId()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCodes.Wallet.NOT_ACTIVE,
                    "Wallet is not active: " + wallet.getId(),
                    wallet.getId().toString());
        }

        if (wallet.getFineractSavingsAccountId() == null) {
            throw new BusinessException(
                    ErrorCodes.Wallet.NOT_ACTIVE,
                    "Wallet has no linked Fineract savings account: " + wallet.getId(),
                    wallet.getId().toString());
        }

        BigDecimal balanceBefore = wallet.getAvailableBalance() != null
                ? wallet.getAvailableBalance() : BigDecimal.ZERO;

        Long fineractTxnId = null;
        try {
            fineractTxnId = fineractPort.deposit(
                    wallet.getFineractSavingsAccountId(),
                    command.amount(),
                    command.idempotencyKey());
            log.info("Fineract deposit OK: savingsId={} amount={} fineractTxnId={}",
                    wallet.getFineractSavingsAccountId(), command.amount(), fineractTxnId);
        } catch (Exception ex) {
            log.error("Fineract deposit FAILED: savingsId={} amount={} error={}",
                    wallet.getFineractSavingsAccountId(), command.amount(), ex.getMessage(), ex);
            throw new BusinessException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to deposit to Fineract: " + ex.getMessage());
        }

        BigDecimal balanceAfter = balanceBefore.add(command.amount());
        wallet.setAvailableBalance(balanceAfter);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        WalletMovement movement = new WalletMovement();
        movement.setTenantId(command.tenantId());
        movement.setWalletId(wallet.getId());
        movement.setMovementNumber("MOV" + System.currentTimeMillis());
        movement.setMovementType(MovementType.CREDIT);
        movement.setPurpose(command.purpose() != null ? command.purpose() : TransactionPurpose.LOAN_PROCEEDS);
        movement.setAmount(command.amount());
        movement.setBalanceBefore(balanceBefore);
        movement.setBalanceAfter(balanceAfter);
        movement.setReferenceType(command.referenceType());
        movement.setReferenceId(command.referenceId());
        movement.setDescription(command.description());
        movement.setIdempotencyKey(command.idempotencyKey());
        movement.setCreatedAt(Instant.now());
        WalletMovement saved = movementRepository.save(movement);

        return new CreditResult(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getFineractSavingsAccountId(),
                fineractTxnId,
                command.amount(),
                balanceAfter,
                saved.getId(),
                "COMPLETED"
        );
    }
}
