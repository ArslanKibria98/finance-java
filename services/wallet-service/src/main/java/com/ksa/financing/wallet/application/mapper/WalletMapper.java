package com.ksa.financing.wallet.application.mapper;

import com.ksa.financing.wallet.application.dto.TopUpResponse;
import com.ksa.financing.wallet.application.dto.WalletMovementResponse;
import com.ksa.financing.wallet.application.dto.WalletResponse;
import com.ksa.financing.wallet.domain.model.TopUpTransaction;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletMovement;

public class WalletMapper {

    private WalletMapper() {}

    public static WalletResponse toResponse(Wallet w) {
        return new WalletResponse(
            w.getId(),
            w.getWalletNumber(),
            w.getCustomerId(),
            w.getAvailableBalance(),
            w.getReservedBalance(),
            w.getTotalBalance(),
            w.getCurrency(),
            w.getStatus() != null ? w.getStatus().name() : null,
            w.isAutoDebitEnabled(),
            w.getFineractSavingsAccountId(),
            w.isLedgerSynced(),
            w.getCreatedAt(),
            w.getUpdatedAt()
        );
    }

    public static TopUpResponse toResponse(TopUpTransaction t) {
        return new TopUpResponse(
            t.getId(),
            t.getTransactionNumber(),
            t.getMethod() != null ? t.getMethod().name() : null,
            t.getAmount(),
            t.getFeeAmount(),
            t.getNetAmount(),
            t.getStatus() != null ? t.getStatus().name() : null,
            t.getInitiatedAt(),
            t.getCompletedAt()
        );
    }

    public static WalletMovementResponse toResponse(WalletMovement m) {
        return new WalletMovementResponse(
            m.getId(),
            m.getMovementNumber(),
            m.getMovementType() != null ? m.getMovementType().name() : null,
            m.getPurpose() != null ? m.getPurpose().name() : null,
            m.getAmount(),
            m.getBalanceBefore(),
            m.getBalanceAfter(),
            m.getDescription(),
            m.getCreatedAt()
        );
    }
}
