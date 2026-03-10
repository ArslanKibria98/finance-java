package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.model.CheckStepResult;
import com.ksa.financing.risk.domain.model.RiskLevel;
import com.ksa.financing.risk.domain.port.out.RiskScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RiskScoreCalculatorImpl implements RiskScoreCalculator {

    private static final int WATCHLIST_WEIGHT = 40;
    private static final int VELOCITY_ANOMALY_WEIGHT = 25;
    private static final int NEW_EVERYTHING_WEIGHT = 15;
    private static final int MULTI_NID_DEVICE_WEIGHT = 30;

    private static final int LOW_MAX = 30;
    private static final int MEDIUM_MAX = 60;
    private static final int HIGH_MAX = 80;

    @Override
    public RiskScoreResult calculateScore(RiskScoreInput input) {
        log.info("Starting preliminary internal risk score calculation");

        try {
            int totalScore = 0;
            List<String> contributingFactors = new ArrayList<>();

            if (input.watchlistMatch()) {
                totalScore += WATCHLIST_WEIGHT;
                contributingFactors.add("WATCHLIST_MATCH(+" + WATCHLIST_WEIGHT + ")");
            }

            if (input.velocityAnomaly()) {
                totalScore += VELOCITY_ANOMALY_WEIGHT;
                contributingFactors.add("VELOCITY_ANOMALY(+" + VELOCITY_ANOMALY_WEIGHT + ")");
            }

            if (input.newDevice() && input.newNid() && input.newMobile()) {
                totalScore += NEW_EVERYTHING_WEIGHT;
                contributingFactors.add("NEW_CUSTOMER_PROFILE(+" + NEW_EVERYTHING_WEIGHT + ")");
            }

            if (input.deviceNidCount() >= 2) {
                totalScore += MULTI_NID_DEVICE_WEIGHT;
                contributingFactors.add("MULTI_NID_DEVICE(+" + MULTI_NID_DEVICE_WEIGHT + ")");
            }

            for (CheckStepResult stepResult : input.previousCheckResults()) {
                if (stepResult.scoreContribution() > 0) {
                    totalScore += stepResult.scoreContribution();
                    contributingFactors.add(stepResult.checkName() + "(+" + stepResult.scoreContribution() + ")");
                }
            }

            totalScore = Math.min(totalScore, 100);

            RiskLevel riskLevel = calculateRiskLevel(totalScore);

            CheckDecision decision;
            if (riskLevel == RiskLevel.CRITICAL) {
                decision = CheckDecision.HARD_BLOCK;
                log.warn("Risk score CRITICAL: {} - blocking", totalScore);
            } else if (riskLevel == RiskLevel.HIGH) {
                decision = CheckDecision.FLAG_ENHANCED_MONITORING;
                log.warn("Risk score HIGH: {} - enhanced monitoring", totalScore);
            } else {
                decision = CheckDecision.PASS;
                log.info("Risk score {}: {} - proceeding", riskLevel, totalScore);
            }

            log.info("Risk score calculation complete: score={}, level={}, factors={}",
                totalScore, riskLevel, contributingFactors);

            return new RiskScoreResult(totalScore, riskLevel, decision, contributingFactors, null);

        } catch (Exception e) {
            log.error("Risk score calculation failed", e);
            return new RiskScoreResult(
                100, RiskLevel.CRITICAL, CheckDecision.HARD_BLOCK,
                List.of("CALCULATION_ERROR"), "Risk score calculation error"
            );
        }
    }

    private RiskLevel calculateRiskLevel(int score) {
        if (score <= LOW_MAX) return RiskLevel.LOW;
        if (score <= MEDIUM_MAX) return RiskLevel.MEDIUM;
        if (score <= HIGH_MAX) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }
}
