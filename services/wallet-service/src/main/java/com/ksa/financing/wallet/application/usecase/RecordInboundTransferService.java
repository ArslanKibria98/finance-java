package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;
import com.ksa.financing.wallet.domain.model.ExternalTransferDirection;
import com.ksa.financing.wallet.domain.model.ExternalTransferStatus;
import com.ksa.financing.wallet.domain.model.TransactionPurpose;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.CreditWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.RecordInboundTransferUseCase;
import com.ksa.financing.wallet.domain.port.out.ExternalFundTransferRepository;
import com.ksa.financing.wallet.domain.port.out.LedgerPostingPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Inbound external fund transfer — credits a user's wallet for money received from an
 * external bank account into the platform's Scotia corporate account.
 *
 * Reuses {@link CreditWalletUseCase} for the actual deposit + movement, then records the
 * inbound leg in {@code external_fund_transfers} and posts the GL entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordInboundTransferService implements RecordInboundTransferUseCase {

    private final WalletRepository walletRepository;
    private final ExternalFundTransferRepository transferRepository;
    private final CreditWalletUseCase creditWalletUseCase;
    private final LedgerPostingPort ledgerPostingPort;

    @Value("${ksa.wallet.scotia.default-currency:CAD}")
    private String defaultCurrency;

    @Override
    @Transactional
    public ExternalFundTransfer record(RecordInboundTransferCommand command) {
        log.info("Recording inbound external transfer account={} customer={} amount={} key={}",
                command.accountNumber(), command.customerId(), command.amount(), command.idempotencyKey());

        // 1. Idempotency replay
        var existing = transferRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET.EXT_TRANSFER.AMOUNT_INVALID",
                    "Inbound amount must be positive");
        }

        // 2. Resolve wallet (by virtual account number or customerId)
        Wallet wallet = resolveWallet(command);
        String currency = command.currency() != null ? command.currency() : defaultCurrency;

        // 3. Persist inbound transfer (INITIATED) to obtain a stable id for references
        ExternalFundTransfer transfer = newTransfer(command, wallet, currency);
        ExternalFundTransfer saved = transferRepository.save(transfer);

        // 4. Credit the wallet (deposit to Fineract + TRANSFER_IN movement, idempotent)
        CreditWalletUseCase.CreditResult credit = creditWalletUseCase.credit(
                new CreditWalletUseCase.CreditCommand(
                        command.tenantId(),
                        wallet.getCustomerId(),
                        command.amount(),
                        TransactionPurpose.TRANSFER_IN,
                        "EXTERNAL_TRANSFER",
                        saved.getId(),
                        "External transfer in " + saved.getTransferNumber()
                                + (command.senderName() != null ? " from " + command.senderName() : ""),
                        command.idempotencyKey() + ":XCR"));
        saved.setMovementId(credit.movementId());

        // 5. GL posting (best-effort): Dr Scotia RTP Clearing / Cr Consumer Wallet
        String ledgerEntryId = ledgerPostingPort.postExternalTransfer(
                command.tenantId(), saved.getId(), saved.getTransferNumber(),
                LedgerPostingPort.Direction.INBOUND, command.amount(),
                command.idempotencyKey(), null);
        if (ledgerEntryId != null) {
            try {
                saved.setLedgerEntryId(UUID.fromString(ledgerEntryId));
            } catch (Exception ignore) {
                // entry id not a UUID — leave null
            }
        }

        // 6. Complete
        saved.setStatus(ExternalTransferStatus.COMPLETED);
        saved.setCompletedAt(Instant.now());
        ExternalFundTransfer completed = transferRepository.save(saved);
        log.info("Inbound external transfer COMPLETED id={} amount={} newBalance={}",
                completed.getId(), completed.getAmount(), credit.newAvailableBalance());
        return completed;
    }

    private Wallet resolveWallet(RecordInboundTransferCommand command) {
        if (command.accountNumber() != null && !command.accountNumber().isBlank()) {
            return walletRepository.findByAccountNumber(command.tenantId(), command.accountNumber())
                    .orElseThrow(() -> NotFoundException.forEntity("Wallet", "account=" + command.accountNumber()));
        }
        if (command.customerId() != null) {
            return walletRepository.findByCustomerId(command.tenantId(), command.customerId())
                    .orElseThrow(() -> NotFoundException.forEntity("Wallet", "customer=" + command.customerId()));
        }
        throw new BusinessException("WALLET.EXT_TRANSFER.TARGET_REQUIRED",
                "accountNumber or customerId is required");
    }

    private ExternalFundTransfer newTransfer(RecordInboundTransferCommand command, Wallet wallet, String currency) {
        UUID id = UUID.randomUUID();
        ExternalFundTransfer t = new ExternalFundTransfer();
        t.setId(id);
        t.setTenantId(command.tenantId());
        t.setTransferNumber("EXTIN-" + System.currentTimeMillis() + "-" + id.toString().substring(0, 8));
        t.setDirection(ExternalTransferDirection.INBOUND);
        t.setWalletId(wallet.getId());
        t.setCustomerId(wallet.getCustomerId());
        t.setAccountNumber(wallet.getAccountNumber());
        t.setCounterpartyName(command.senderName());
        t.setCounterpartyAccount(command.senderAccount());
        t.setAmount(command.amount());
        t.setFeeAmount(BigDecimal.ZERO);
        t.setCurrency(currency);
        t.setStatus(ExternalTransferStatus.INITIATED);
        t.setPurposeNote(command.reference());
        t.setIdempotencyKey(command.idempotencyKey());
        t.setInitiatedAt(Instant.now());
        return t;
    }
}
