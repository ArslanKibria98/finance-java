package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.port.in.GetWithdrawalUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletWithdrawalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetWithdrawalService implements GetWithdrawalUseCase {

    private final WalletWithdrawalRepository repository;

    @Override
    @Transactional(readOnly = true)
    public WalletWithdrawal getById(UUID tenantId, UUID withdrawalId) {
        return repository.findByIdAndTenantId(withdrawalId, tenantId)
                .orElseThrow(() -> NotFoundException.forEntity("Withdrawal", withdrawalId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletWithdrawal> listByWallet(UUID walletId) {
        return repository.findBySourceWallet(walletId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletWithdrawal> listByWallet(UUID walletId, PageQuery query) {
        return repository.findBySourceWallet(walletId, query);
    }
}
