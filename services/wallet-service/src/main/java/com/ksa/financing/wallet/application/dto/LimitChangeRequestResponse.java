package com.ksa.financing.wallet.application.dto;

import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LimitChangeRequestResponse(
        UUID id,
        UUID walletId,
        UUID customerId,
        BigDecimal requestedSingleLimit,
        BigDecimal requestedDailyLimit,
        BigDecimal requestedWeeklyLimit,
        BigDecimal requestedMonthlyLimit,
        BigDecimal requestedYearlyLimit,
        BigDecimal currentSingleLimit,
        BigDecimal currentDailyLimit,
        BigDecimal currentWeeklyLimit,
        BigDecimal currentMonthlyLimit,
        BigDecimal currentYearlyLimit,
        String reason,
        String status,
        UUID requestedBy,
        Instant requestedAt,
        UUID decisionBy,
        Instant decisionAt,
        String decisionNotes,
        String rejectionReason) {

    public static LimitChangeRequestResponse from(WalletLimitChangeRequest r) {
        return new LimitChangeRequestResponse(
                r.getId(), r.getWalletId(), r.getCustomerId(),
                r.getRequestedSingleLimit(), r.getRequestedDailyLimit(), r.getRequestedWeeklyLimit(), r.getRequestedMonthlyLimit(), r.getRequestedYearlyLimit(),
                r.getCurrentSingleLimit(), r.getCurrentDailyLimit(), r.getCurrentWeeklyLimit(), r.getCurrentMonthlyLimit(), r.getCurrentYearlyLimit(),
                r.getReason(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getRequestedBy(), r.getRequestedAt(),
                r.getDecisionBy(), r.getDecisionAt(),
                r.getDecisionNotes(), r.getRejectionReason());
    }
}
