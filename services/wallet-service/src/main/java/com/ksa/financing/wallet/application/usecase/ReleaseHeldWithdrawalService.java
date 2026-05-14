package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.model.WithdrawalStatus;
import com.ksa.financing.wallet.domain.port.in.ReleaseHeldWithdrawalUseCase;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.domain.port.out.WalletWithdrawalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Releases a HELD_AML withdrawal — moves it to VALIDATED then continues the SAGA.
 * <p>
 * Authorisation: caller must have @SecuredEndpoint(obj="wallet.withdrawals.aml-review", act="approve")
 * (compliance_officer or admin). Enforced at controller layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseHeldWithdrawalService implements ReleaseHeldWithdrawalUseCase {

    private final WalletWithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final FineractSavingsPort fineractPort;
    private final InitiateWithdrawalService initiateService;

    @Override
    @Transactional
    public WalletWithdrawal release(ReleaseCommand cmd) {
        WalletWithdrawal w = withdrawalRepository.findByIdAndTenantId(cmd.withdrawalId(), cmd.tenantId())
                .orElseThrow(() -> NotFoundException.forEntity("Withdrawal", cmd.withdrawalId().toString()));

        if (w.getStatus() != WithdrawalStatus.HELD_AML) {
            throw new BusinessException("WALLET.WITHDRAWAL.NOT_HELD",
                    "Withdrawal is not HELD_AML — current status: " + w.getStatus());
        }
        if (cmd.reason() == null || cmd.reason().isBlank()) {
            throw new BusinessException("WALLET.WITHDRAWAL.RELEASE_REASON_REQUIRED",
                    "release reason is required for SAMA audit trail");
        }

        // Audit: record reviewer
        w.setReleasedBy(cmd.releasedBy());
        w.setReleasedAt(Instant.now());
        w.setReleaseReason(cmd.reason());
        w.setStatus(WithdrawalStatus.VALIDATED);
        WalletWithdrawal validated = withdrawalRepository.save(w);

        log.info("Withdrawal RELEASED withdrawalId={} reviewer={} reason={}",
                validated.getId(), cmd.releasedBy(), cmd.reason());

        // Resume SAGA — load wallet + Fineract account info, then debit + submit
        Wallet source = walletRepository.findById(validated.getSourceWalletId())
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", validated.getSourceWalletId().toString()));
        if (source.getFineractSavingsAccountId() == null) {
            throw new BusinessException("WALLET.WITHDRAWAL.NOT_FINERACT_LINKED",
                    "Wallet not linked to Fineract savings account");
        }

        FineractSavingsPort.SavingsAccountInfo srcInfo;
        try {
            srcInfo = fineractPort.getAccountInfo(source.getFineractSavingsAccountId());
        } catch (Exception ex) {
            log.error("Fineract account info fetch failed during release: {}", ex.getMessage());
            throw new BusinessException("WALLET.WITHDRAWAL.FINERACT_UNAVAILABLE",
                    "Core banking unavailable, please retry");
        }
        // Re-check balance — may have changed while held
        if (srcInfo.availableBalance() == null
                || srcInfo.availableBalance().compareTo(validated.getTotalDebit()) < 0) {
            throw new BusinessException(ErrorCodes.Wallet.INSUFFICIENT_FUNDS,
                    "Insufficient available balance at release time: have="
                            + srcInfo.availableBalance() + " need=" + validated.getTotalDebit());
        }

        return initiateService.executeDebitAndSubmit(validated, source, srcInfo);
    }
}
