package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.TransferChannel;
import com.ksa.financing.wallet.domain.model.TransferStatus;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.in.InitiateTransferUseCase;
import com.ksa.financing.wallet.domain.port.out.EventPublisherPort;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.domain.port.out.WalletTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wallet-to-wallet transfer use case.
 * <p>
 * Source of truth = Fineract savings accounts (Option A).
 * wallet-service stores: transfer audit + idempotency only.
 * Balance reads/writes go directly to Fineract.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateTransferService implements InitiateTransferUseCase {

    private final WalletRepository walletRepository;
    private final WalletTransferRepository transferRepository;
    private final FineractSavingsPort fineractPort;
    private final EventPublisherPort eventPublisher;

    @Override
    @Transactional
    public WalletTransfer initiate(InitiateTransferCommand command) {
        log.info("Initiating transfer src={} dst={} amount={} key={}",
                command.sourceWalletId(), command.destinationWalletId(),
                command.amount(), command.idempotencyKey());

        // 1. Idempotency replay
        var existing = transferRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent replay returning transferId={}", existing.get().getId());
            return existing.get();
        }

        // 2. Basic validations
        validateAmount(command.amount());

        UUID dstWalletIdRaw = command.destinationWalletId();
        if (dstWalletIdRaw == null && command.destinationWalletNumber() != null) {
            Wallet byNumber = walletRepository.findByWalletNumber(command.destinationWalletNumber())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.Wallet.NOT_FOUND,
                            "Destination wallet not found: " + command.destinationWalletNumber()));
            dstWalletIdRaw = byNumber.getId();
        }
        if (dstWalletIdRaw == null) {
            throw new BusinessException(
                    "WALLET.TRANSFER.DESTINATION_REQUIRED",
                    "destinationWalletId or destinationWalletNumber is required");
        }
        if (command.sourceWalletId().equals(dstWalletIdRaw)) {
            throw new BusinessException(
                    "WALLET.TRANSFER.SELF_NOT_ALLOWED",
                    "Source and destination wallets must be different");
        }
        final UUID dstWalletId = dstWalletIdRaw;

        // 3. Load wallets (metadata only — NOT used for balance)
        Wallet source = walletRepository.findById(command.sourceWalletId())
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", command.sourceWalletId().toString()));
        Wallet destination = walletRepository.findById(dstWalletId)
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", dstWalletId.toString()));

        // 4. Tenant + status checks
        if (!source.getTenantId().equals(command.tenantId())) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "Source wallet does not belong to tenant");
        }
        if (!destination.getTenantId().equals(command.tenantId())) {
            throw new BusinessException("WALLET.TRANSFER.CROSS_TENANT_NOT_ALLOWED",
                    "Cross-tenant transfers are not permitted");
        }
        if (source.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.Wallet.NOT_ACTIVE,
                    "Source wallet is not active: " + source.getStatus());
        }
        if (destination.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException("WALLET.TRANSFER.DESTINATION_NOT_ACTIVE",
                    "Destination wallet is not active: " + destination.getStatus());
        }

        // 5. Currency check
        String currency = command.currency() != null ? command.currency() : source.getCurrency();
        if (!source.getCurrency().equalsIgnoreCase(currency)
                || !destination.getCurrency().equalsIgnoreCase(currency)) {
            throw new BusinessException("WALLET.TRANSFER.CURRENCY_MISMATCH",
                    "Currency mismatch between source/destination/request");
        }

        // 6. Fineract account info (source of truth for balance + clientId)
        if (source.getFineractSavingsAccountId() == null
                || destination.getFineractSavingsAccountId() == null) {
            throw new BusinessException("WALLET.TRANSFER.NOT_FINERACT_LINKED",
                    "Wallet not linked to Fineract savings account");
        }

        FineractSavingsPort.SavingsAccountInfo srcInfo;
        FineractSavingsPort.SavingsAccountInfo dstInfo;
        try {
            srcInfo = fineractPort.getAccountInfo(source.getFineractSavingsAccountId());
            dstInfo = fineractPort.getAccountInfo(destination.getFineractSavingsAccountId());
        } catch (Exception ex) {
            log.error("Failed to fetch Fineract account info: {}", ex.getMessage());
            throw new BusinessException("WALLET.TRANSFER.FINERACT_UNAVAILABLE",
                    "Core banking unavailable, please retry");
        }

        // 7. Insufficient funds check (against Fineract balance)
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal totalDebit = command.amount().add(fee);
        if (srcInfo.availableBalance() == null || srcInfo.availableBalance().compareTo(totalDebit) < 0) {
            throw new BusinessException(ErrorCodes.Wallet.INSUFFICIENT_FUNDS,
                    "Insufficient available balance: have=" + srcInfo.availableBalance()
                            + " need=" + totalDebit);
        }

        // 8. Persist transfer record (audit + idempotency) status PROCESSING
        UUID transferId = UUID.randomUUID();
        String transferNumber = "TRF-" + System.currentTimeMillis()
                + "-" + transferId.toString().substring(0, 8);

        WalletTransfer transfer = new WalletTransfer();
        transfer.setId(transferId);
        transfer.setTenantId(command.tenantId());
        transfer.setTransferNumber(transferNumber);
        transfer.setSourceWalletId(source.getId());
        transfer.setDestinationWalletId(destination.getId());
        transfer.setSourceCustomerId(source.getCustomerId());
        transfer.setDestinationCustomerId(destination.getCustomerId());
        transfer.setChannel(parseChannel(command.channel()));
        transfer.setAmount(command.amount());
        transfer.setFeeAmount(fee);
        transfer.setTotalDebit(totalDebit);
        transfer.setCurrency(currency);
        transfer.setStatus(TransferStatus.PROCESSING);
        transfer.setPurposeNote(command.purposeNote());
        transfer.setIdempotencyKey(command.idempotencyKey());
        transfer.setInitiatorUserId(command.initiatorUserId());
        transfer.setInitiatorIp(command.initiatorIp());
        transfer.setInitiatorDeviceId(command.initiatorDeviceId());
        transfer.setInitiatedAt(Instant.now());
        WalletTransfer saved = transferRepository.save(transfer);

        // 9. Call Fineract /accounttransfers (the real money movement)
        try {
            String description = command.purposeNote() != null
                    ? "P2P:" + transferNumber + " " + command.purposeNote()
                    : "P2P:" + transferNumber;

            Long fineractTransferId = fineractPort.transferBetweenSavings(
                    srcInfo.clientId(), srcInfo.savingsId(),
                    dstInfo.clientId(), dstInfo.savingsId(),
                    command.amount(),
                    description);

            saved.setFineractTransferId(fineractTransferId != null ? fineractTransferId.toString() : null);
            saved.setStatus(TransferStatus.COMPLETED);
            saved.setCompletedAt(Instant.now());
            WalletTransfer completed = transferRepository.save(saved);

            log.info("Transfer COMPLETED transferId={} fineractRef={} src={} dst={} amount={}",
                    completed.getId(), fineractTransferId,
                    source.getId(), destination.getId(), completed.getAmount());

            try {
                eventPublisher.publishTransferInitiated(completed);
                eventPublisher.publishTransferCompleted(completed);
            } catch (Exception ex) {
                log.warn("Event publish failed transferId={}: {}", completed.getId(), ex.getMessage());
            }
            return completed;

        } catch (BusinessException be) {
            throw markFailed(saved, be.getErrorCode(), be.getMessage());
        } catch (Exception ex) {
            log.error("Fineract transfer failed transferId={}: {}", saved.getId(), ex.getMessage(), ex);
            throw markFailed(saved, "WALLET.TRANSFER.FINERACT_FAILED",
                    "Fineract transfer failed: " + ex.getMessage());
        }
    }

    private BusinessException markFailed(WalletTransfer transfer, String errorCode, String errorMessage) {
        transfer.setStatus(TransferStatus.FAILED);
        transfer.setErrorCode(errorCode);
        transfer.setErrorMessage(errorMessage);
        WalletTransfer failed = transferRepository.save(transfer);
        try { eventPublisher.publishTransferFailed(failed); } catch (Exception ignore) { }
        return new BusinessException(errorCode, errorMessage);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET.TRANSFER.AMOUNT_INVALID",
                    "Transfer amount must be positive");
        }
        if (amount.scale() > 6) {
            throw new BusinessException("WALLET.TRANSFER.AMOUNT_INVALID",
                    "Transfer amount exceeds allowed scale");
        }
    }

    private TransferChannel parseChannel(String input) {
        if (input == null || input.isBlank()) return TransferChannel.P2P_WALLET_ID;
        try {
            return TransferChannel.valueOf(input);
        } catch (IllegalArgumentException ex) {
            return TransferChannel.P2P_WALLET_ID;
        }
    }
}
