package com.ksa.financing.wallet.application.dto;

import com.ksa.financing.wallet.domain.model.WalletLimitBounds;

import java.math.BigDecimal;

public record LimitBoundsResponse(
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
        BigDecimal defaultSingleLimit,
        BigDecimal defaultDailyLimit,
        BigDecimal defaultWeeklyLimit,
        BigDecimal defaultMonthlyLimit,
        BigDecimal defaultYearlyLimit) {

    public static LimitBoundsResponse from(WalletLimitBounds b) {
        return new LimitBoundsResponse(
                b.getMinSingleLimit(), b.getMaxSingleLimit(),
                b.getMinDailyLimit(), b.getMaxDailyLimit(),
                b.getMinWeeklyLimit(), b.getMaxWeeklyLimit(),
                b.getMinMonthlyLimit(), b.getMaxMonthlyLimit(),
                b.getMinYearlyLimit(), b.getMaxYearlyLimit(),
                b.getDefaultSingleLimit(), b.getDefaultDailyLimit(),
                b.getDefaultWeeklyLimit(), b.getDefaultMonthlyLimit(), b.getDefaultYearlyLimit());
    }
}
