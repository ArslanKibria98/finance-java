package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;
import com.ksa.financing.wallet.domain.model.ExternalTransferDirection;
import com.ksa.financing.wallet.domain.model.ExternalTransferStatus;
import com.ksa.financing.wallet.domain.model.MovementType;
import com.ksa.financing.wallet.domain.model.TransactionPurpose;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletMovement;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.CreditWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateExternalTransferUseCase;
import com.ksa.financing.wallet.domain.port.out.ExternalFundTransferRepository;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.LedgerPostingPort;
import com.ksa.financing.wallet.domain.port.out.ScotiaRtpPort;
import com.ksa.financing.wallet.domain.port.out.WalletMovementRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound external fund transfer (user wallet → external Canadian bank account) via Scotia RTP.
 *
 * Money mechanics:
 *   1. Scotia RTP commit (through middleware) moves money out of the platform's shared
 *      corporate Scotia account to the external counterparty.
 *   2. The user's wallet is then debited in Fineract (withdraw) and a TRANSFER_OUT movement
 *      is recorded so balance reads + transaction history stay consistent.
 *   3. A double-entry GL posting (Dr Consumer Wallet / Cr Scotia RTP Clearing) is made to ledger-service.
 *
 * Fineract is the source of truth for balance; the local availableBalance is a projection.
 */
@Slf4j
@Service
public class InitiateExternalTransferService implements InitiateExternalTransferUseCase {

    private final WalletRepository walletRepository;
    private final ExternalFundTransferRepository transferRepository;
    private final WalletMovementRepository movementRepository;
    private final FineractSavingsPort fineractPort;
    private final ScotiaRtpPort scotiaRtpPort;
    private final LedgerPostingPort ledgerPostingPort;
    private final CreditWalletUseCase creditWalletUseCase;
    private final com.ksa.financing.wallet.application.support.TransactionLimitEnforcer limitEnforcer;
    private final String corporateAccount;
    private final String corporateName;
    private final String defaultCurrency;

    public InitiateExternalTransferService(
            WalletRepository walletRepository,
            ExternalFundTransferRepository transferRepository,
            WalletMovementRepository movementRepository,
            FineractSavingsPort fineractPort,
            ScotiaRtpPort scotiaRtpPort,
            LedgerPostingPort ledgerPostingPort,
            CreditWalletUseCase creditWalletUseCase,
            com.ksa.financing.wallet.application.support.TransactionLimitEnforcer limitEnforcer,
            @Value("${ksa.wallet.scotia.corporate-account:002-80150-0000000}") String corporateAccount,
            @Value("${ksa.wallet.scotia.corporate-name:KSA Islamic Financing Corp}") String corporateName,
            @Value("${ksa.wallet.scotia.default-currency:CAD}") String defaultCurrency) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.movementRepository = movementRepository;
        this.fineractPort = fineractPort;
        this.scotiaRtpPort = scotiaRtpPort;
        this.ledgerPostingPort = ledgerPostingPort;
        this.creditWalletUseCase = creditWalletUseCase;
        this.limitEnforcer = limitEnforcer;
        this.corporateAccount = corporateAccount;
        this.corporateName = corporateName;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    @Transactional
    public ExternalFundTransfer initiate(InitiateExternalTransferCommand command) {
        log.info("Initiating external transfer wallet={} customer={} amount={} key={}",
                command.sourceWalletId(), command.customerId(), command.amount(), command.idempotencyKey());

        // 1. Idempotency replay
        var existing = transferRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent replay returning externalTransferId={}", existing.get().getId());
            return existing.get();
        }

        // 2. Validate
        validateAmount(command.amount());
        if (command.counterpartyAccount() == null || command.counterpartyAccount().isBlank()) {
            throw new BusinessException("WALLET.EXT_TRANSFER.COUNTERPARTY_REQUIRED",
                    "counterpartyAccount is required");
        }

        // 3. Resolve + validate wallet
        Wallet wallet = resolveWallet(command);
        if (!wallet.getTenantId().equals(command.tenantId())) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "Wallet does not belong to tenant");
        }
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.Wallet.NOT_ACTIVE,
                    "Wallet is not active: " + wallet.getStatus());
        }
        if (wallet.getFineractSavingsAccountId() == null) {
            throw new BusinessException("WALLET.EXT_TRANSFER.NOT_FINERACT_LINKED",
                    "Wallet not linked to Fineract savings account");
        }

        String currency = command.currency() != null ? command.currency() : defaultCurrency;

        // 4. Insufficient funds check (Fineract = source of truth)
        FineractSavingsPort.SavingsAccountInfo info;
        try {
            info = fineractPort.getAccountInfo(wallet.getFineractSavingsAccountId());
        } catch (Exception ex) {
            log.error("Failed to fetch Fineract account info: {}", ex.getMessage());
            throw new BusinessException("WALLET.EXT_TRANSFER.FINERACT_UNAVAILABLE",
                    "Core banking unavailable, please retry");
        }
        if (info.availableBalance() == null || info.availableBalance().compareTo(command.amount()) < 0) {
            throw new BusinessException(ErrorCodes.Wallet.INSUFFICIENT_FUNDS,
                    "Insufficient available balance: have=" + info.availableBalance()
                            + " need=" + command.amount());
        }

        // 4b. Daily / monthly transaction-limit enforcement (cumulative spend vs wallet limits)
        limitEnforcer.enforce(wallet, command.amount());

        // 5. Persist transfer (audit + idempotency), status INITIATED
        ExternalFundTransfer transfer = newTransfer(command, wallet, currency);
        ExternalFundTransfer saved = transferRepository.save(transfer);

        // 6. Scotia RTP (options-inquiry + commit) — moves money via the corporate account
        var rtp = scotiaRtpPort.sendPayment(new ScotiaRtpPort.RtpPaymentRequest(
                command.amount(),
                currency,
                corporateName,
                corporateAccount,
                command.counterpartyName(),
                command.counterpartyAccount(),
                command.counterpartyEmail(),
                deriveMessageIdentification(saved),
                command.idempotencyKey()));

        saved.setScotiaPaymentId(rtp.paymentId());
        saved.setScotiaClearingRef(rtp.clearingReference());
        saved.setScotiaStatus(rtp.status());

        if (!rtp.success()) {
            saved.setStatus(ExternalTransferStatus.REJECTED);
            saved.setErrorCode(rtp.errorCode());
            saved.setErrorMessage(rtp.errorMessage());
            transferRepository.save(saved);
            throw new BusinessException(
                    rtp.errorCode() != null ? rtp.errorCode() : "SCOTIA.RTP.REJECTED",
                    rtp.errorMessage() != null ? rtp.errorMessage() : "Scotia RTP rejected the payment");
        }
        // Mark SUBMITTED in memory only — persisted once at COMPLETED. Saving here would
        // bump the row @Version, and the later credit's flush then makes the final save
        // hit a stale version (ObjectOptimisticLockingFailureException).
        saved.setStatus(ExternalTransferStatus.SUBMITTED);

        // 7. Debit the user wallet in Fineract + record local projection movement
        WalletMovement movement;
        try {
            fineractPort.withdraw(wallet.getFineractSavingsAccountId(), command.amount(),
                    "EXT:" + saved.getTransferNumber());
            movement = applyDebit(wallet, command.amount(), saved);
        } catch (Exception ex) {
            // RTP already moved money but local debit failed — leave SUBMITTED for reconciliation.
            log.error("Wallet debit failed after Scotia commit for transfer={}: {}",
                    saved.getTransferNumber(), ex.getMessage(), ex);
            saved.setStatus(ExternalTransferStatus.SUBMITTED);
            saved.setErrorCode("WALLET.EXT_TRANSFER.DEBIT_FAILED");
            saved.setErrorMessage("Scotia committed but wallet debit failed: " + ex.getMessage());
            return transferRepository.save(saved);
        }
        saved.setMovementId(movement.getId());

        // 8. GL posting (best-effort)
        String ledgerEntryId = ledgerPostingPort.postExternalTransfer(
                command.tenantId(), saved.getId(), saved.getTransferNumber(),
                LedgerPostingPort.Direction.OUTBOUND, command.amount(),
                command.idempotencyKey(), command.initiatorUserId());
        if (ledgerEntryId != null) {
            saved.setLedgerEntryId(parseUuidOrNull(ledgerEntryId));
        }

        // 8b. If the counterparty account number belongs to one of OUR wallets, mirror the
        //     credit here — Scotia is only the money rail; our system does both the
        //     subtraction (sender) and the addition (recipient) in the same operation.
        creditInternalRecipientIfAny(command, wallet, saved, currency);

        // 9. Complete
        saved.setStatus(ExternalTransferStatus.COMPLETED);
        saved.setCompletedAt(Instant.now());
        ExternalFundTransfer completed = transferRepository.save(saved);
        log.info("External transfer COMPLETED id={} scotiaPaymentId={} amount={}",
                completed.getId(), completed.getScotiaPaymentId(), completed.getAmount());
        return completed;
    }

    private Wallet resolveWallet(InitiateExternalTransferCommand command) {
        if (command.sourceWalletId() != null) {
            return walletRepository.findById(command.sourceWalletId())
                    .orElseThrow(() -> NotFoundException.forEntity("Wallet", command.sourceWalletId().toString()));
        }
        if (command.customerId() != null) {
            return walletRepository.findByCustomerId(command.tenantId(), command.customerId())
                    .orElseThrow(() -> NotFoundException.forEntity("Wallet", "customer=" + command.customerId()));
        }
        throw new BusinessException("WALLET.EXT_TRANSFER.SOURCE_REQUIRED",
                "sourceWalletId or customerId is required");
    }

    private ExternalFundTransfer newTransfer(InitiateExternalTransferCommand command, Wallet wallet, String currency) {
        UUID id = UUID.randomUUID();
        ExternalFundTransfer t = new ExternalFundTransfer();
        t.setId(id);
        t.setTenantId(command.tenantId());
        t.setTransferNumber("EXT-" + System.currentTimeMillis() + "-" + id.toString().substring(0, 8));
        t.setDirection(ExternalTransferDirection.OUTBOUND);
        t.setWalletId(wallet.getId());
        t.setCustomerId(wallet.getCustomerId());
        t.setAccountNumber(wallet.getAccountNumber());
        t.setCounterpartyName(command.counterpartyName());
        t.setCounterpartyAccount(command.counterpartyAccount());
        t.setCounterpartyEmail(command.counterpartyEmail());
        t.setCounterpartyBankCode(command.counterpartyBankCode());
        t.setAmount(command.amount());
        t.setFeeAmount(BigDecimal.ZERO);
        t.setCurrency(currency);
        t.setStatus(ExternalTransferStatus.INITIATED);
        t.setPurposeNote(command.purposeNote());
        t.setIdempotencyKey(command.idempotencyKey());
        t.setInitiatorUserId(command.initiatorUserId());
        t.setInitiatorIp(command.initiatorIp());
        t.setInitiatorDeviceId(command.initiatorDeviceId());
        t.setInitiatedAt(Instant.now());
        return t;
    }

    private WalletMovement applyDebit(Wallet wallet, BigDecimal amount, ExternalFundTransfer transfer) {
        BigDecimal before = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;
        BigDecimal after = before.subtract(amount);
        wallet.setAvailableBalance(after);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        WalletMovement m = new WalletMovement();
        m.setTenantId(transfer.getTenantId());
        m.setWalletId(wallet.getId());
        m.setMovementNumber("MOV" + System.currentTimeMillis() + "-XD");
        m.setMovementType(MovementType.DEBIT);
        m.setPurpose(TransactionPurpose.TRANSFER_OUT);
        m.setAmount(amount);
        m.setBalanceBefore(before);
        m.setBalanceAfter(after);
        m.setReferenceType("EXTERNAL_TRANSFER");
        m.setReferenceId(transfer.getId());
        m.setDescription("External transfer out " + transfer.getTransferNumber()
                + " to " + transfer.getCounterpartyAccount());
        m.setIdempotencyKey(transfer.getIdempotencyKey() + ":XDR");
        m.setCreatedAt(Instant.now());
        return movementRepository.save(m);
    }

    /**
     * When the counterparty account number is one of our own wallets, credit it so the
     * money "arrives" inside our system too (Scotia is only the transport). Best-effort:
     * a failure here is logged on the transfer but does not roll back the sender debit.
     */
    private void creditInternalRecipientIfAny(InitiateExternalTransferCommand command,
                                              Wallet sender, ExternalFundTransfer transfer, String currency) {
        var recipientOpt = walletRepository.findByAccountNumber(command.tenantId(), command.counterpartyAccount());
        if (recipientOpt.isEmpty() || recipientOpt.get().getId().equals(sender.getId())) {
            return; // external counterparty (or self) — sender debit only
        }
        Wallet recipient = recipientOpt.get();
        try {
            var credit = creditWalletUseCase.credit(new CreditWalletUseCase.CreditCommand(
                    command.tenantId(),
                    recipient.getCustomerId(),
                    command.amount(),
                    TransactionPurpose.TRANSFER_IN,
                    "EXTERNAL_TRANSFER",
                    transfer.getId(),
                    "External transfer in " + transfer.getTransferNumber()
                            + " from " + sender.getAccountNumber(),
                    command.idempotencyKey() + ":INCR"));
            transfer.setCounterpartyInternal(true);
            transfer.setCounterpartyWalletId(recipient.getId());
            transfer.setCounterpartyMovementId(credit.movementId());
            // Mirror GL for the credit leg (Dr Scotia RTP Clearing / Cr Consumer Wallet);
            // the clearing account nets to zero across the two legs.
            ledgerPostingPort.postExternalTransfer(
                    command.tenantId(), transfer.getId(), transfer.getTransferNumber() + "-IN",
                    LedgerPostingPort.Direction.INBOUND, command.amount(),
                    command.idempotencyKey() + ":INCR", command.initiatorUserId());
            log.info("Internal recipient credited transfer={} recipientWallet={} movement={}",
                    transfer.getTransferNumber(), recipient.getId(), credit.movementId());
        } catch (Exception ex) {
            log.error("Internal recipient credit FAILED transfer={} account={}: {}",
                    transfer.getTransferNumber(), command.counterpartyAccount(), ex.getMessage(), ex);
            transfer.setCounterpartyInternal(true);
            transfer.setCounterpartyWalletId(recipient.getId());
            transfer.setErrorCode("WALLET.EXT_TRANSFER.RECIPIENT_CREDIT_FAILED");
            transfer.setErrorMessage("Recipient credit failed: " + ex.getMessage());
        }
    }

    private String deriveMessageIdentification(ExternalFundTransfer transfer) {
        // Scotia expects a numeric-ish unique message id; derive a stable 10-digit value.
        long n = Math.abs((long) transfer.getId().hashCode()) % 10_000_000_000L;
        return String.valueOf(n);
    }

    private UUID parseUuidOrNull(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET.EXT_TRANSFER.AMOUNT_INVALID",
                    "Transfer amount must be positive");
        }
        if (amount.scale() > 6) {
            throw new BusinessException("WALLET.EXT_TRANSFER.AMOUNT_INVALID",
                    "Transfer amount exceeds allowed scale");
        }
    }
}
