package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.iban.IbanValidator;
import com.ksa.financing.wallet.domain.iso.ChargeBearerType1Code;
import com.ksa.financing.wallet.domain.iso.ExternalPurpose1Code;
import com.ksa.financing.wallet.domain.iso.ServiceLevel;
import com.ksa.financing.wallet.domain.model.IbanBeneficiary;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.model.WithdrawalChannel;
import com.ksa.financing.wallet.domain.model.WithdrawalStatus;
import com.ksa.financing.wallet.domain.port.in.InitiateWithdrawalUseCase;
import com.ksa.financing.wallet.domain.port.out.BankRailsPort;
import com.ksa.financing.wallet.domain.port.out.EventPublisherPort;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.IbanBeneficiaryRepository;
import com.ksa.financing.wallet.domain.port.out.PaymentScreeningPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.domain.port.out.WalletWithdrawalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Withdrawal use case (cash-out from wallet to external bank IBAN).
 * <p>
 * SAGA (sync, except settlement which can be deferred to Temporal in production):
 *   1. Idempotency replay
 *   2. Resolve destination (beneficiary OR ad-hoc)
 *   3. IBAN MOD-97 + format validation (ISO 13616)
 *   4. ISO purpose-code mandatory check (>= 5,000 SAR)
 *   5. Wallet status + currency + Fineract linkage
 *   6. Read Fineract balance
 *   7. Limits check
 *   8. AML / sanctions / PEP screening (pre-debit)         ⭐
 *        PASS              → continue
 *        HOLD_FOR_REVIEW   → status=HELD_AML, halt SAGA, emit event for ops
 *        REJECT            → 422 with screening error code, no debit
 *   9. Persist VALIDATED + screening result + UETR + endToEndId
 *  10. Debit Fineract savings (status=DEBITED) — point of no return
 *  11. Submit pacs.008-equivalent to bank rails
 *  12. On bank success → COMPLETED; on rejection after debit → COMPENSATED (refund)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateWithdrawalService implements InitiateWithdrawalUseCase {

    private static final BigDecimal PURPOSE_CODE_THRESHOLD = new BigDecimal("5000");

    private final WalletRepository walletRepository;
    private final WalletWithdrawalRepository withdrawalRepository;
    private final IbanBeneficiaryRepository beneficiaryRepository;
    private final FineractSavingsPort fineractPort;
    private final BankRailsPort bankRailsPort;
    private final PaymentScreeningPort screeningPort;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;
    private final com.ksa.financing.wallet.application.support.TransactionLimitEnforcer limitEnforcer;

    @Override
    @Transactional
    public WalletWithdrawal initiate(InitiateWithdrawalCommand command) {
        log.info("Initiating withdrawal walletId={} amount={} purpose={} key={}",
                command.sourceWalletId(), command.amount(),
                command.purposeCode(), command.idempotencyKey());

        // 1. Idempotency replay
        var replay = withdrawalRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (replay.isPresent()) {
            log.info("Idempotent replay returning withdrawalId={}", replay.get().getId());
            return replay.get();
        }

        // 2. Resolve destination + 3. IBAN MOD-97 validation
        ResolvedDestination dest = resolveDestination(command);

        // Validate amount
        validateAmount(command.amount());

        // 4. ISO purpose-code mandatory check
        ExternalPurpose1Code purposeCode = command.purposeCode();
        if (command.amount().compareTo(PURPOSE_CODE_THRESHOLD) >= 0) {
            if (purposeCode == null || purposeCode == ExternalPurpose1Code.OTHR) {
                throw new BusinessException("WALLET.WITHDRAWAL.PURPOSE_CODE_REQUIRED",
                        "ISO purpose code required for transfers >= "
                                + PURPOSE_CODE_THRESHOLD + " SAR");
            }
        }

        // 5. Load source wallet & validate tenant + status
        Wallet source = walletRepository.findById(command.sourceWalletId())
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", command.sourceWalletId().toString()));
        if (!source.getTenantId().equals(command.tenantId())) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "Source wallet does not belong to tenant");
        }
        if (source.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.Wallet.NOT_ACTIVE,
                    "Source wallet is not active: " + source.getStatus());
        }

        // Currency check
        String currency = command.currency() != null ? command.currency() : source.getCurrency();
        if (!source.getCurrency().equalsIgnoreCase(currency)) {
            throw new BusinessException("WALLET.WITHDRAWAL.CURRENCY_MISMATCH",
                    "Currency mismatch: wallet=" + source.getCurrency() + " request=" + currency);
        }

        // Fineract linkage check
        if (source.getFineractSavingsAccountId() == null) {
            throw new BusinessException("WALLET.WITHDRAWAL.NOT_FINERACT_LINKED",
                    "Wallet not linked to Fineract savings account");
        }

        // 6. Read Fineract balance + check sufficiency
        BigDecimal fee = computeFee(command.amount(), parseChannel(command.channel()));
        BigDecimal totalDebit = command.amount().add(fee);

        FineractSavingsPort.SavingsAccountInfo srcInfo;
        try {
            srcInfo = fineractPort.getAccountInfo(source.getFineractSavingsAccountId());
        } catch (Exception ex) {
            log.error("Fineract account info fetch failed: {}", ex.getMessage());
            throw new BusinessException("WALLET.WITHDRAWAL.FINERACT_UNAVAILABLE",
                    "Core banking unavailable, please retry");
        }
        if (srcInfo.availableBalance() == null
                || srcInfo.availableBalance().compareTo(totalDebit) < 0) {
            throw new BusinessException(ErrorCodes.Wallet.INSUFFICIENT_FUNDS,
                    "Insufficient available balance: have=" + srcInfo.availableBalance()
                            + " need=" + totalDebit);
        }

        // 7. Limits check
        validateLimits(source, command.amount());

        // 8. AML / sanctions / PEP screening — BEFORE debit
        PaymentScreeningPort.ScreeningResult screening;
        try {
            screening = screeningPort.screen(new PaymentScreeningPort.ScreeningRequest(
                    command.tenantId(), source.getCustomerId(),
                    dest.beneficiaryName, dest.iban, dest.country,
                    command.amount(), currency, purposeCode));
        } catch (Exception ex) {
            log.error("AML screening unavailable — failing closed: {}", ex.getMessage());
            throw new BusinessException("WALLET.WITHDRAWAL.SCREENING_UNAVAILABLE",
                    "Payment screening service unavailable — please retry");
        }
        log.info("AML screening decision={} score={} ref={}",
                screening.decision(), screening.riskScore(), screening.screeningRef());

        // 9. Persist withdrawal
        UUID withdrawalId = UUID.randomUUID();
        UUID uetr = UUID.randomUUID();
        String withdrawalNumber = "WDR-" + System.currentTimeMillis()
                + "-" + withdrawalId.toString().substring(0, 8);
        String endToEndId = "E2E-" + withdrawalId.toString().substring(0, 8) + "-"
                + System.currentTimeMillis();
        String instructionId = "INSTR-" + withdrawalNumber;

        WalletWithdrawal w = new WalletWithdrawal();
        w.setId(withdrawalId);
        w.setTenantId(command.tenantId());
        w.setWithdrawalNumber(withdrawalNumber);
        w.setSourceWalletId(source.getId());
        w.setSourceCustomerId(source.getCustomerId());
        w.setChannel(parseChannel(command.channel()));
        w.setDestinationIban(dest.iban);
        w.setDestinationBankCode(dest.bankCode);
        w.setDestinationBankName(dest.bankName);
        w.setDestinationCountry(dest.country);
        w.setBeneficiaryName(dest.beneficiaryName);
        w.setBeneficiaryId(dest.beneficiaryId);
        w.setAmount(command.amount());
        w.setFeeAmount(fee);
        w.setTotalDebit(totalDebit);
        w.setCurrency(currency);
        w.setPurposeNote(command.purposeNote());
        w.setPurposeCode(purposeCode);
        w.setChargeBearer(command.chargeBearer() != null ? command.chargeBearer() : ChargeBearerType1Code.DEBT);
        w.setServiceLevel(command.serviceLevel() != null ? command.serviceLevel() : ServiceLevel.NURG);
        w.setEndToEndId(endToEndId);
        w.setUetr(uetr);
        w.setInstructionId(instructionId);
        w.setIdempotencyKey(command.idempotencyKey());
        w.setInitiatorUserId(command.initiatorUserId());
        w.setInitiatorIp(command.initiatorIp());
        w.setInitiatorDeviceId(command.initiatorDeviceId());
        w.setInitiatedAt(Instant.now());
        // Screening
        w.setScreeningRef(screening.screeningRef());
        w.setScreeningDecision(screening.decision().name());
        w.setScreeningScore(screening.riskScore());
        w.setScreeningMatchesJson(serializeMatches(screening));
        w.setScreenedAt(screening.screenedAt());
        w.setEddRequired(screening.requiresEdd());

        // 8b. Branch on screening decision
        if (screening.decision() == PaymentScreeningPort.Decision.REJECT) {
            w.setStatus(WithdrawalStatus.FAILED);
            w.setErrorCode("WALLET.WITHDRAWAL.AML_REJECTED");
            w.setErrorMessage("Payment rejected by sanctions/AML screening");
            w.setFailedAt(Instant.now());
            WalletWithdrawal rejected = withdrawalRepository.save(w);
            try { eventPublisher.publishWithdrawalFailed(rejected); } catch (Exception ignore) { }
            throw new BusinessException("WALLET.WITHDRAWAL.AML_REJECTED",
                    "Payment cannot proceed — screening REJECT");
        }
        if (screening.decision() == PaymentScreeningPort.Decision.HOLD_FOR_REVIEW) {
            w.setStatus(WithdrawalStatus.HELD_AML);
            WalletWithdrawal held = withdrawalRepository.save(w);
            try { eventPublisher.publishWithdrawalInitiated(held); } catch (Exception ignore) { }
            log.warn("Withdrawal HELD_AML withdrawalId={} ref={} — awaiting compliance review",
                    held.getId(), screening.screeningRef());
            return held;   // 202 ACCEPTED — customer is informed via response status
        }

        // PASS — proceed
        w.setStatus(WithdrawalStatus.VALIDATED);
        WalletWithdrawal saved = withdrawalRepository.save(w);

        try { eventPublisher.publishWithdrawalInitiated(saved); }
        catch (Exception ex) { log.warn("Event publish (initiated) failed: {}", ex.getMessage()); }

        return executeDebitAndSubmit(saved, source, srcInfo);
    }

    /**
     * Steps 10-12: debit Fineract → submit bank rails → completed/compensated.
     * Public-package so it can be reused by AdminReleaseWithdrawalService.
     */
    WalletWithdrawal executeDebitAndSubmit(WalletWithdrawal saved, Wallet source,
                                           FineractSavingsPort.SavingsAccountInfo srcInfo) {
        // 10. Debit Fineract
        Long fineractTxnId;
        try {
            fineractTxnId = fineractPort.withdraw(
                    source.getFineractSavingsAccountId(),
                    saved.getTotalDebit(),
                    "WDR:" + saved.getWithdrawalNumber());
        } catch (Exception ex) {
            log.error("Fineract debit failed withdrawalId={}: {}", saved.getId(), ex.getMessage(), ex);
            throw markFailed(saved, "WALLET.WITHDRAWAL.DEBIT_FAILED",
                    "Failed to debit wallet: " + ex.getMessage());
        }
        saved.setFineractDebitTxnId(fineractTxnId != null ? fineractTxnId.toString() : null);
        saved.setStatus(WithdrawalStatus.DEBITED);
        saved.setDebitedAt(Instant.now());
        saved = withdrawalRepository.save(saved);
        log.info("Withdrawal DEBITED withdrawalId={} fineractTxnId={} totalDebit={}",
                saved.getId(), fineractTxnId, saved.getTotalDebit());

        // 11. Submit to bank rails
        BankRailsPort.SubmissionReceipt receipt;
        try {
            receipt = bankRailsPort.submit(new BankRailsPort.SubmissionRequest(
                    saved.getWithdrawalNumber(),
                    saved.getIdempotencyKey(),
                    saved.getBeneficiaryName(),
                    saved.getDestinationIban(),
                    saved.getDestinationBankCode(),
                    saved.getAmount(),
                    saved.getCurrency(),
                    saved.getPurposeCode() != null ? saved.getPurposeCode().name() : null,
                    saved.getPurposeNote()));
        } catch (Exception ex) {
            log.error("Bank rails submission threw withdrawalId={}: {}", saved.getId(), ex.getMessage(), ex);
            return compensate(saved, "WALLET.WITHDRAWAL.BANK_UNAVAILABLE",
                    "Bank rails unavailable: " + ex.getMessage());
        }
        saved.setStatus(WithdrawalStatus.BANK_SUBMITTED);
        saved.setBankSubmittedAt(Instant.now());
        saved.setBankReference(receipt.bankReference());
        saved.setSarieReference(receipt.sarieReference());
        saved = withdrawalRepository.save(saved);

        if (!receipt.accepted()) {
            log.warn("Bank rails REJECTED withdrawalId={} code={} msg={}",
                    saved.getId(), receipt.rejectionCode(), receipt.rejectionMessage());
            return compensate(saved,
                    receipt.rejectionCode() != null ? receipt.rejectionCode() : "WALLET.WITHDRAWAL.BANK_REJECTED",
                    receipt.rejectionMessage() != null ? receipt.rejectionMessage() : "Bank rails rejected the payment");
        }

        // 12. COMPLETED
        saved.setStatus(WithdrawalStatus.COMPLETED);
        saved.setCompletedAt(Instant.now());
        WalletWithdrawal completed = withdrawalRepository.save(saved);

        log.info("Withdrawal COMPLETED withdrawalId={} bankRef={} amount={}",
                completed.getId(), receipt.bankReference(), completed.getAmount());

        try { eventPublisher.publishWithdrawalCompleted(completed); }
        catch (Exception ex) { log.warn("Event publish (completed) failed: {}", ex.getMessage()); }
        return completed;
    }

    private WalletWithdrawal compensate(WalletWithdrawal w, String errorCode, String errorMessage) {
        log.warn("Compensating withdrawalId={} reason={}", w.getId(), errorMessage);
        try {
            Long refundTxn = fineractPort.deposit(
                    walletRepository.findById(w.getSourceWalletId())
                            .map(Wallet::getFineractSavingsAccountId)
                            .orElse(null),
                    w.getTotalDebit(),
                    "REFUND:" + w.getWithdrawalNumber());
            w.setFineractRefundTxnId(refundTxn != null ? refundTxn.toString() : null);
            w.setStatus(WithdrawalStatus.COMPENSATED);
            w.setCompensatedAt(Instant.now());
            w.setErrorCode(errorCode);
            w.setErrorMessage(errorMessage);
            WalletWithdrawal compensated = withdrawalRepository.save(w);
            try { eventPublisher.publishWithdrawalCompensated(compensated); } catch (Exception ignore) { }
            throw new BusinessException(errorCode, errorMessage);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("CRITICAL: compensation failed withdrawalId={} — manual intervention required: {}",
                    w.getId(), ex.getMessage(), ex);
            w.setStatus(WithdrawalStatus.FAILED);
            w.setErrorCode("WALLET.WITHDRAWAL.COMPENSATION_FAILED");
            w.setErrorMessage("Compensation failed: " + ex.getMessage() + " | original: " + errorMessage);
            w.setFailedAt(Instant.now());
            withdrawalRepository.save(w);
            throw new BusinessException("WALLET.WITHDRAWAL.COMPENSATION_FAILED",
                    "Withdrawal failed and refund could not be posted — escalate");
        }
    }

    private BusinessException markFailed(WalletWithdrawal w, String errorCode, String errorMessage) {
        w.setStatus(WithdrawalStatus.FAILED);
        w.setErrorCode(errorCode);
        w.setErrorMessage(errorMessage);
        w.setFailedAt(Instant.now());
        WalletWithdrawal failed = withdrawalRepository.save(w);
        try { eventPublisher.publishWithdrawalFailed(failed); } catch (Exception ignore) { }
        return new BusinessException(errorCode, errorMessage);
    }

    private ResolvedDestination resolveDestination(InitiateWithdrawalCommand cmd) {
        if (cmd.beneficiaryId() != null) {
            IbanBeneficiary b = beneficiaryRepository.findByIdAndTenantId(cmd.beneficiaryId(), cmd.tenantId())
                    .orElseThrow(() -> NotFoundException.forEntity("Beneficiary", cmd.beneficiaryId().toString()));
            if (!b.isActive()) {
                throw new BusinessException("WALLET.BENEFICIARY.INACTIVE",
                        "Beneficiary is inactive: " + b.getId());
            }
            // Beneficiary IBAN was validated when added — but re-validate to catch drift.
            IbanValidator.ValidationResult v = IbanValidator.validate(b.getIban());
            if (!v.valid()) {
                throw new BusinessException("WALLET.BENEFICIARY.IBAN_INVALID",
                        "Saved beneficiary IBAN failed re-validation: " + v.errorMessage());
            }
            return new ResolvedDestination(
                    v.normalizedIban(), b.getBankCode(), b.getBankName(),
                    b.getBeneficiaryName(), b.getId(), v.countryCode());
        }
        if (cmd.destinationIban() == null || cmd.destinationIban().isBlank()) {
            throw new BusinessException("WALLET.WITHDRAWAL.DESTINATION_REQUIRED",
                    "beneficiaryId or destinationIban is required");
        }
        if (cmd.beneficiaryName() == null || cmd.beneficiaryName().isBlank()) {
            throw new BusinessException("WALLET.WITHDRAWAL.BENEFICIARY_NAME_REQUIRED",
                    "beneficiaryName is required for ad-hoc IBAN withdrawals");
        }
        IbanValidator.ValidationResult v = IbanValidator.validate(cmd.destinationIban());
        if (!v.valid()) {
            throw new BusinessException(
                    v.errorCode() != null ? "WALLET.WITHDRAWAL." + v.errorCode() : "WALLET.WITHDRAWAL.IBAN_INVALID",
                    v.errorMessage() != null ? v.errorMessage() : "Destination IBAN is invalid");
        }
        return new ResolvedDestination(
                v.normalizedIban(), cmd.destinationBankCode(), cmd.destinationBankName(),
                cmd.beneficiaryName(), null, v.countryCode());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WALLET.WITHDRAWAL.AMOUNT_INVALID",
                    "Withdrawal amount must be positive");
        }
        if (amount.scale() > 6) {
            throw new BusinessException("WALLET.WITHDRAWAL.AMOUNT_INVALID",
                    "Withdrawal amount exceeds allowed scale");
        }
    }

    private void validateLimits(Wallet wallet, BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            throw new BusinessException("WALLET.WITHDRAWAL.SINGLE_LIMIT_EXCEEDED",
                    "Withdrawal exceeds single-transaction limit");
        }
        // Daily / monthly transaction-limit enforcement (cumulative spend vs wallet limits)
        limitEnforcer.enforce(wallet, amount);
    }

    private BigDecimal computeFee(BigDecimal amount, WithdrawalChannel channel) {
        return switch (channel) {
            case INSTANT_SARIE -> new BigDecimal("1.00");
            case BANK_IBAN, INTERNAL_TRANSFER, OWN_BANK_ACCOUNT -> BigDecimal.ZERO;
        };
    }

    private WithdrawalChannel parseChannel(String input) {
        if (input == null || input.isBlank()) return WithdrawalChannel.BANK_IBAN;
        try {
            return WithdrawalChannel.valueOf(input);
        } catch (IllegalArgumentException ex) {
            return WithdrawalChannel.BANK_IBAN;
        }
    }

    private String serializeMatches(PaymentScreeningPort.ScreeningResult r) {
        if (r.matches() == null || r.matches().isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(r.matches());
        } catch (Exception ex) {
            log.warn("Failed to serialize screening matches: {}", ex.getMessage());
            return null;
        }
    }

    private record ResolvedDestination(
            String iban,
            String bankCode,
            String bankName,
            String beneficiaryName,
            UUID beneficiaryId,
            String country
    ) {}
}
