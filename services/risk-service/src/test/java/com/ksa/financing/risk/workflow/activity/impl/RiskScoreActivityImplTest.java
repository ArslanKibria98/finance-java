package com.ksa.financing.risk.workflow.activity.impl;

import com.ksa.islamic.orchestration.activity.risk.CheckDecision;
import com.ksa.islamic.orchestration.activity.risk.CheckStepResult;
import com.ksa.islamic.orchestration.activity.risk.RiskLevel;
import com.ksa.islamic.orchestration.activity.risk.RiskScoreActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoreActivityImplTest {

    private RiskScoreActivityImpl activity;

    @BeforeEach
    void setUp() {
        activity = new RiskScoreActivityImpl();
    }

    @Test
    @DisplayName("Clean new customer should have LOW risk")
    void cleanNewCustomer() {
        var input = new RiskScoreActivity.RiskScoreInput(
                List.of(),
                false, false, true, true, true, 0
        );

        var result = activity.calculateScore(input);

        assertEquals(RiskLevel.LOW, result.riskLevel());
        assertEquals(CheckDecision.PASS, result.decision());
        assertEquals(15, result.totalScore()); // new-everything weight
    }

    @Test
    @DisplayName("Watchlist match + velocity anomaly should be HIGH risk")
    void watchlistAndVelocity() {
        var input = new RiskScoreActivity.RiskScoreInput(
                List.of(),
                true, true, false, false, false, 1
        );

        var result = activity.calculateScore(input);

        assertEquals(RiskLevel.HIGH, result.riskLevel());
        assertEquals(CheckDecision.FLAG_ENHANCED_MONITORING, result.decision());
        assertEquals(65, result.totalScore()); // 40 + 25
    }

    @Test
    @DisplayName("Multiple risk factors should hit CRITICAL")
    void criticalRisk() {
        var checkResults = List.of(
                new CheckStepResult("TEST_CHECK", CheckDecision.FLAG_HIGH_RISK, "test", 20, Instant.now())
        );

        var input = new RiskScoreActivity.RiskScoreInput(
                checkResults,
                true, true, false, false, false, 2
        );

        var result = activity.calculateScore(input);

        // 40 (watchlist) + 25 (velocity) + 30 (multi-NID device) + 20 (check contribution) = 115 → capped at 100
        assertEquals(RiskLevel.CRITICAL, result.riskLevel());
        assertEquals(CheckDecision.HARD_BLOCK, result.decision());
        assertEquals(100, result.totalScore());
    }

    @Test
    @DisplayName("Returning customer with no flags should be LOW risk")
    void returningCustomerLowRisk() {
        var input = new RiskScoreActivity.RiskScoreInput(
                List.of(),
                false, false, false, false, false, 1
        );

        var result = activity.calculateScore(input);

        assertEquals(RiskLevel.LOW, result.riskLevel());
        assertEquals(CheckDecision.PASS, result.decision());
        assertEquals(0, result.totalScore());
    }

    @Test
    @DisplayName("Device with 2 NIDs should add 30 points")
    void multiNidDevice() {
        var input = new RiskScoreActivity.RiskScoreInput(
                List.of(),
                false, false, false, true, true, 2
        );

        var result = activity.calculateScore(input);

        assertEquals(30, result.totalScore()); // multi-NID device only
        assertEquals(RiskLevel.LOW, result.riskLevel());
    }
}
