-- ============================================================================
-- V22: Seed SAMA-Compliant Approval Workflow Cases
-- ============================================================================
-- Seeds all approval workflow scenarios for the Personal Financing product:
--   - 18 Rejection Cases (Auto Reject — SAMA mandatory blocks)
--   -  7 Auto Approval Cases (Low risk — all green)
--   - 18 Manual Review Cases (Amber zone — officer discretion)
--   Total: 43 workflow rules
--
-- Tables: product_approval_workflows, product_approval_conditions, product_approval_actions
-- Does NOT touch credit_scoring tables
-- ============================================================================

DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_product_id UUID;
    v_wf_id UUID;

BEGIN

-- ============================================================================
-- LOOK UP PRODUCT
-- ============================================================================

SELECT id INTO v_product_id
FROM products
WHERE tenant_id = default_tenant AND product_code = 'MICRO-001';

IF v_product_id IS NULL THEN
    RAISE NOTICE 'Product MICRO-001 not found — skipping approval workflow seed';
    RETURN;
END IF;

-- ============================================================================
-- ██████╗ ███████╗     ██╗███████╗ ██████╗████████╗██╗ ██████╗ ███╗   ██╗
-- ██╔══██╗██╔════╝     ██║██╔════╝██╔════╝╚══██╔══╝██║██╔═══██╗████╗  ██║
-- ██████╔╝█████╗       ██║█████╗  ██║        ██║   ██║██║   ██║██╔██╗ ██║
-- ██╔══██╗██╔══╝  ██   ██║██╔══╝  ██║        ██║   ██║██║   ██║██║╚██╗██║
-- ██║  ██║███████╗╚█████╔╝███████╗╚██████╗   ██║   ██║╚██████╔╝██║ ╚████║
-- ╚═╝  ╚═╝╚══════╝ ╚════╝ ╚══════╝ ╚═════╝   ╚═╝   ╚═╝ ╚═════╝ ╚═╝  ╚═══╝
-- REJECTION SCENARIOS (18 Cases) — Priority evaluated FIRST
-- SAMA Mandatory: These CANNOT be overridden by any officer
-- ============================================================================

-- ── R1: Simah Stage 3 Default (90+ DPD) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R1: Simah Stage 3 Default', 'ر1: تعثر سمة المرحلة 3',
    'SAMA mandatory: Auto reject when Simah shows 90+ days past due (Stage 3 default)',
    TRUE, 1, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_max_dpd', 'GTE', '90', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SIMAH.STAGE3_DEFAULT", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R2: Active Write-Off in Simah ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R2: Active Write-Off', 'ر2: شطب نشط في سمة',
    'SAMA mandatory: Auto reject when Simah report contains active write-off records',
    TRUE, 2, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_writeoff_count', 'GT', '0', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SIMAH.ACTIVE_WRITEOFF", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R3: Simah Score Below Minimum ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R3: Simah Score Below Minimum', 'ر3: درجة سمة أقل من الحد الأدنى',
    'Auto reject when Simah credit score is below 500 (high risk threshold)',
    TRUE, 3, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_score', 'LT', '500', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SIMAH.SCORE_BELOW_MINIMUM", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R4: DBR Exceeds SAMA Absolute Cap (65%) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R4: DBR Exceeds SAMA Cap 65%', 'ر4: نسبة عبء الدين تتجاوز سقف ساما 65%',
    'SAMA mandatory: Auto reject when Debt Burden Ratio exceeds absolute SAMA cap of 65%',
    TRUE, 4, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'dbr_after', 'GT', '65', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.DBR.ABSOLUTE_LIMIT_EXCEEDED", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R5: Salary Below SAMA Minimum (3000 SAR) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R5: Salary Below SAMA Minimum', 'ر5: الراتب أقل من الحد الأدنى لساما',
    'SAMA responsible lending: Auto reject when monthly salary is below 3,000 SAR minimum',
    TRUE, 5, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'monthly_salary', 'LT', '3000', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.MIN_SALARY", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R6: Age Below Minimum (21) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R6: Underage Applicant', 'ر6: مقدم طلب قاصر',
    'Auto reject when customer age is below 21 years (product minimum age requirement)',
    TRUE, 6, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'customer_age', 'LT', '21', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.UNDERAGE", "notify_customer": true}', 1, NOW(), NOW());


-- ── R7: Age at Maturity Exceeds Limit — Employee (60) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R7: Employee Age at Maturity > 60', 'ر7: عمر الموظف عند الاستحقاق > 60',
    'SAMA: Auto reject when employee age at loan maturity exceeds 60 years',
    TRUE, 7, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'age_at_maturity', 'GT', '60', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', 'NEQ', 'RETIRED', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.AGE_AT_MATURITY", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R8: Age at Maturity Exceeds Limit — Retiree (70) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R8: Retiree Age at Maturity > 70', 'ر8: عمر المتقاعد عند الاستحقاق > 70',
    'SAMA: Auto reject when retiree age at loan maturity exceeds 70 years',
    TRUE, 8, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'age_at_maturity', 'GT', '70', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', 'EQ', 'RETIRED', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.RETIREE_AGE_LIMIT", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R9: Sanctions List Match (AML) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R9: Sanctions Match', 'ر9: مطابقة قوائم العقوبات',
    'SAMA AML: Auto reject when applicant matches UN/OFAC/Local sanctions list',
    TRUE, 9, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'sanction_match', 'EQ', 'true', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.AML.SANCTION_MATCH", "notify_customer": false, "sama_mandatory": true, "report_to_compliance": true}', 1, NOW(), NOW());


-- ── R10: Fraud Detected (High Severity) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R10: Fraud Detected', 'ر10: اكتشاف احتيال',
    'Auto reject when high-severity fraud signals are detected by risk engine',
    TRUE, 10, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'fraud_detected', 'EQ', 'true', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.FRAUD.DETECTED", "notify_customer": false, "sama_mandatory": true, "report_to_compliance": true}', 1, NOW(), NOW());


-- ── R11: Risk Score Critical (> 80) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R11: Critical Risk Score', 'ر11: درجة مخاطر حرجة',
    'Auto reject when risk assessment score exceeds 80 (CRITICAL level)',
    TRUE, 11, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_score', 'GT', '80', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.RISK.CRITICAL_SCORE", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R12: NID/Iqama Blacklisted ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R12: Blacklisted Entity', 'ر12: كيان في القائمة السوداء',
    'Auto reject when National ID or Iqama is in internal fraud blacklist',
    TRUE, 12, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'is_blacklisted', 'EQ', 'true', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.FRAUD.BLACKLISTED_ENTITY", "notify_customer": false, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R13: Device Fraud (3+ NIDs per Device) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R13: Device Identity Farming', 'ر13: زراعة هوية الجهاز',
    'Auto reject when device fingerprint is associated with 3+ different National IDs',
    TRUE, 13, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'device_nid_count', 'GTE', '3', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.FRAUD.DEVICE_FARMING", "notify_customer": false, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R14: Non-KSA Access (VPN/Proxy/Outside KSA) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R14: Non-KSA Access', 'ر14: وصول من خارج المملكة',
    'SAMA data residency: Auto reject when access is from outside KSA or VPN/proxy detected',
    TRUE, 14, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'is_ksa_access', 'EQ', 'false', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SECURITY.NON_KSA_ACCESS", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R15: Application Velocity Exceeded (3+/day) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R15: Velocity Exceeded', 'ر15: تجاوز سرعة التقديم',
    'Auto reject when applicant submits more than 3 applications per day (per NID)',
    TRUE, 15, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'applications_today', 'GT', '3', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.FRAUD.VELOCITY_EXCEEDED", "notify_customer": true}', 1, NOW(), NOW());


-- ── R16: Negative Disposable Income ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R16: Negative Disposable Income', 'ر16: دخل متاح سلبي',
    'SAMA responsible lending: Auto reject when disposable income after loan is negative',
    TRUE, 16, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'disposable_income_after', 'LTE', '0', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.AFFORDABILITY.NEGATIVE_DISPOSABLE", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());


-- ── R17: Internal Scorecard Below Decline Cutoff ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R17: Scorecard Below Cutoff', 'ر17: بطاقة الأداء أقل من الحد',
    'Auto reject when internal credit scorecard result falls below decline cutoff threshold',
    TRUE, 17, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'internal_score_result', 'EQ', 'DECLINE', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.SCORECARD.BELOW_CUTOFF", "notify_customer": true}', 1, NOW(), NOW());


-- ── R18: No Employment Stability + High Amount ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R18: No Employment Stability', 'ر18: عدم استقرار وظيفي',
    'Auto reject when employment is less than 3 months and financing amount exceeds 50,000 SAR',
    TRUE, 18, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'employment_months', 'LT', '3', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'GT', '50000', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.NO_EMPLOYMENT_STABILITY", "notify_customer": true}', 1, NOW(), NOW());


-- ============================================================================
--  █████╗ ██╗   ██╗████████╗ ██████╗
-- ██╔══██╗██║   ██║╚══██╔══╝██╔═══██╗
-- ███████║██║   ██║   ██║   ██║   ██║
-- ██╔══██║██║   ██║   ██║   ██║   ██║
-- ██║  ██║╚██████╔╝   ██║   ╚██████╔╝
-- ╚═╝  ╚═╝ ╚═════╝    ╚═╝    ╚═════╝
-- AUTO APPROVAL SCENARIOS (7 Cases) — Priority evaluated SECOND
-- All conditions in each case must be TRUE simultaneously
-- ============================================================================

-- ── A1: Government Employee — Low Risk ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A1: Government Employee Low Risk', 'م1: موظف حكومي منخفض المخاطر',
    'Auto approve government employees with high Simah score, adequate salary, clean history, and moderate amount',
    TRUE, 19, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'monthly_salary', 'GTE', '10000', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'LTE', '100000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '33', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', 'EQ', 'GOVERNMENT', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_accounts_in_arrears', 'EQ', '0', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '30', 7, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.GOVT_LOW_RISK", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A2: Military Employee — Low Risk ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A2: Military Employee Low Risk', 'م2: موظف عسكري منخفض المخاطر',
    'Auto approve military personnel with high Simah score, DBR within 45% military cap, clean history',
    TRUE, 20, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'monthly_salary', 'GTE', '8000', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'LTE', '100000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '45', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', 'EQ', 'MILITARY', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_accounts_in_arrears', 'EQ', '0', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '30', 7, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.MILITARY_LOW_RISK", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A3: Government + Salary Assignment (Higher DBR 40%) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A3: Govt + Salary Assignment', 'م3: حكومي + تحويل راتب',
    'Auto approve government employees with salary assignment — DBR allowed up to 40%',
    TRUE, 21, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'monthly_salary', 'GTE', '10000', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'LTE', '150000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '40', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', 'EQ', 'GOVERNMENT', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'has_salary_assignment', 'EQ', 'true', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_accounts_in_arrears', 'EQ', '0', 7, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '30', 8, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.GOVT_SALARY_ASSIGNMENT", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A4: Listed Private Sector Company ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A4: Listed Private Sector', 'م4: قطاع خاص مدرج',
    'Auto approve private sector employees from listed companies with excellent score and 6+ months tenure',
    TRUE, 22, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '750', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'monthly_salary', 'GTE', '15000', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'LTE', '100000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '33', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', 'EQ', 'PRIVATE_SECTOR', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employer_category', 'EQ', 'LISTED', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_accounts_in_arrears', 'EQ', '0', 7, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '30', 8, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_months', 'GTE', '6', 9, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.LISTED_PRIVATE_LOW_RISK", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A5: Existing Customer — Small Amount, Clean History ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A5: Existing Customer Small Amount', 'م5: عميل حالي مبلغ صغير',
    'Auto approve existing customers with clean internal repayment history and small financing amount',
    TRUE, 23, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '650', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'is_existing_customer', 'EQ', 'true', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'internal_repayment_history', 'EQ', 'CLEAN', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'LTE', '50000', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '33', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_accounts_in_arrears', 'EQ', '0', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '30', 7, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.EXISTING_CUSTOMER_CLEAN", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A6: Retiree with GOSI-Verified Pension ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A6: Retiree Verified Pension', 'م6: متقاعد بمعاش تقاعدي مؤكد',
    'Auto approve retirees with GOSI-verified pension, high Simah score, and age at maturity within 70',
    TRUE, 24, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', 'EQ', 'RETIRED', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'pension_verified', 'EQ', 'true', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'LTE', '80000', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '33', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'age_at_maturity', 'LTE', '70', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_accounts_in_arrears', 'EQ', '0', 7, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '30', 8, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.RETIREE_VERIFIED_PENSION", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ── A7: Saudi Citizen — Very High Income, Excellent Score ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A7: Citizen Premium Profile', 'م7: مواطن بملف متميز',
    'Auto approve Saudi citizens with excellent Simah score 800+, high income 30K+, and very low risk',
    TRUE, 25, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '800', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'customer_type', 'EQ', 'CITIZEN', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'monthly_salary', 'GTE', '30000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '33', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_accounts_in_arrears', 'EQ', '0', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '20', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'enquiry_count_90days', 'LTE', '3', 7, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.CITIZEN_PREMIUM_PROFILE", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ============================================================================
-- ███╗   ███╗ █████╗ ███╗   ██╗██╗   ██╗ █████╗ ██╗
-- ████╗ ████║██╔══██╗████╗  ██║██║   ██║██╔══██╗██║
-- ██╔████╔██║███████║██╔██╗ ██║██║   ██║███████║██║
-- ██║╚██╔╝██║██╔══██║██║╚██╗██║██║   ██║██╔══██║██║
-- ██║ ╚═╝ ██║██║  ██║██║ ╚████║╚██████╔╝██║  ██║███████╗
-- ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝
-- MANUAL REVIEW SCENARIOS (18 Cases) — Priority evaluated LAST
-- Officer discretion: trained underwriter reviews and decides
-- ============================================================================

-- ── M1: Borderline Simah Score (500-699) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M1: Borderline Simah Score', 'ي1: درجة سمة حدية',
    'Manual review when Simah credit score is in borderline range 500-699',
    TRUE, 26, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', 'GTE', '500', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', 'LT', '700', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.SIMAH.BORDERLINE_SCORE"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'NOTIFY', '{"notify_email": "credit-committee@kfs.com", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M2: DBR Above Product Cap but Within SAMA Cap (33%-65%) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M2: DBR Above Product Cap', 'ي2: نسبة عبء الدين فوق حد المنتج',
    'Manual review when DBR exceeds product cap (33%) but is within SAMA absolute cap (65%)',
    TRUE, 27, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'dbr_after', 'GT', '33', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_after', 'LTE', '65', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.DBR.ABOVE_PRODUCT_CAP"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'NOTIFY', '{"notify_email": "credit-committee@kfs.com", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M3: Simah Stage 2 Delinquency (31-90 DPD) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M3: Stage 2 Delinquency', 'ي3: تعثر المرحلة 2',
    'SAMA: Manual review required when Simah shows Stage 2 delinquency (31-90 days past due)',
    TRUE, 28, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_max_dpd', 'GTE', '31', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_max_dpd', 'LT', '90', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.SIMAH.STAGE2_DELINQUENCY"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M4: PEP Detected — Enhanced Due Diligence ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M4: PEP Enhanced Due Diligence', 'ي4: العناية الواجبة المعززة لشخص سياسي',
    'SAMA AML: Manual review with Enhanced Due Diligence when Politically Exposed Person is detected',
    TRUE, 29, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'is_pep', 'EQ', 'true', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "COMPLIANCE_OFFICER", "reason_code": "MANUAL.PEP.EDD_REQUIRED", "require_document": "EDD_REPORT"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "HEAD_OF_COMPLIANCE", "escalation_hours": 48}', 2, NOW(), NOW());


-- ── M5: Self-Employed Applicant ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M5: Self-Employed', 'ي5: عمل حر',
    'SAMA responsible lending: Manual review for self-employed applicants — variable income needs verification',
    TRUE, 30, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'employment_type', 'EQ', 'SELF_EMPLOYED', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.EMPLOYMENT.SELF_EMPLOYED", "require_documents": ["BANK_STATEMENT_6M", "TAX_RETURN", "CR_CERTIFICATE"]}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M6: Amount Exceeds Auto-Approval Limit (100K-300K) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M6: High Amount (100K-300K)', 'ي6: مبلغ مرتفع (100ألف-300ألف)',
    'Manual review when financing amount exceeds auto-approval limit but is within senior credit authority',
    TRUE, 31, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'financing_amount', 'GT', '100000', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'LTE', '300000', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "SENIOR_CREDIT", "reason_code": "MANUAL.AMOUNT.EXCEEDS_AUTO_LIMIT"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "CREDIT_COMMITTEE", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M7: Very High Amount (> 300K) — Credit Committee ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M7: Very High Amount (>300K)', 'ي7: مبلغ مرتفع جداً (>300ألف)',
    'SAMA prudential: Credit committee approval required for financing exceeding 300,000 SAR',
    TRUE, 32, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'financing_amount', 'GT', '300000', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_COMMITTEE", "reason_code": "MANUAL.AMOUNT.COMMITTEE_REQUIRED"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "GM_CREDIT", "escalation_hours": 48}', 2, NOW(), NOW());


-- ── M8: Disputed Simah Records ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M8: Disputed Simah Records', 'ي8: سجلات سمة متنازع عليها',
    'Manual review when Simah report contains disputed records requiring officer verification',
    TRUE, 33, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_disputed_count', 'GT', '0', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.SIMAH.DISPUTED_RECORDS", "require_documents": ["DISPUTE_EVIDENCE"]}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M9: New Customer + High Exposure (> 50K) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M9: New Customer High Exposure', 'ي9: عميل جديد تعرض مرتفع',
    'Manual review for first-time customers requesting more than 50,000 SAR — no internal track record',
    TRUE, 34, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'is_existing_customer', 'EQ', 'false', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', 'GT', '50000', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.CUSTOMER.NEW_HIGH_EXPOSURE"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M10: Employer Not in Approved List ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M10: Unknown Employer', 'ي10: جهة عمل غير معروفة',
    'Manual review when employer is not in the approved employer list — salary transfer reliability unclear',
    TRUE, 35, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'employer_category', 'EQ', 'UNKNOWN', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.EMPLOYER.NOT_APPROVED", "require_documents": ["EMPLOYMENT_LETTER", "GOSI_CERTIFICATE"]}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M11: Short Employment Tenure (3-6 months) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M11: Short Employment Tenure', 'ي11: فترة توظيف قصيرة',
    'Manual review when employment tenure is 3-6 months — probation period risk',
    TRUE, 36, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'employment_months', 'GTE', '3', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_months', 'LT', '6', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.EMPLOYMENT.SHORT_TENURE", "require_documents": ["EMPLOYMENT_LETTER", "GOSI_CERTIFICATE"]}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M12: Salary Mismatch (> 15%) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M12: Salary Mismatch', 'ي12: تباين في الراتب',
    'SAMA income verification: Manual review when declared salary differs from bank statement by more than 15%',
    TRUE, 37, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'salary_mismatch_percentage', 'GT', '15', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.INCOME.SALARY_MISMATCH", "require_documents": ["BANK_STATEMENT_6M", "SALARY_CERTIFICATE"]}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M13: Expat with Short Iqama Validity (< 12 months) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M13: Short Iqama Validity', 'ي13: صلاحية إقامة قصيرة',
    'Manual review for expat residents when Iqama remaining validity is less than 12 months',
    TRUE, 38, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'customer_type', 'EQ', 'RESIDENT', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'iqama_remaining_months', 'LT', '12', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.RESIDENCY.SHORT_IQAMA"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M14: Loan Shopping (> 5 Enquiries in 90 days) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M14: Loan Shopping', 'ي14: تسوق القروض',
    'SAMA flag: Manual review when more than 5 credit enquiries in past 90 days — potential distress signal',
    TRUE, 39, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'enquiry_count_90days', 'GT', '5', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.SIMAH.LOAN_SHOPPING"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M15: Risk Score Elevated (31-80) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M15: Elevated Risk Score', 'ي15: درجة مخاطر مرتفعة',
    'Manual review when risk assessment score is in MEDIUM-HIGH range (31-80)',
    TRUE, 40, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'risk_score', 'GT', '30', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', 'LTE', '80', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.RISK.ELEVATED_SCORE"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M16: Existing Restructured Loan ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M16: Restructured Loan History', 'ي16: تاريخ إعادة هيكلة قرض',
    'Manual review when applicant has an existing restructured loan — indicates past financial difficulty',
    TRUE, 41, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'has_restructured_loan', 'EQ', 'true', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "SENIOR_CREDIT", "reason_code": "MANUAL.HISTORY.RESTRUCTURED_LOAN"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "CREDIT_COMMITTEE", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M17: Tenure Exceeds Iqama Validity (Expat) ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M17: Tenure > Iqama Validity', 'ي17: المدة تتجاوز صلاحية الإقامة',
    'Manual review for expats when requested loan tenure exceeds Iqama remaining validity period',
    TRUE, 42, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'customer_type', 'EQ', 'RESIDENT', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'tenure_exceeds_iqama', 'EQ', 'true', 2, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.RESIDENCY.TENURE_EXCEEDS_IQAMA"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ── M18: High Sector Risk Employer ──
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M18: High Sector Risk', 'ي18: مخاطر قطاع مرتفعة',
    'Manual review when employer operates in high-risk sector (construction, real estate, hospitality)',
    TRUE, 43, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'employer_sector_risk', 'EQ', 'HIGH', 1, NOW(), NOW());

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.EMPLOYER.HIGH_SECTOR_RISK"}', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());


-- ============================================================================
-- SUMMARY
-- ============================================================================
-- Total workflows seeded: 43
--   Rejection Scenarios (R1-R18):   18 cases — Priority 1-18
--   Auto Approval (A1-A7):           7 cases — Priority 19-25
--   Manual Review (M1-M18):         18 cases — Priority 26-43
--
-- Evaluation order:
--   1. Check ALL rejection rules (any match → AUTO REJECT)
--   2. Check ALL auto-approval rules (any full match → AUTO APPROVE)
--   3. Check ALL manual rules (any match → MANUAL REVIEW with reason codes)
--   4. Default fallback → MANUAL REVIEW (safety net)
-- ============================================================================

RAISE NOTICE 'SAMA approval workflow cases seeded: 18 rejection + 7 auto-approval + 18 manual = 43 total';

END $$;
