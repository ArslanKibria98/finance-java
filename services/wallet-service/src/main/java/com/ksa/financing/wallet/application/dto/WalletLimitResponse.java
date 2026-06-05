package com.ksa.financing.wallet.application.dto;

import com.ksa.financing.wallet.domain.port.in.GetWalletLimitUseCase.WalletLimitView;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletLimitResponse(
        UUID walletId,
        BigDecimal singleLimit,
        BigDecimal dailyLimit,
        BigDecimal weeklyLimit,
        BigDecimal monthlyLimit,
        BigDecimal yearlyLimit,
        BigDecimal todaySpent,
        BigDecimal weekSpent,
        BigDecimal monthSpent,
        BigDecimal yearSpent,
        BigDecimal minSingleLimit,
        BigDecimal maxSingleLimit,
        BigDecimal minDailyLimit,
        BigDecimal maxDailyLimit,
        BigDecimal minWeeklyLimit,
        BigDecimal maxWeeklyLimit,
        BigDecimal minMonthlyLimit,
        BigDecimal maxMonthlyLimit,
        BigDecimal minYearlyLimit,
        BigDecimal maxYearlyLimit,
        String currency) {

    public static WalletLimitResponse from(WalletLimitView v) {
        return new WalletLimitResponse(
                v.walletId(), v.singleLimit(), v.dailyLimit(), v.weeklyLimit(), v.monthlyLimit(), v.yearlyLimit(),
                v.todaySpent(), v.weekSpent(), v.monthSpent(), v.yearSpent(),
                v.minSingleLimit(), v.maxSingleLimit(),
                v.minDailyLimit(), v.maxDailyLimit(),
                v.minWeeklyLimit(), v.maxWeeklyLimit(),
                v.minMonthlyLimit(), v.maxMonthlyLimit(),
                v.minYearlyLimit(), v.maxYearlyLimit(), v.currency());
    }
}
