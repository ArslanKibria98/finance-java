package com.ksa.financing.fraud.infrastructure.rule;

import com.ksa.financing.fraud.domain.model.fraud.FraudDecision;

/**
 * Score contribution constants per decision (used uniformly by all evaluators).
 */
public final class EvaluatorScores {

    public static final int BLOCK_SCORE = 50;
    public static final int HOLD_SCORE = 25;
    public static final int ALERT_SCORE = 10;

    private EvaluatorScores() {}

    public static int forDecision(FraudDecision decision) {
        return switch (decision) {
            case BLOCK -> BLOCK_SCORE;
            case HOLD -> HOLD_SCORE;
            case ALERT -> ALERT_SCORE;
            case ALLOW -> 0;
        };
    }
}
