package com.ksa.financing.fraud.application.dto;

public record FraudHistoryCheckResponseDto(
        boolean hasFraudHistory,
        int confirmedFraudCount,
        int suspectedFraudCount,
        int totalEvaluations,
        String riskLevel,
        String recommendation
) {

    public static FraudHistoryCheckResponseDto clean() {
        return new FraudHistoryCheckResponseDto(false, 0, 0, 0, "LOW", "PASS");
    }

    public static FraudHistoryCheckResponseDto withHistory(int confirmed, int suspected, int total) {
        var riskLevel = confirmed > 0 ? "CRITICAL" : suspected > 2 ? "HIGH" : suspected > 0 ? "MEDIUM" : "LOW";
        var recommendation = confirmed > 0 ? "BLOCK" : suspected > 2 ? "REVIEW_MANUAL" : "PASS";
        return new FraudHistoryCheckResponseDto(confirmed > 0 || suspected > 0, confirmed, suspected, total, riskLevel, recommendation);
    }
}
