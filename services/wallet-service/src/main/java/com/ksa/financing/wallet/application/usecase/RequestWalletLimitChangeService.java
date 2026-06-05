package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.LimitRequestStatus;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;
import com.ksa.financing.wallet.domain.port.in.ManageWalletLimitBoundsUseCase;
import com.ksa.financing.wallet.domain.port.in.RequestWalletLimitChangeUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletLimitChangeRequestRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RequestWalletLimitChangeService implements RequestWalletLimitChangeUseCase {

    private final WalletRepository walletRepository;
    private final WalletLimitChangeRequestRepository requestRepository;
    private final ManageWalletLimitBoundsUseCase boundsUseCase;

    public RequestWalletLimitChangeService(
            WalletRepository walletRepository,
            WalletLimitChangeRequestRepository requestRepository,
            ManageWalletLimitBoundsUseCase boundsUseCase) {
        this.walletRepository = walletRepository;
        this.requestRepository = requestRepository;
        this.boundsUseCase = boundsUseCase;
    }

    @Override
    @Transactional
    public WalletLimitChangeRequest request(RequestCommand cmd) {
        Wallet wallet = loadWallet(cmd.tenantId(), cmd.walletId());

        WalletLimitBounds bounds = boundsUseCase.getBounds(cmd.tenantId());
        if (!bounds.isSingleWithinBounds(cmd.requestedSingleLimit())) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.SINGLE_OUT_OF_BOUNDS",
                    "Requested single (per-transaction) limit must be within [" + bounds.getMinSingleLimit()
                            + ", " + bounds.getMaxSingleLimit() + "]");
        }
        if (!bounds.isDailyWithinBounds(cmd.requestedDailyLimit())) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.DAILY_OUT_OF_BOUNDS",
                    "Requested daily limit must be within [" + bounds.getMinDailyLimit()
                            + ", " + bounds.getMaxDailyLimit() + "]");
        }
        if (!bounds.isMonthlyWithinBounds(cmd.requestedMonthlyLimit())) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.MONTHLY_OUT_OF_BOUNDS",
                    "Requested monthly limit must be within [" + bounds.getMinMonthlyLimit()
                            + ", " + bounds.getMaxMonthlyLimit() + "]");
        }
        if (!bounds.isYearlyWithinBounds(cmd.requestedYearlyLimit())) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.YEARLY_OUT_OF_BOUNDS",
                    "Requested yearly limit must be within [" + bounds.getMinYearlyLimit()
                            + ", " + bounds.getMaxYearlyLimit() + "]");
        }
        if (cmd.requestedDailyLimit().compareTo(cmd.requestedSingleLimit()) < 0) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.DAILY_BELOW_SINGLE",
                    "Daily limit cannot be lower than the single (per-transaction) limit");
        }
        if (cmd.requestedMonthlyLimit().compareTo(cmd.requestedDailyLimit()) < 0) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.MONTHLY_BELOW_DAILY",
                    "Monthly limit cannot be lower than daily limit");
        }
        if (cmd.requestedYearlyLimit().compareTo(cmd.requestedMonthlyLimit()) < 0) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.YEARLY_BELOW_MONTHLY",
                    "Yearly limit cannot be lower than monthly limit");
        }
        if (requestRepository.existsByWalletAndStatus(cmd.walletId(), LimitRequestStatus.PENDING)) {
            throw new BusinessException("WALLET.LIMIT_REQUEST.ALREADY_PENDING",
                    "A pending limit-change request already exists for this wallet");
        }

        WalletLimitChangeRequest req = new WalletLimitChangeRequest();
        req.setId(UUID.randomUUID());
        req.setTenantId(cmd.tenantId());
        req.setWalletId(wallet.getId());
        req.setCustomerId(wallet.getCustomerId());
        req.setRequestedSingleLimit(cmd.requestedSingleLimit());
        req.setRequestedDailyLimit(cmd.requestedDailyLimit());
        req.setRequestedMonthlyLimit(cmd.requestedMonthlyLimit());
        req.setRequestedYearlyLimit(cmd.requestedYearlyLimit());
        req.setCurrentSingleLimit(wallet.getSingleTransactionLimit());
        req.setCurrentDailyLimit(wallet.getDailyTransactionLimit());
        req.setCurrentMonthlyLimit(wallet.getMonthlyTransactionLimit());
        req.setCurrentYearlyLimit(wallet.getYearlyTransactionLimit());
        req.setReason(cmd.reason());
        req.setStatus(LimitRequestStatus.PENDING);
        req.setRequestedBy(cmd.requestedBy());
        req.setRequestedAt(Instant.now());
        WalletLimitChangeRequest saved = requestRepository.save(req);
        log.info("Limit-change request created id={} wallet={} reqDaily={} reqMonthly={} reqYearly={} by={}",
                saved.getId(), wallet.getId(), cmd.requestedDailyLimit(),
                cmd.requestedMonthlyLimit(), cmd.requestedYearlyLimit(), cmd.requestedBy());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletLimitChangeRequest> listForWallet(UUID tenantId, UUID walletId) {
        loadWallet(tenantId, walletId);
        return requestRepository.findByWallet(walletId);
    }

    private Wallet loadWallet(UUID tenantId, UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", walletId.toString()));
        if (!wallet.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "Wallet does not belong to tenant");
        }
        return wallet;
    }
}
