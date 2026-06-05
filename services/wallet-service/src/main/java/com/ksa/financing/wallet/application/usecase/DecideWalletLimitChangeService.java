package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.LimitRequestStatus;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;
import com.ksa.financing.wallet.domain.port.in.DecideWalletLimitChangeUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletLimitChangeRequestRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecideWalletLimitChangeService implements DecideWalletLimitChangeUseCase {

    private final WalletRepository walletRepository;
    private final WalletLimitChangeRequestRepository requestRepository;

    @Override
    @Transactional
    public WalletLimitChangeRequest approve(ApproveCommand cmd) {
        WalletLimitChangeRequest req = loadRequest(cmd.tenantId(), cmd.requestId());
        req.approve(cmd.decisionBy(), cmd.notes());

        // Apply the approved limits to the wallet — this is when the new limit takes effect.
        Wallet wallet = walletRepository.findById(req.getWalletId())
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", req.getWalletId().toString()));
        wallet.setSingleTransactionLimit(req.getRequestedSingleLimit());
        wallet.setDailyTransactionLimit(req.getRequestedDailyLimit());
        wallet.setMonthlyTransactionLimit(req.getRequestedMonthlyLimit());
        wallet.setYearlyTransactionLimit(req.getRequestedYearlyLimit());
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        WalletLimitChangeRequest saved = requestRepository.save(req);
        log.info("Limit-change APPROVED id={} wallet={} newDaily={} newMonthly={} newYearly={} by={}",
                saved.getId(), wallet.getId(), req.getRequestedDailyLimit(),
                req.getRequestedMonthlyLimit(), req.getRequestedYearlyLimit(), cmd.decisionBy());
        return saved;
    }

    @Override
    @Transactional
    public WalletLimitChangeRequest reject(RejectCommand cmd) {
        WalletLimitChangeRequest req = loadRequest(cmd.tenantId(), cmd.requestId());
        req.reject(cmd.decisionBy(), cmd.reason(), cmd.notes());
        WalletLimitChangeRequest saved = requestRepository.save(req);
        log.info("Limit-change REJECTED id={} wallet={} by={} reason={}",
                saved.getId(), req.getWalletId(), cmd.decisionBy(), cmd.reason());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletLimitChangeRequest> list(UUID tenantId, LimitRequestStatus status) {
        return status != null
                ? requestRepository.findByTenantAndStatus(tenantId, status)
                : requestRepository.findByTenant(tenantId);
    }

    private WalletLimitChangeRequest loadRequest(UUID tenantId, UUID requestId) {
        WalletLimitChangeRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> NotFoundException.forEntity("WalletLimitChangeRequest", requestId.toString()));
        if (!req.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "Limit-change request does not belong to tenant");
        }
        return req;
    }
}
