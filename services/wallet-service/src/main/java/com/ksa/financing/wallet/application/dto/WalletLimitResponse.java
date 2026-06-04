package com.ksa.financing.wallet.application.dto;

import com.ksa.financing.wallet.domain.port.in.GetWalletLimitUseCase.WalletLimitView;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletLimitResponse(
        UUID walletId,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        BigDecimal yearlyLimit,
        BigDecimal todaySpent,
        BigDecimal monthSpent,
        BigDecimal yearSpent,
        BigDecimal dailyRemaining,
        BigDecimal monthlyRemaining,
        BigDecimal yearlyRemaining,
        String currency) {

    public static WalletLimitResponse from(WalletLimitView v) {
        return new WalletLimitResponse(
                v.walletId(), v.dailyLimit(), v.monthlyLimit(), v.yearlyLimit(),
                v.todaySpent(), v.monthSpent(), v.yearSpent(),
                v.dailyRemaining(), v.monthlyRemaining(), v.yearlyRemaining(), v.currency());
    }
}
