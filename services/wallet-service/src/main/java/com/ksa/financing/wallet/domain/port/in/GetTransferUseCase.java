package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.domain.model.WalletTransfer;

import java.util.List;
import java.util.UUID;

public interface GetTransferUseCase {
    WalletTransfer getById(UUID tenantId, UUID transferId);
    List<WalletTransfer> listByWallet(UUID walletId);
    PageResponse<WalletTransfer> listByWallet(UUID walletId, PageQuery query);
}
