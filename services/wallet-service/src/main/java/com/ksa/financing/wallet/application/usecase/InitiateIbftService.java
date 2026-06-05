package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.application.support.TransactionLimitEnforcer;
import com.ksa.financing.wallet.domain.model.IbftBeneficiary;
import com.ksa.financing.wallet.domain.model.IbftStatus;
import com.ksa.financing.wallet.domain.model.IbftTransaction;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.InitiateIbftUseCase;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.IbftBeneficiaryRepository;
import com.ksa.financing.wallet.domain.port.out.IbftTransactionRepository;
import com.ksa.financing.wallet.domain.port.out.ScotiaEftPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * IBFT initiate: pre-checks → HOLD funds → Scotia EFT create + submit → SUBMITTED.
 * Settlement is async (reconciliation cron finalizes/releases).
 */
@Slf4j
@Service
public class InitiateIbftService implements InitiateIbftUseCase {

    private final WalletRepository walletRepository;
    private final IbftTransactionRepository ibftRepository;
    private final IbftBeneficiaryRepository beneficiaryRepository;
    private final FineractSavingsPort fineractPort;
    private final ScotiaEftPort scotiaEftPort;
    private final WalletHoldService holdService;
    private final TransactionLimitEnforcer limitEnforcer;
    private final String corporateAccount;
    private final String defaultCurrency;

    @org.springframework.beans.factory.annotation.Value("${ksa.wallet.ibft.validate-one-time:true}")
    private boolean validateOneTime;

    public InitiateIbftService(WalletRepository walletRepository,
                               IbftTransactionRepository ibftRepository,
                               IbftBeneficiaryRepository beneficiaryRepository,
                               FineractSavingsPort fineractPort,
                               ScotiaEftPort scotiaEftPort,
                               WalletHoldService holdService,
                               TransactionLimitEnforcer limitEnforcer,
                               @Value("${ksa.wallet.scotia.corporate-account:002-80150-0000000}") String corporateAccount,
                               @Value("${ksa.wallet.scotia.default-currency:CAD}") String defaultCurrency) {
        this.walletRepository = walletRepository;
        this.ibftRepository = ibftRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.fineractPort = fineractPort;
        this.scotiaEftPort = scotiaEftPort;
        this.holdService = holdService;
        this.limitEnforcer = limitEnforcer;
        this.corporateAccount = corporateAccount;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    @Transactional
    public IbftTransaction initiate(InitiateIbftCommand c) {
        // 1. Idempotency replay
        var existing = ibftRepository.findByIdempotencyKey(c.tenantId(), c.idempotencyKey());
        if (existing.isPresent()) return existing.get();

        // 2. Validate amount
        if (c.amount() == null || c.amount().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("IBFT.AMOUNT_INVALID", "Amount must be positive");

        // 3. Wallet
        Wallet wallet = walletRepository.findById(c.walletId())
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", String.valueOf(c.walletId())));
        if (!wallet.getTenantId().equals(c.tenantId()))
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Wallet does not belong to tenant");
        if (wallet.getStatus() != WalletStatus.ACTIVE)
            throw new BusinessException(ErrorCodes.Wallet.NOT_ACTIVE, "Wallet is not active: " + wallet.getStatus());
        if (wallet.getFineractSavingsAccountId() == null)
            throw new BusinessException("IBFT.NOT_FINERACT_LINKED", "Wallet not linked to Fineract");

        // 4. Resolve creditor — a saved beneficiary OR a one-time (ad-hoc) payee
        Creditor creditor = resolveCreditor(c, wallet);

        String currency = c.currency() != null ? c.currency() : defaultCurrency;

        // 5. Insufficient funds (Fineract source of truth)
        FineractSavingsPort.SavingsAccountInfo info;
        try {
            info = fineractPort.getAccountInfo(wallet.getFineractSavingsAccountId());
        } catch (Exception ex) {
            throw new BusinessException("IBFT.FINERACT_UNAVAILABLE", "Core banking unavailable, please retry");
        }
        if (info.availableBalance() == null || info.availableBalance().compareTo(c.amount()) < 0)
            throw new BusinessException(ErrorCodes.Wallet.INSUFFICIENT_FUNDS,
                    "Insufficient available balance: have=" + info.availableBalance() + " need=" + c.amount());

        // 6. Daily/monthly limits
        limitEnforcer.enforce(wallet, c.amount());

        // 7. Persist INITIATED
        IbftTransaction tx = newTransaction(c, wallet, creditor, currency);
        IbftTransaction saved = ibftRepository.save(tx);

        // 8. HOLD funds (Fineract block + reserve + movement + GL)
        WalletHoldService.HoldResult hold;
        try {
            hold = holdService.placeHold(wallet, c.amount(), saved.getId(), saved.getIbftNumber(), c.idempotencyKey());
        } catch (Exception ex) {
            log.error("IBFT hold failed for {}: {}", saved.getIbftNumber(), ex.getMessage(), ex);
            throw new BusinessException("IBFT.HOLD_FAILED", "Could not hold funds: " + ex.getMessage());
        }
        // HELD set in memory only — persisted once at the end (avoids @Version conflict from the
        // hold's intermediate flush; same 2-save pattern as external transfers).
        saved.setFineractHoldTxnId(hold.fineractHoldTxnId());
        saved.setHoldMovementId(hold.holdMovementId());
        saved.setLedgerEntryId(hold.ledgerEntryId());
        saved.setStatus(IbftStatus.HELD);

        // 9. Scotia EFT create
        var create = scotiaEftPort.createPayment(new ScotiaEftPort.EftPaymentRequest(
                c.amount(), currency, null, corporateAccount,
                creditor.name(), saved.getCreditorAccount(), saved.getEndToEndId(), saved.getIbftNumber()));
        if (!create.success()) {
            return failAndRelease(wallet, saved, create.errorCode(), create.errorMessage());
        }
        saved.setScotiaSubmissionId(create.submissionId());
        saved.setScotiaPaymentId(create.paymentId());
        saved.setScotiaStatus(create.status());

        // 10. Scotia EFT submit
        var submit = scotiaEftPort.submit(create.submissionId(), c.idempotencyKey());
        if (!submit.success()) {
            return failAndRelease(wallet, saved, submit.errorCode(), submit.errorMessage());
        }
        saved.setScotiaStatus(submit.status());
        saved.setStatus(IbftStatus.SUBMITTED);
        saved.setSubmittedAt(Instant.now());
        IbftTransaction submitted = ibftRepository.save(saved);
        log.info("IBFT SUBMITTED id={} submissionId={} amount={}",
                submitted.getId(), submitted.getScotiaSubmissionId(), submitted.getAmount());
        return submitted;
    }

    /** Release the hold and persist FAILED (does not throw — caller sees status FAILED). */
    private IbftTransaction failAndRelease(Wallet wallet, IbftTransaction tx, String code, String message) {
        log.warn("IBFT {} failed at Scotia: {} - {}; releasing hold", tx.getIbftNumber(), code, message);
        try {
            holdService.releaseHold(wallet, tx);
        } catch (Exception ex) {
            log.error("Hold release failed for {}: {}", tx.getIbftNumber(), ex.getMessage(), ex);
        }
        tx.setStatus(IbftStatus.FAILED);
        tx.setErrorCode(code);
        tx.setErrorMessage(message);
        tx.setFailedAt(Instant.now());
        return ibftRepository.save(tx);
    }

    /** A creditor (payee) — resolved from a saved beneficiary or one-time inline details. */
    private record Creditor(UUID beneficiaryId, String institution, String transit, String account,
                            String name, String bankName, boolean oneTime) {}

    private Creditor resolveCreditor(InitiateIbftCommand c, Wallet wallet) {
        // Saved beneficiary path
        if (c.beneficiaryId() != null) {
            IbftBeneficiary ben = beneficiaryRepository.findByIdAndTenantId(c.beneficiaryId(), c.tenantId())
                    .orElseThrow(() -> new BusinessException("IBFT.BENEFICIARY.NOT_FOUND",
                            "Beneficiary not found: " + c.beneficiaryId()));
            if (!ben.isActive())
                throw new BusinessException("IBFT.BENEFICIARY.INACTIVE", "Beneficiary is not active");
            if (!ben.getCustomerId().equals(wallet.getCustomerId()))
                throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Beneficiary does not belong to you");
            return new Creditor(ben.getId(), ben.getInstitutionNumber(), ben.getTransit(),
                    ben.getAccountNumber(), ben.getBeneficiaryName(), ben.getBankName(), false);
        }
        // One-time (ad-hoc) payee path — inline details required + format-checked + Scotia-validated
        String inst = c.institutionNumber(), tr = c.transit(), acc = c.accountNumber(), name = c.beneficiaryName();
        if (badDigits(inst, 3, 4) || badDigits(tr, 5, 5) || badDigits(acc, 5, 20) || name == null || name.isBlank())
            throw new BusinessException("IBFT.PAYEE.INVALID",
                    "Provide beneficiaryId, or one-time payee: beneficiaryName + institutionNumber(3-4) + transit(5) + accountNumber(5-20) digits");
        if (validateOneTime) {
            var v = scotiaEftPort.validateAccount(inst, tr, acc, name);
            if (!v.valid())
                throw new BusinessException("IBFT.PAYEE.VALIDATION_FAILED",
                        "Scotia could not validate the payee account (" + v.status() + ")");
        }
        return new Creditor(null, inst, tr, acc, name.trim(), c.bankName(), true);
    }

    private static boolean badDigits(String s, int min, int max) {
        return s == null || !s.matches("^[0-9]{" + min + "," + max + "}$");
    }

    private IbftTransaction newTransaction(InitiateIbftCommand c, Wallet wallet, Creditor creditor, String currency) {
        UUID id = UUID.randomUUID();
        IbftTransaction t = new IbftTransaction();
        t.setId(id);
        t.setTenantId(c.tenantId());
        t.setIbftNumber("IBFT-" + System.currentTimeMillis() + "-" + id.toString().substring(0, 8));
        t.setCustomerId(wallet.getCustomerId());
        t.setWalletId(wallet.getId());
        t.setBeneficiaryId(creditor.beneficiaryId());
        t.setOneTime(creditor.oneTime());
        t.setDebtorCorporateAccount(corporateAccount);
        t.setCreditorAccount(creditor.institution() + "-" + creditor.transit() + "-" + creditor.account());
        t.setCreditorInstitution(creditor.institution());
        t.setCreditorTransit(creditor.transit());
        t.setCreditorAccountNo(creditor.account());
        t.setCreditorName(creditor.name());
        t.setAmount(c.amount());
        t.setFeeAmount(BigDecimal.ZERO);
        t.setCurrency(currency);
        t.setStatus(IbftStatus.INITIATED);
        t.setPurposeNote(c.purposeNote());
        t.setEndToEndId(String.valueOf(Math.abs((long) id.hashCode()) % 10_000_000_000L));
        t.setIdempotencyKey(c.idempotencyKey());
        t.setInitiatorUserId(c.initiatorUserId());
        t.setInitiatorIp(c.initiatorIp());
        t.setInitiatorDeviceId(c.initiatorDeviceId());
        t.setInitiatedAt(Instant.now());
        return t;
    }
}
