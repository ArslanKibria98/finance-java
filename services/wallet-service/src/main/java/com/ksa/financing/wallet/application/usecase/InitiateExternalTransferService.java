package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.application.support.RecipientAccountResolver;
import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;
import com.ksa.financing.wallet.domain.model.ExternalTransferDirection;
import com.ksa.financing.wallet.domain.model.ExternalTransferStatus;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.InitiateExternalTransferUseCase;
import com.ksa.financing.wallet.domain.port.out.ExternalFundTransferRepository;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.ScotiaRtpPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound external fund transfer (user wallet → Canadian bank account / another platform wallet)
 * via Scotia RTP.
 *
 * Money mechanics — "settle owns it":
 *   The Scotia RTP commit goes through the middleware with the SENDER's wallet account as the
 *   debtor. The middleware's wallet-settlement callback ({@code /internal/wallets/settle}) then
 *   performs the actual money movement: debit the sender wallet (Fineract) + credit the creditor
 *   wallet when it is one of ours, post the double-entry GL legs, and emit FUNDS_SENT / FUNDS_RECEIVED
 *   notifications. This service therefore does NOT debit/credit or post GL itself — it only
 *   validates, enforces transaction limits, triggers the rail, and keeps a findable tracking row.
 */
@Slf4j
@Service
public class InitiateExternalTransferService implements InitiateExternalTransferUseCase {

    private final WalletRepository walletRepository;
    private final ExternalFundTransferRepository transferRepository;
    private final FineractSavingsPort fineractPort;
    private final ScotiaRtpPort scotiaRtpPort;
    private final com.ksa.financing.wallet.application.support.TransactionLimitEnforcer limitEnforcer;
    private final RecipientAccountResolver recipientAccountResolver;
    private final String corporateName;
    private final String defaultCurrency;

    public InitiateExternalTransferService(
            WalletRepository walletRepository,
            ExternalFundTransferRepository transferRepository,
            FineractSavingsPort fineractPort,
            ScotiaRtpPort scotiaRtpPort,
            com.ksa.financing.wallet.application.support.TransactionLimitEnforcer limitEnforcer,
            RecipientAccountResolver recipientAccountResolver,
            @Value("${ksa.wallet.scotia.corporate-name:KSA Islamic Financing Corp}") String corporateName,
            @Value("${ksa.wallet.scotia.default-currency:CAD}") String defaultCurrency) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.fineractPort = fineractPort;
        this.scotiaRtpPort = scotiaRtpPort;
        this.limitEnforcer = limitEnforcer;
        this.recipientAccountResolver = recipientAccountResolver;
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

        // 2b. counterpartyAccount carries the recipient's MOBILE number — resolve it to the
        //     recipient's virtual wallet account number before it flows into Scotia RTP and the
        //     settlement callback. Rejects if no matching customer/wallet. An account number, if
        //     sent, passes through unchanged.
        var resolvedRecipient = recipientAccountResolver.resolve(
                command.tenantId(), command.counterpartyAccount());
        command = new InitiateExternalTransferUseCase.InitiateExternalTransferCommand(
                command.tenantId(),
                command.sourceWalletId(),
                command.customerId(),
                (command.counterpartyName() == null || command.counterpartyName().isBlank())
                        ? resolvedRecipient.name() : command.counterpartyName(),
                resolvedRecipient.accountNumber(),
                command.counterpartyEmail(),
                command.counterpartyBankCode(),
                command.amount(),
                command.currency(),
                command.purposeNote(),
                command.idempotencyKey(),
                command.initiatorUserId(),
                command.initiatorIp(),
                command.initiatorDeviceId());

        // 3. Resolve + validate sender wallet
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
        if (wallet.getAccountNumber() == null || wallet.getAccountNumber().isBlank()) {
            throw new BusinessException("WALLET.EXT_TRANSFER.NO_ACCOUNT_NUMBER",
                    "Sender wallet has no account number");
        }

        String currency = command.currency() != null ? command.currency() : defaultCurrency;

        // 4. Insufficient funds check (Fineract = source of truth) — fast fail before the rail.
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

        // 4b. Daily / monthly transaction-limit enforcement (cumulative spend vs wallet limits).
        //     The settlement callback does NOT enforce limits — this is the only gate for them.
        limitEnforcer.enforce(wallet, command.amount());

        // 5. Persist tracking row (audit + idempotency + GET-by-id), status INITIATED.
        ExternalFundTransfer transfer = newTransfer(command, wallet, currency);
        ExternalFundTransfer saved = transferRepository.save(transfer);

        // 6. Scotia RTP (options-inquiry + commit). The debtor is the SENDER's wallet account, so the
        //    middleware settlement callback debits the sender and credits the recipient. messageId is
        //    stable so a retry is idempotent on the rail.
        var rtp = scotiaRtpPort.sendPayment(new ScotiaRtpPort.RtpPaymentRequest(
                command.amount(),
                currency,
                wallet.getMaskedName() != null ? wallet.getMaskedName() : corporateName,
                wallet.getAccountNumber(),
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

        // 7. The settlement callback (triggered by the commit) has moved the money + posted GL +
        //    emitted notifications. Mark our tracking row COMPLETED and reflect whether the
        //    counterparty is one of our wallets (for the response).
        walletRepository.findByAccountNumber(command.tenantId(), command.counterpartyAccount())
                .filter(r -> !r.getId().equals(wallet.getId()))
                .ifPresent(r -> {
                    saved.setCounterpartyInternal(true);
                    saved.setCounterpartyWalletId(r.getId());
                });
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

    private String deriveMessageIdentification(ExternalFundTransfer transfer) {
        // Scotia expects a numeric-ish unique message id; derive a stable 10-digit value.
        long n = Math.abs((long) transfer.getId().hashCode()) % 10_000_000_000L;
        return String.valueOf(n);
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
