package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.wallet.domain.model.*;
import com.ksa.financing.wallet.domain.port.in.TopUpUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletMovementRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class TopUpService implements TopUpUseCase {

    private final WalletRepository walletRepository;
    private final WalletMovementRepository movementRepository;

    public TopUpService(WalletRepository walletRepository,
                        WalletMovementRepository movementRepository) {
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional
    public TopUpTransaction topUp(TopUpCommand command) {
        var existingMovement = movementRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existingMovement.isPresent()) {
            TopUpTransaction txn = new TopUpTransaction();
            txn.setStatus(TopUpStatus.COMPLETED);
            return txn;
        }

        Wallet wallet = walletRepository.findById(command.walletId())
            .orElseThrow(() -> NotFoundException.forEntity("Wallet", command.walletId().toString()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCodes.Wallet.NOT_ACTIVE,
                    "Wallet is not active: " + command.walletId(),
                    command.walletId().toString());
        }

        if (command.amount().compareTo(wallet.getSingleTopUpLimit()) > 0) {
            throw new BusinessException(
                    ErrorCodes.Wallet.LIMIT_EXCEEDED,
                    "Amount exceeds single top-up limit",
                    command.amount().toString());
        }

        BigDecimal balanceBefore = wallet.getAvailableBalance();
        wallet.setAvailableBalance(balanceBefore.add(command.amount()));
        wallet.setTodayTopUpAmount(wallet.getTodayTopUpAmount().add(command.amount()));
        wallet.setMonthTopUpAmount(wallet.getMonthTopUpAmount().add(command.amount()));
        walletRepository.save(wallet);

        WalletMovement movement = new WalletMovement();
        movement.setTenantId(command.tenantId());
        movement.setWalletId(wallet.getId());
        movement.setMovementNumber("MOV" + System.currentTimeMillis());
        movement.setMovementType(MovementType.CREDIT);
        movement.setPurpose(TransactionPurpose.TOP_UP);
        movement.setAmount(command.amount());
        movement.setBalanceBefore(balanceBefore);
        movement.setBalanceAfter(wallet.getAvailableBalance());
        movement.setIdempotencyKey(command.idempotencyKey());
        movement.setDescription("Wallet top-up via " + command.method());
        movementRepository.save(movement);

        TopUpTransaction txn = new TopUpTransaction();
        txn.setTenantId(command.tenantId());
        txn.setWalletId(wallet.getId());
        txn.setTransactionNumber("TXN" + System.currentTimeMillis());
        txn.setMethod(TopUpMethod.valueOf(command.method()));
        txn.setAmount(command.amount());
        txn.setFeeAmount(BigDecimal.ZERO);
        txn.setNetAmount(command.amount());
        txn.setSourceIban(command.sourceIban());
        txn.setStatus(TopUpStatus.COMPLETED);
        txn.setMovementId(movement.getId());
        txn.setIdempotencyKey(command.idempotencyKey());
        txn.setInitiatedAt(Instant.now());
        txn.setCompletedAt(Instant.now());

        return txn;
    }
}
