package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;
import com.ksa.financing.wallet.domain.model.ExternalTransferDirection;
import com.ksa.financing.wallet.domain.model.ExternalTransferStatus;
import com.ksa.financing.wallet.domain.model.TransactionPurpose;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.CreditWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.DebitWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.GetBalanceUseCase;
import com.ksa.financing.wallet.domain.port.in.SettleExternalPaymentUseCase;
import com.ksa.financing.wallet.domain.port.out.ExternalFundTransferRepository;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.LedgerPostingPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Mirrors a Scotia-settled payment into our wallets: debits the debtor account and credits
 * the creditor account (whichever are our own wallets). Scotia is only the rail — the
 * addition + subtraction happen here. Idempotent + best-effort per leg.
 */
@Slf4j
@Service
public class SettleExternalPaymentService implements SettleExternalPaymentUseCase {

    private final WalletRepository walletRepository;
    private final DebitWalletUseCase debitWalletUseCase;
    private final CreditWalletUseCase creditWalletUseCase;
    private final ExternalFundTransferRepository transferRepository;
    private final LedgerPostingPort ledgerPostingPort;
    private final FineractSavingsPort fineractPort;
    private final GetBalanceUseCase getBalanceUseCase;
    private final String defaultCurrency;

    public SettleExternalPaymentService(
            WalletRepository walletRepository,
            DebitWalletUseCase debitWalletUseCase,
            CreditWalletUseCase creditWalletUseCase,
            ExternalFundTransferRepository transferRepository,
            LedgerPostingPort ledgerPostingPort,
            FineractSavingsPort fineractPort,
            GetBalanceUseCase getBalanceUseCase,
            @Value("${ksa.wallet.scotia.default-currency:CAD}") String defaultCurrency) {
        this.walletRepository = walletRepository;
        this.debitWalletUseCase = debitWalletUseCase;
        this.creditWalletUseCase = creditWalletUseCase;
        this.transferRepository = transferRepository;
        this.ledgerPostingPort = ledgerPostingPort;
        this.fineractPort = fineractPort;
        this.getBalanceUseCase = getBalanceUseCase;
        this.defaultCurrency = defaultCurrency;
    }

    /**
     * Resolve the debtor wallet: by account number if given, else by the JWT-derived mobile.
     * Throws DEBTOR_REQUIRED if neither is present, DEBTOR_NOT_INTERNAL if it isn't our wallet.
     */
    private Wallet resolveDebtor(SettleCommand command) {
        if (command.debtorAccount() != null && !command.debtorAccount().isBlank()) {
            return walletRepository.findByAccountNumber(command.tenantId(), command.debtorAccount())
                    .orElseThrow(() -> new BusinessException("WALLET.SETTLE.DEBTOR_NOT_INTERNAL",
                            "Debtor account " + command.debtorAccount() + " is not one of our wallets"));
        }
        if (command.debtorMobile() != null && !command.debtorMobile().isBlank()) {
            try {
                return getBalanceUseCase.getByMobile(command.tenantId(), command.debtorMobile());
            } catch (Exception e) {
                throw new BusinessException("WALLET.SETTLE.DEBTOR_NOT_INTERNAL",
                        "No wallet found for the authenticated user (mobile)");
            }
        }
        throw new BusinessException("WALLET.SETTLE.DEBTOR_REQUIRED",
                "Settlement requires a debtor — provide debtorAccount or authenticate (mobile)");
    }

    @Override
    public void validate(SettleCommand command) {
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET.SETTLE.AMOUNT_INVALID", "Settlement amount must be positive");
        }
        Wallet debtor = resolveDebtor(command);
        if (debtor.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.Wallet.NOT_ACTIVE,
                    "Debtor wallet is not active: " + debtor.getStatus());
        }
        if (debtor.getFineractSavingsAccountId() == null) {
            throw new BusinessException("WALLET.SETTLE.NOT_FINERACT_LINKED",
                    "Debtor wallet is not linked to Fineract");
        }
        BigDecimal available;
        try {
            available = fineractPort.getAccountInfo(debtor.getFineractSavingsAccountId()).availableBalance();
        } catch (Exception ex) {
            throw new BusinessException("WALLET.SETTLE.FINERACT_UNAVAILABLE",
                    "Core banking unavailable, please retry");
        }
        if (available == null || available.compareTo(command.amount()) < 0) {
            throw new BusinessException(ErrorCodes.Wallet.INSUFFICIENT_FUNDS,
                    "Insufficient balance in debtor account " + command.debtorAccount()
                            + ": have=" + available + " need=" + command.amount());
        }
    }

    @Override
    @Transactional
    public SettleResult settle(SettleCommand command) {
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET.SETTLE.AMOUNT_INVALID", "Settlement amount must be positive");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new BusinessException("WALLET.SETTLE.KEY_REQUIRED", "idempotencyKey is required");
        }
        String currency = command.currency() != null ? command.currency() : defaultCurrency;
        UUID tenantId = command.tenantId();

        log.info("Settle Scotia payment debtor={} creditor={} amount={} key={}",
                command.debtorAccount(), command.creditorAccount(), command.amount(), command.idempotencyKey());

        // SAFETY: a settlement must have a funded source — resolve the debtor (by account or the
        // authenticated user's mobile) and debit it first; the debit enforces insufficient-funds.
        Wallet debtorWallet = resolveDebtor(command);

        // ---- Debtor leg (subtraction) — internal funded debtor (insufficient funds → 422) ----
        boolean debtorDebited = false;
        UUID debtorWalletId = null, debtorMovementId = null;
        {
            Wallet w = debtorWallet;
            String key = command.idempotencyKey() + ":OUT";
            var existing = transferRepository.findByIdempotencyKey(tenantId, key);
            if (existing.isPresent()) {
                debtorDebited = true; debtorWalletId = w.getId(); debtorMovementId = existing.get().getMovementId();
            } else {
                var debit = debitWalletUseCase.debit(new DebitWalletUseCase.DebitCommand(
                        tenantId, w.getCustomerId(), command.amount(), TransactionPurpose.TRANSFER_OUT,
                        "EXTERNAL_SETTLEMENT", null,
                        "Scotia settlement out to " + command.creditorAccount()
                                + (command.reference() != null ? " (" + command.reference() + ")" : ""),
                        key));
                var row = recordRow(tenantId, ExternalTransferDirection.OUTBOUND, w, command.creditorAccount(),
                        command.amount(), currency, command.reference(), debit.movementId(), key);
                String ledgerId = ledgerPostingPort.postExternalTransfer(tenantId, row.getId(),
                        row.getTransferNumber(), LedgerPostingPort.Direction.OUTBOUND, command.amount(), key, null);
                if (ledgerId != null) row.setLedgerEntryId(parseUuid(ledgerId));
                transferRepository.save(row);
                debtorDebited = true; debtorWalletId = w.getId(); debtorMovementId = debit.movementId();
            }
        }

        // ---- Creditor leg (addition) — only if the creditor account is one of ours ----
        boolean creditorCredited = false;
        UUID creditorWalletId = null, creditorMovementId = null;
        Optional<Wallet> creditor = command.creditorAccount() == null ? Optional.empty()
                : walletRepository.findByAccountNumber(tenantId, command.creditorAccount());
        if (creditor.isPresent()) {
            Wallet w = creditor.get();
            String key = command.idempotencyKey() + ":IN";
            var existing = transferRepository.findByIdempotencyKey(tenantId, key);
            if (existing.isPresent()) {
                creditorCredited = true; creditorWalletId = w.getId(); creditorMovementId = existing.get().getMovementId();
            } else {
                var credit = creditWalletUseCase.credit(new CreditWalletUseCase.CreditCommand(
                        tenantId, w.getCustomerId(), command.amount(), TransactionPurpose.TRANSFER_IN,
                        "EXTERNAL_SETTLEMENT", null,
                        "Scotia settlement in from " + command.debtorAccount()
                                + (command.reference() != null ? " (" + command.reference() + ")" : ""),
                        key));
                var row = recordRow(tenantId, ExternalTransferDirection.INBOUND, w, command.debtorAccount(),
                        command.amount(), currency, command.reference(), credit.movementId(), key);
                String ledgerId = ledgerPostingPort.postExternalTransfer(tenantId, row.getId(),
                        row.getTransferNumber(), LedgerPostingPort.Direction.INBOUND, command.amount(), key, null);
                if (ledgerId != null) row.setLedgerEntryId(parseUuid(ledgerId));
                transferRepository.save(row);
                creditorCredited = true; creditorWalletId = w.getId(); creditorMovementId = credit.movementId();
            }
        } else {
            log.info("Creditor account {} is external — skipping credit", command.creditorAccount());
        }

        log.info("Settlement done debtorDebited={} creditorCredited={} amount={}",
                debtorDebited, creditorCredited, command.amount());
        return new SettleResult(debtorDebited, debtorWalletId, debtorMovementId,
                creditorCredited, creditorWalletId, creditorMovementId, command.amount(), currency);
    }

    private ExternalFundTransfer recordRow(UUID tenantId, ExternalTransferDirection direction, Wallet wallet,
                                           String counterpartyAccount, BigDecimal amount, String currency,
                                           String reference, UUID movementId, String idempotencyKey) {
        UUID id = UUID.randomUUID();
        ExternalFundTransfer t = new ExternalFundTransfer();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setTransferNumber("STL-" + System.currentTimeMillis() + "-" + id.toString().substring(0, 8));
        t.setDirection(direction);
        t.setWalletId(wallet.getId());
        t.setCustomerId(wallet.getCustomerId());
        t.setAccountNumber(wallet.getAccountNumber());
        t.setCounterpartyAccount(counterpartyAccount);
        t.setAmount(amount);
        t.setFeeAmount(BigDecimal.ZERO);
        t.setCurrency(currency);
        t.setStatus(ExternalTransferStatus.COMPLETED);
        t.setPurposeNote(reference);
        t.setMovementId(movementId);
        t.setIdempotencyKey(idempotencyKey);
        t.setInitiatedAt(Instant.now());
        t.setCompletedAt(Instant.now());
        return t;
    }

    private UUID parseUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }
}
