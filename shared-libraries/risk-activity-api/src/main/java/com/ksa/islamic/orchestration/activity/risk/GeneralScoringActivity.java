package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Temporal activity contract for GENERAL (product-agnostic, onboarding)
 * credit scoring. Called by the onboarding workflow after each third-party
 * stage (Nafath, Yakeen, GOSI salary, EDD form, AML) to incrementally
 * update the customer's general credit score.
 *
 * Sibling to {@link AmlRiskScoringActivity} which runs AML scoring.
 *
 * <p>Implementation lives in risk-service and delegates to the
 * {@code EvaluateGeneralScoringUseCase}; persists a snapshot to
 * {@code customer_credit_score_snapshots} + upserts
 * {@code customer_credit_score_current}.
 */
@ActivityInterface
public interface GeneralScoringActivity {

    @ActivityMethod
    GeneralScoringResult scoreIncremental(GeneralScoringInput input);

    /** Stage labels used by the workflow when calling scoreIncremental. */
    final class Stages {
        public static final String NAFATH_VERIFIED      = "NAFATH_VERIFIED";
        public static final String YAKEEN_VERIFIED      = "YAKEEN_VERIFIED";
        public static final String EDD_SUBMITTED        = "EDD_SUBMITTED";
        public static final String SALARY_FETCHED       = "SALARY_FETCHED";
        public static final String AML_SCORED           = "AML_SCORED";
        public static final String ONBOARDING_COMPLETE  = "ONBOARDING_COMPLETE";
        private Stages() {}
    }

    record GeneralScoringInput(
            String tenantId,
            String customerId,
            String workflowId,
            String stage,
            Map<String, String> answers
    ) {}

    record GeneralScoringResult(
            String decision,
            BigDecimal scorePercentage,
            BigDecimal totalScore,
            BigDecimal maxPossibleScore,
            String reasonCode,
            int matchedCriteria,
            int totalCriteria,
            String summary
    ) {}
}
