package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;

import java.util.List;
import java.util.UUID;

public interface GetWithdrawalUseCase {

    WalletWithdrawal getById(UUID tenantId, UUID withdrawalId);

    List<WalletWithdrawal> listByWallet(UUID walletId);

    PageResponse<WalletWithdrawal> listByWallet(UUID walletId, PageQuery query);
}
