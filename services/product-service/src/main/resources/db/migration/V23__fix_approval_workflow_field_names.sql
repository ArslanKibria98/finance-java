-- ============================================================================
-- V23: Fix Approval Workflow Field Names to Match UI Dropdowns
-- ============================================================================
-- UI dropdown field keys (snake_case — what frontend stores):
--   dbr_percentage, financing_amount, credit_score, simah_score, risk_type, pep
--
-- UI dropdown operators:
--   <=, >=, =, <, >
--
-- This migration:
--   1. Deletes V22 seeded workflows (keeps manually created ones)
--   2. Re-seeds with correct field names and operators matching UI
-- ============================================================================

DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_product_id UUID := '4210f18e-8c35-4b12-9b7f-c96de95767c7';
    v_wf_id UUID;

BEGIN

-- ============================================================================
-- STEP 1: Delete V22 seeded workflows (name starts with R/A/M + number)
-- Keep manually created workflows (like "Auto Scenario")
-- ============================================================================

DELETE FROM product_approval_workflows
WHERE product_id = v_product_id
  AND (name_en LIKE 'R%:%' OR name_en LIKE 'A%:%' OR name_en LIKE 'M%:%');

RAISE NOTICE 'Deleted old V22 workflows with wrong field names';

-- ============================================================================
-- ██████╗ ███████╗     ██╗███████╗ ██████╗████████╗██╗ ██████╗ ███╗   ██╗
-- REJECTION SCENARIOS — Using only: DBR, Loan Amount, Credit Score, SIMAH, Risk Type, PEP
-- ============================================================================

-- ── R1: SIMAH Score Below 500 (High Risk) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R1: SIMAH Score Below 500', 'ر1: درجة سمة أقل من 500',
    'SAMA: Auto reject when SIMAH credit score is below 500',
    TRUE, 1, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_score', '<', '500', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SIMAH.SCORE_BELOW_MINIMUM", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R2: DBR Exceeds SAMA Absolute Cap 65% ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R2: DBR Exceeds 65% SAMA Cap', 'ر2: نسبة عبء الدين تتجاوز 65%',
    'SAMA mandatory: Auto reject when Debt Burden Ratio exceeds absolute SAMA cap of 65%',
    TRUE, 2, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'dbr_percentage', '>', '65', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.DBR.ABSOLUTE_LIMIT_EXCEEDED", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R3: Risk Type = CRITICAL ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R3: Critical Risk Level', 'ر3: مستوى مخاطر حرج',
    'Auto reject when risk assessment engine returns CRITICAL risk level',
    TRUE, 3, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_type', '=', 'CRITICAL', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.RISK.CRITICAL", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R4: Credit Score Below 30 (Scorecard Decline) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R4: Credit Score Below Cutoff', 'ر4: درجة الائتمان أقل من الحد',
    'Auto reject when internal credit score falls below decline cutoff of 30',
    TRUE, 4, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'credit_score', '<', '30', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.SCORECARD.BELOW_CUTOFF", "notify_customer": true}', 1, NOW(), NOW());


-- ── R5: Loan Amount Exceeds Product Maximum (500K) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R5: Loan Amount Exceeds Maximum', 'ر5: مبلغ التمويل يتجاوز الحد الأقصى',
    'Auto reject when requested loan amount exceeds product maximum of 500,000 SAR',
    TRUE, 5, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'financing_amount', '>', '500000', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "PRODUCT.AMOUNT.EXCEEDS_MAXIMUM", "notify_customer": true}', 1, NOW(), NOW());


-- ── R6: SIMAH < 550 AND DBR > 50 (Bad Score + High Debt) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R6: Low SIMAH + High DBR', 'ر6: سمة منخفض + عبء دين مرتفع',
    'Auto reject when SIMAH is below 550 AND DBR exceeds 50% — compounding risk factors',
    TRUE, 6, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '<', '550', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '>', '50', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND_RISK.LOW_SIMAH_HIGH_DBR", "notify_customer": true}', 1, NOW(), NOW());


-- ── R7: Risk Type = HIGH AND SIMAH < 550 ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R7: High Risk + Low SIMAH', 'ر7: مخاطر عالية + سمة منخفض',
    'Auto reject when Risk Type is HIGH and SIMAH score is below 550',
    TRUE, 7, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'risk_type', '=', 'HIGH', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '<', '550', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND_RISK.HIGH_RISK_LOW_SIMAH", "notify_customer": true}', 1, NOW(), NOW());


-- ── R8: Low Credit Score + High Loan Amount ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R8: Low Score + High Amount', 'ر8: درجة منخفضة + مبلغ مرتفع',
    'Auto reject when credit score is below 40 and loan amount exceeds 100,000 SAR',
    TRUE, 8, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'credit_score', '<', '40', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '>', '100000', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND_RISK.LOW_SCORE_HIGH_AMOUNT", "notify_customer": true}', 1, NOW(), NOW());


-- ── R9: PEP = true AND Risk Type = HIGH ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R9: PEP with High Risk', 'ر9: شخص سياسي مع مخاطر عالية',
    'Auto reject Politically Exposed Person when combined with HIGH risk assessment',
    TRUE, 9, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'pep', '=', 'true', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'HIGH', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.AML.PEP_HIGH_RISK", "notify_customer": false, "report_to_compliance": true}', 1, NOW(), NOW());


-- ── R10: DBR > 45 AND SIMAH < 600 ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R10: Stretched DBR + Weak SIMAH', 'ر10: عبء دين ممتد + سمة ضعيف',
    'Auto reject when DBR exceeds 45% and SIMAH score is below 600 — dual weakness',
    TRUE, 10, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'dbr_percentage', '>', '45', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '<', '600', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND_RISK.STRETCHED_DBR_WEAK_SIMAH", "notify_customer": true}', 1, NOW(), NOW());


-- ── R11: Loan Amount > 200K AND Credit Score < 50 ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R11: High Amount + Moderate Score', 'ر11: مبلغ عالي + درجة متوسطة',
    'Auto reject when loan amount exceeds 200K and credit score is below 50',
    TRUE, 11, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'financing_amount', '>', '200000', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '<', '50', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND_RISK.HIGH_AMOUNT_MODERATE_SCORE", "notify_customer": true}', 1, NOW(), NOW());


-- ── R12: Risk Type = HIGH AND DBR > 40 ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R12: High Risk + Stretched DBR', 'ر12: مخاطر عالية + عبء دين ممتد',
    'Auto reject when risk type is HIGH and DBR exceeds 40%',
    TRUE, 12, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'risk_type', '=', 'HIGH', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '>', '40', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND_RISK.HIGH_RISK_STRETCHED_DBR", "notify_customer": true}', 1, NOW(), NOW());


-- ============================================================================
--  █████╗ ██╗   ██╗████████╗ ██████╗
-- AUTO APPROVAL SCENARIOS — All conditions must be TRUE simultaneously
-- ============================================================================

-- ── A1: Excellent Profile — High SIMAH, Low DBR, Moderate Amount ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A1: Excellent Profile', 'م1: ملف ممتاز',
    'Auto approve: SIMAH >= 700, DBR <= 33%, Loan <= 100K, Low Risk, Credit Score >= 70',
    TRUE, 13, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '100000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'LOW', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '>=', '70', 5, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.EXCELLENT_PROFILE", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A2: Premium Score — SIMAH 800+, Small Amount ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A2: Premium SIMAH Score', 'م2: درجة سمة متميزة',
    'Auto approve: SIMAH >= 800, DBR <= 33%, Loan <= 200K, Not PEP',
    TRUE, 14, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '800', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '200000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'pep', '=', 'false', 4, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.PREMIUM_SIMAH", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A3: Small Loan — Low Amount, Decent Score ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A3: Small Loan Low Risk', 'م3: تمويل صغير منخفض المخاطر',
    'Auto approve: Loan <= 30K, SIMAH >= 650, DBR <= 33%, Low Risk',
    TRUE, 15, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'financing_amount', '<=', '30000', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '>=', '650', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'LOW', 4, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.SMALL_LOAN_LOW_RISK", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A4: High Score + High Credit Score ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A4: Dual High Scores', 'م4: درجات مزدوجة مرتفعة',
    'Auto approve: SIMAH >= 750, Credit Score >= 80, DBR <= 40%, Not PEP',
    TRUE, 16, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '750', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '>=', '80', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '40', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'pep', '=', 'false', 4, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.DUAL_HIGH_SCORES", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A5: Conservative — Very Low DBR, Good Score ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A5: Very Low DBR Profile', 'م5: ملف نسبة دين منخفضة جداً',
    'Auto approve: DBR <= 20%, SIMAH >= 650, Loan <= 150K, Low Risk',
    TRUE, 17, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '20', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '>=', '650', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '150000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'LOW', 4, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.VERY_LOW_DBR", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A6: Moderate Amount + Strong Indicators ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A6: Strong All-Round Profile', 'م6: ملف قوي شامل',
    'Auto approve: SIMAH >= 700, Credit Score >= 75, DBR <= 33%, Loan <= 150K, Low Risk, Not PEP',
    TRUE, 18, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '>=', '75', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '150000', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'LOW', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'pep', '=', 'false', 6, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.STRONG_ALL_ROUND", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ============================================================================
-- ███╗   ███╗ █████╗ ███╗   ██╗██╗   ██╗ █████╗ ██╗
-- MANUAL REVIEW SCENARIOS — Amber zone, officer discretion
-- ============================================================================

-- ── M1: Borderline SIMAH Score (500-699) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M1: Borderline SIMAH (500-699)', 'ي1: سمة حدي (500-699)',
    'Manual review: SIMAH in borderline range — officer must assess repayment ability',
    TRUE, 19, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '500', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '<', '700', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.SIMAH.BORDERLINE"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'NOTIFY', '{"notify_email": "credit-committee@kfs.com", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M2: DBR Above Product Cap (33%-65%) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M2: DBR 33%-65% Above Product Cap', 'ي2: نسبة دين 33%-65% فوق حد المنتج',
    'Manual review: DBR above product cap but within SAMA absolute cap — officer can approve with justification',
    TRUE, 20, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'dbr_percentage', '>', '33', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '65', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.DBR.ABOVE_PRODUCT_CAP"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'NOTIFY', '{"notify_email": "credit-committee@kfs.com", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M3: PEP Detected — Enhanced Due Diligence ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M3: PEP Enhanced Due Diligence', 'ي3: عناية واجبة معززة لشخص سياسي',
    'SAMA AML: Manual review with EDD when Politically Exposed Person is detected',
    TRUE, 21, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'pep', '=', 'true', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "COMPLIANCE_OFFICER", "reason_code": "MANUAL.PEP.EDD_REQUIRED"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "HEAD_OF_COMPLIANCE", "escalation_hours": 48}', 2, NOW(), NOW());


-- ── M4: High Loan Amount (100K-300K) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M4: High Amount (100K-300K)', 'ي4: مبلغ مرتفع (100ألف-300ألف)',
    'Manual review: Financing amount exceeds auto-approval limit — senior credit approval required',
    TRUE, 22, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'financing_amount', '>', '100000', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '300000', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "SENIOR_CREDIT", "reason_code": "MANUAL.AMOUNT.HIGH"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "CREDIT_COMMITTEE", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M5: Very High Amount (> 300K) — Committee ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M5: Very High Amount (>300K)', 'ي5: مبلغ مرتفع جداً (>300ألف)',
    'SAMA prudential: Credit committee approval required for financing exceeding 300K SAR',
    TRUE, 23, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'financing_amount', '>', '300000', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_COMMITTEE", "reason_code": "MANUAL.AMOUNT.COMMITTEE_REQUIRED"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "GM_CREDIT", "escalation_hours": 48}', 2, NOW(), NOW());


-- ── M6: Risk Type = HIGH ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M6: High Risk Assessment', 'ي6: تقييم مخاطر عالي',
    'Manual review when risk engine returns HIGH risk level — needs officer assessment',
    TRUE, 24, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_type', '=', 'HIGH', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.RISK.HIGH"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M7: Risk Type = MEDIUM AND Loan > 50K ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M7: Medium Risk + Significant Amount', 'ي7: مخاطر متوسطة + مبلغ كبير',
    'Manual review: Medium risk combined with loan amount over 50K SAR',
    TRUE, 25, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'risk_type', '=', 'MEDIUM', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '>', '50000', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.MEDIUM_RISK_HIGH_AMOUNT"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M8: Borderline Credit Score (30-59) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M8: Borderline Credit Score (30-59)', 'ي8: درجة ائتمان حدية (30-59)',
    'Manual review: Internal credit score in amber zone between decline and approval cutoffs',
    TRUE, 26, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'credit_score', '>=', '30', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '<', '60', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.CREDIT_SCORE.BORDERLINE"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M9: SIMAH 500-599 AND DBR > 40% ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M9: Low SIMAH + Stretched DBR', 'ي9: سمة منخفض + عبء دين ممتد',
    'Manual review: SIMAH 500-599 combined with DBR above 40% — dual amber flags',
    TRUE, 27, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '500', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '<', '600', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '>', '40', 3, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "SENIOR_CREDIT", "reason_code": "MANUAL.COMPOUND.LOW_SIMAH_HIGH_DBR"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "CREDIT_COMMITTEE", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M10: Risk Type = MEDIUM AND SIMAH < 650 ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M10: Medium Risk + Moderate SIMAH', 'ي10: مخاطر متوسطة + سمة معتدل',
    'Manual review: Medium risk assessment combined with SIMAH below 650',
    TRUE, 28, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'risk_type', '=', 'MEDIUM', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '<', '650', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.MEDIUM_RISK_MODERATE_SIMAH"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M11: PEP + High Amount (> 100K) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M11: PEP + High Amount', 'ي11: شخص سياسي + مبلغ مرتفع',
    'Manual review: PEP detected with loan amount over 100K — enhanced scrutiny required',
    TRUE, 29, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'pep', '=', 'true', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '>', '100000', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "COMPLIANCE_OFFICER", "reason_code": "MANUAL.PEP.HIGH_AMOUNT"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "HEAD_OF_COMPLIANCE", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M12: Credit Score 60-69 AND Loan > 100K ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M12: Moderate Score + High Amount', 'ي12: درجة معتدلة + مبلغ مرتفع',
    'Manual review: Credit score 60-69 (not strong enough for auto-approve) with high loan amount',
    TRUE, 30, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'credit_score', '>=', '60', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '<', '70', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '>', '100000', 3, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "SENIOR_CREDIT", "reason_code": "MANUAL.MODERATE_SCORE_HIGH_AMOUNT"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "CREDIT_COMMITTEE", "escalation_hours": 24}', 2, NOW(), NOW());


-- ============================================================================
-- SUMMARY
-- ============================================================================
-- Total NEW workflows: 30
--   Rejection (R1-R12):    12 cases — Priority 1-12
--   Auto Approval (A1-A6):  6 cases — Priority 13-18
--   Manual Review (M1-M12): 12 cases — Priority 19-30
--
-- ALL conditions use ONLY these UI fields: DBR, Loan Amount, Credit Score, SIMAH, Risk Type, PEP
-- ALL operators use ONLY: <=, >=, =, <, >
-- ============================================================================

RAISE NOTICE 'V23: Fixed approval workflows seeded — 12 rejection + 6 auto + 12 manual = 30 total';

END $$;
