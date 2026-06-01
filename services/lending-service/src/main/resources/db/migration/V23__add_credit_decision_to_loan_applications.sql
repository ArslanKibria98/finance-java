-- ============================================================================
-- V23: Persist credit decision engine result on loan applications
-- ----------------------------------------------------------------------------
-- LOS §5 Step 4 — Credit Decisioning ("Black Box"). The risk-service
-- CreditScoringDecisionEngine produces a Green/Amber/Red decision with a
-- score breakdown; we snapshot that decision on the loan application so it is
-- visible to admins, audit, and reporting without re-running the engine.
-- ============================================================================

ALTER TABLE loan_applications
    ADD COLUMN IF NOT EXISTS credit_decision         VARCHAR(32),
    ADD COLUMN IF NOT EXISTS credit_decision_reason  VARCHAR(64),
    ADD COLUMN IF NOT EXISTS scoring_total_score     NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS scoring_max_score       NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS scoring_percentage      NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS scoring_green_threshold NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS scoring_amber_threshold NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS scoring_summary         TEXT,
    ADD COLUMN IF NOT EXISTS scoring_details         JSONB,
    ADD COLUMN IF NOT EXISTS scoring_evaluated_at    TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_loan_apps_credit_decision
    ON loan_applications (tenant_id, credit_decision)
    WHERE credit_decision IS NOT NULL;

COMMENT ON COLUMN loan_applications.credit_decision IS
    'Credit decision engine outcome: AUTO_APPROVE | REFER_MANUAL_REVIEW | AUTO_REJECT';
COMMENT ON COLUMN loan_applications.credit_decision_reason IS
    'Short reason code: GREEN_AUTO_APPROVE | AMBER_MANUAL_REVIEW | RED_AUTO_REJECT | NO_CRITERIA | ENGINE_UNAVAILABLE';
COMMENT ON COLUMN loan_applications.scoring_details IS
    'JSONB array of CriteriaEvaluationDetail entries (fieldKey, passed, scoredWeight, maxWeight, matchedRule, failureReason)';
