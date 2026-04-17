-- ============================================================================
-- V25: Re-seed Approval Workflows Using ALL 20 Condition Fields
-- ============================================================================
-- Deletes V22/V23 seeded workflows (keeps manually created ones)
-- Re-seeds 43 cases covering ALL 20 fields from approval_condition_field_definitions
--
-- Fields used:   dbr_percentage, financing_amount, monthly_salary, total_income,
--                disposable_income, simah_score, credit_score, risk_score, risk_type,
--                simah_max_dpd, simah_writeoff_count, simah_arrears_count,
--                simah_enquiry_count, customer_age, age_at_maturity, employment_type,
--                employment_months, employer_category, pep, sanction_match
--
-- Operators:     <=, >=, =, <, >
-- ============================================================================

DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_product_id UUID := '4210f18e-8c35-4b12-9b7f-c96de95767c7';
    v_wf_id UUID;
BEGIN

-- Delete old seeded workflows (keep manually created ones like "Auto Scenario")
DELETE FROM product_approval_workflows
WHERE product_id = v_product_id
  AND (name_en LIKE 'R%:%' OR name_en LIKE 'A%:%' OR name_en LIKE 'M%:%');

-- ============================================================================
-- REJECTION SCENARIOS (18 Cases) — Priority 1-18
-- ============================================================================

-- R1: SIMAH Stage 3 Default (90+ DPD)
-- Fields: simah_max_dpd
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R1: SIMAH Stage 3 Default', 'ر1: تعثر سمة المرحلة 3',
    'SAMA mandatory: Reject when SIMAH shows 90+ days past due', TRUE, 1, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_max_dpd', '>=', '90', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SIMAH.STAGE3_DEFAULT", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());

-- R2: Active Write-Off in SIMAH
-- Fields: simah_writeoff_count
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R2: Active Write-Off', 'ر2: شطب نشط في سمة',
    'SAMA mandatory: Reject when SIMAH has active write-off records', TRUE, 2, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_writeoff_count', '>', '0', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SIMAH.ACTIVE_WRITEOFF", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());

-- R3: SIMAH Score Below 500
-- Fields: simah_score
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R3: SIMAH Score Below 500', 'ر3: درجة سمة أقل من 500',
    'Reject when SIMAH credit score below 500', TRUE, 3, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_score', '<', '500', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.SIMAH.SCORE_BELOW_MINIMUM", "notify_customer": true}', 1, NOW(), NOW());

-- R4: DBR Exceeds SAMA 65% Cap
-- Fields: dbr_percentage
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R4: DBR Exceeds 65% SAMA Cap', 'ر4: نسبة عبء الدين تتجاوز 65%',
    'SAMA mandatory: Reject when DBR exceeds absolute 65% cap', TRUE, 4, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'dbr_percentage', '>', '65', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.DBR.ABSOLUTE_LIMIT_EXCEEDED", "notify_customer": true, "sama_mandatory": true}', 1, NOW(), NOW());

-- R5: Salary Below Minimum (3000 SAR)
-- Fields: monthly_salary
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R5: Salary Below 3000 SAR', 'ر5: الراتب أقل من 3000 ريال',
    'SAMA responsible lending: Reject when salary below 3000 SAR', TRUE, 5, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'monthly_salary', '<', '3000', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.MIN_SALARY", "notify_customer": true}', 1, NOW(), NOW());

-- R6: Customer Underage (< 21)
-- Fields: customer_age
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R6: Underage Applicant', 'ر6: مقدم طلب قاصر',
    'Reject when customer age below 21', TRUE, 6, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'customer_age', '<', '21', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.UNDERAGE", "notify_customer": true}', 1, NOW(), NOW());

-- R7: Age at Maturity > 60 (Employee)
-- Fields: age_at_maturity, employment_type
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R7: Employee Maturity Age > 60', 'ر7: عمر الموظف عند الاستحقاق > 60',
    'SAMA: Reject when non-retiree age at maturity exceeds 60', TRUE, 7, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'age_at_maturity', '>', '60', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', '=', 'PRIVATE_SECTOR', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.AGE_AT_MATURITY", "notify_customer": true}', 1, NOW(), NOW());

-- R8: Sanctions Match
-- Fields: sanction_match
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R8: Sanctions Match', 'ر8: مطابقة قوائم العقوبات',
    'SAMA AML: Reject on UN/OFAC/Local sanctions match', TRUE, 8, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'sanction_match', '=', 'true', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.AML.SANCTION_MATCH", "notify_customer": false, "report_to_compliance": true}', 1, NOW(), NOW());

-- R9: Critical Risk Score (> 80)
-- Fields: risk_score
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R9: Critical Risk Score', 'ر9: درجة مخاطر حرجة',
    'Reject when risk score exceeds 80 (CRITICAL)', TRUE, 9, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_score', '>', '80', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.RISK.CRITICAL_SCORE", "notify_customer": true}', 1, NOW(), NOW());

-- R10: Critical Risk Type
-- Fields: risk_type
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R10: Critical Risk Type', 'ر10: نوع مخاطر حرج',
    'Reject when risk type is CRITICAL', TRUE, 10, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_type', '=', 'CRITICAL', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.RISK.CRITICAL_TYPE", "notify_customer": true}', 1, NOW(), NOW());

-- R11: Loan Amount Exceeds Maximum (500K)
-- Fields: financing_amount
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R11: Amount Exceeds 500K', 'ر11: المبلغ يتجاوز 500 ألف',
    'Reject when loan amount exceeds product max 500,000 SAR', TRUE, 11, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'financing_amount', '>', '500000', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "PRODUCT.AMOUNT.EXCEEDS_MAXIMUM", "notify_customer": true}', 1, NOW(), NOW());

-- R12: Negative Disposable Income
-- Fields: disposable_income
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R12: Negative Disposable Income', 'ر12: دخل متاح سلبي',
    'SAMA: Reject when disposable income after loan is zero or negative', TRUE, 12, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'disposable_income', '<=', '0', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.AFFORDABILITY.NEGATIVE_DISPOSABLE", "notify_customer": true}', 1, NOW(), NOW());

-- R13: Credit Score Below Cutoff
-- Fields: credit_score
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R13: Credit Score Below 30', 'ر13: درجة الائتمان أقل من 30',
    'Reject when internal credit scorecard below decline cutoff', TRUE, 13, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'credit_score', '<', '30', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.SCORECARD.BELOW_CUTOFF", "notify_customer": true}', 1, NOW(), NOW());

-- R14: PEP + High Risk
-- Fields: pep, risk_type
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R14: PEP with High Risk', 'ر14: شخص سياسي مع مخاطر عالية',
    'Reject PEP when combined with HIGH risk type', TRUE, 14, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'pep', '=', 'true', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'HIGH', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.AML.PEP_HIGH_RISK", "report_to_compliance": true}', 1, NOW(), NOW());

-- R15: No Employment Stability + High Amount
-- Fields: employment_months, financing_amount
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R15: Short Employment + High Amount', 'ر15: توظيف قصير + مبلغ مرتفع',
    'Reject when employment < 3 months and amount > 50K', TRUE, 15, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'employment_months', '<', '3', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '>', '50000', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "SAMA.ELIGIBILITY.NO_EMPLOYMENT_STABILITY", "notify_customer": true}', 1, NOW(), NOW());

-- R16: Low SIMAH + High DBR
-- Fields: simah_score, dbr_percentage
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R16: Low SIMAH + High DBR', 'ر16: سمة منخفض + عبء دين مرتفع',
    'Reject when SIMAH < 550 and DBR > 50%', TRUE, 16, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '<', '550', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '>', '50', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND.LOW_SIMAH_HIGH_DBR", "notify_customer": true}', 1, NOW(), NOW());

-- R17: High Risk + Unknown Employer
-- Fields: risk_type, employer_category
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R17: High Risk + Unknown Employer', 'ر17: مخاطر عالية + جهة عمل مجهولة',
    'Reject when risk type HIGH and employer is UNKNOWN', TRUE, 17, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'risk_type', '=', 'HIGH', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employer_category', '=', 'UNKNOWN', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND.HIGH_RISK_UNKNOWN_EMPLOYER", "notify_customer": true}', 1, NOW(), NOW());

-- R18: Low Total Income + High Amount
-- Fields: total_income, financing_amount
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO',
    'R18: Low Income + High Amount', 'ر18: دخل منخفض + مبلغ مرتفع',
    'Reject when total income < 5000 and loan > 100K', TRUE, 18, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'total_income', '<', '5000', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '>', '100000', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'REJECT', '{"reason_code": "CREDIT.COMPOUND.LOW_INCOME_HIGH_AMOUNT", "notify_customer": true}', 1, NOW(), NOW());


-- ============================================================================
-- AUTO APPROVAL SCENARIOS (7 Cases) — Priority 19-25
-- ============================================================================

-- A1: Government Employee — Excellent Profile
-- Fields: simah_score, dbr_percentage, financing_amount, risk_type, credit_score, employment_type, simah_arrears_count
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A1: Govt Employee Excellent', 'م1: موظف حكومي ممتاز',
    'Auto approve: Govt employee, SIMAH >= 700, DBR <= 33%, amount <= 100K, low risk, no arrears', TRUE, 19, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '100000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'LOW', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '>=', '70', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', '=', 'GOVERNMENT', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_arrears_count', '=', '0', 7, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.GOVT_EXCELLENT", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());

-- A2: Military — Higher DBR Allowed (45%)
-- Fields: simah_score, dbr_percentage, financing_amount, employment_type, monthly_salary, risk_score, pep
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A2: Military Low Risk', 'م2: عسكري منخفض المخاطر',
    'Auto approve: Military, SIMAH >= 700, DBR <= 45%, salary >= 8K, not PEP', TRUE, 20, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '700', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '45', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '100000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_type', '=', 'MILITARY', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'monthly_salary', '>=', '8000', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', '<=', '30', 6, NOW(), NOW()),
    (default_tenant, v_wf_id, 'pep', '=', 'false', 7, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.MILITARY_LOW_RISK", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());

-- A3: Listed Private Sector
-- Fields: simah_score, dbr_percentage, employer_category, employment_months, credit_score, simah_writeoff_count
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A3: Listed Private Sector', 'م3: قطاع خاص مدرج',
    'Auto approve: Listed company, SIMAH >= 750, 6+ months, no write-offs', TRUE, 21, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '750', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employer_category', '=', 'LISTED', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employment_months', '>=', '6', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'credit_score', '>=', '75', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_writeoff_count', '=', '0', 6, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.LISTED_PRIVATE", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());

-- A4: Premium SIMAH (800+) — Any Employment
-- Fields: simah_score, dbr_percentage, financing_amount, simah_enquiry_count, sanction_match, disposable_income
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A4: Premium SIMAH 800+', 'م4: سمة ممتاز 800+',
    'Auto approve: SIMAH >= 800, DBR <= 33%, <= 3 enquiries, no sanctions, positive disposable', TRUE, 22, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'simah_score', '>=', '800', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'financing_amount', '<=', '200000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_enquiry_count', '<=', '3', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'sanction_match', '=', 'false', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'disposable_income', '>', '0', 6, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.PREMIUM_SIMAH", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());

-- A5: Small Loan (< 30K) — Relaxed
-- Fields: financing_amount, simah_score, dbr_percentage, risk_score, customer_age, simah_max_dpd
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A5: Small Loan < 30K', 'م5: تمويل صغير < 30 ألف',
    'Auto approve: Small amount <= 30K, SIMAH >= 650, risk <= 30, age >= 21, no DPD', TRUE, 23, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'financing_amount', '<=', '30000', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '>=', '650', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_score', '<=', '30', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'customer_age', '>=', '21', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_max_dpd', '=', '0', 6, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.SMALL_LOAN", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());

-- A6: Retiree with Verified Pension
-- Fields: employment_type, simah_score, dbr_percentage, age_at_maturity, monthly_salary, pep
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A6: Retiree Verified Pension', 'م6: متقاعد بمعاش مؤكد',
    'Auto approve: Retired, SIMAH >= 700, DBR <= 33%, maturity <= 70, salary >= 5K', TRUE, 24, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'employment_type', '=', 'RETIRED', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '>=', '700', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '33', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'age_at_maturity', '<=', '70', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'monthly_salary', '>=', '5000', 5, NOW(), NOW()),
    (default_tenant, v_wf_id, 'pep', '=', 'false', 6, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.RETIREE_PENSION", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());

-- A7: Very Low DBR + Approved Employer
-- Fields: dbr_percentage, employer_category, total_income, simah_score, risk_type
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL',
    'A7: Very Low DBR + Approved Employer', 'م7: دين منخفض جداً + جهة معتمدة',
    'Auto approve: DBR <= 20%, approved employer, income >= 10K, SIMAH >= 650', TRUE, 25, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES
    (default_tenant, v_wf_id, 'dbr_percentage', '<=', '20', 1, NOW(), NOW()),
    (default_tenant, v_wf_id, 'employer_category', '=', 'APPROVED', 2, NOW(), NOW()),
    (default_tenant, v_wf_id, 'total_income', '>=', '10000', 3, NOW(), NOW()),
    (default_tenant, v_wf_id, 'simah_score', '>=', '650', 4, NOW(), NOW()),
    (default_tenant, v_wf_id, 'risk_type', '=', 'LOW', 5, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'APPROVE', '{"reason_code": "AUTO.LOW_DBR_APPROVED_EMPLOYER", "notify_customer": true, "notify_email": "approvals@kfs.com"}', 1, NOW(), NOW());


-- ============================================================================
-- MANUAL REVIEW SCENARIOS (18 Cases) — Priority 26-43
-- ============================================================================

-- M1: Borderline SIMAH (500-699)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M1: Borderline SIMAH 500-699', 'ي1: سمة حدي 500-699', 'Manual: borderline SIMAH range', TRUE, 26, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_score', '>=', '500', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'simah_score', '<', '700', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.SIMAH.BORDERLINE"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'NOTIFY', '{"notify_email": "credit-committee@kfs.com", "escalation_hours": 24}', 2, NOW(), NOW());

-- M2: DBR Above Product Cap (33%-65%)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M2: DBR 33%-65%', 'ي2: نسبة دين 33%-65%', 'Manual: DBR above product cap within SAMA limit', TRUE, 27, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'dbr_percentage', '>', '33', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'dbr_percentage', '<=', '65', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.DBR.ABOVE_PRODUCT_CAP"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'NOTIFY', '{"notify_email": "credit-committee@kfs.com", "escalation_hours": 24}', 2, NOW(), NOW());

-- M3: Stage 2 Delinquency (31-89 DPD)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M3: SIMAH Stage 2 (31-89 DPD)', 'ي3: تعثر المرحلة 2', 'SAMA: Stage 2 requires manager approval', TRUE, 28, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_max_dpd', '>=', '31', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'simah_max_dpd', '<', '90', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.SIMAH.STAGE2"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M4: PEP — Enhanced Due Diligence
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M4: PEP Enhanced Due Diligence', 'ي4: عناية واجبة معززة', 'SAMA AML: EDD for PEP', TRUE, 29, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'pep', '=', 'true', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "COMPLIANCE_OFFICER", "reason_code": "MANUAL.PEP.EDD"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "HEAD_OF_COMPLIANCE", "escalation_hours": 48}', 2, NOW(), NOW());

-- M5: Self-Employed
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M5: Self-Employed', 'ي5: عمل حر', 'Manual: variable income needs verification', TRUE, 30, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'employment_type', '=', 'SELF_EMPLOYED', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.SELF_EMPLOYED", "require_documents": ["BANK_STATEMENT_6M","TAX_RETURN","CR_CERTIFICATE"]}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M6: High Amount (100K-300K)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M6: High Amount 100K-300K', 'ي6: مبلغ مرتفع 100-300 ألف', 'Manual: senior credit review', TRUE, 31, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'financing_amount', '>', '100000', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'financing_amount', '<=', '300000', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "SENIOR_CREDIT", "reason_code": "MANUAL.AMOUNT.HIGH"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "CREDIT_COMMITTEE", "escalation_hours": 24}', 2, NOW(), NOW());

-- M7: Very High Amount (> 300K) — Committee
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M7: Very High Amount > 300K', 'ي7: مبلغ > 300 ألف', 'Committee approval required', TRUE, 32, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'financing_amount', '>', '300000', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_COMMITTEE", "reason_code": "MANUAL.AMOUNT.COMMITTEE"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "GM_CREDIT", "escalation_hours": 48}', 2, NOW(), NOW());

-- M8: High Risk Type
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M8: High Risk Type', 'ي8: نوع مخاطر مرتفع', 'Manual: officer assessment needed', TRUE, 33, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_type', '=', 'HIGH', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.RISK.HIGH"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M9: Elevated Risk Score (31-80)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M9: Risk Score 31-80', 'ي9: درجة مخاطر 31-80', 'Manual: MEDIUM-HIGH risk', TRUE, 34, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_score', '>', '30', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'risk_score', '<=', '80', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.RISK_SCORE.ELEVATED"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M10: Unknown Employer
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M10: Unknown Employer', 'ي10: جهة عمل غير معروفة', 'Manual: employer not in approved list', TRUE, 35, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'employer_category', '=', 'UNKNOWN', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.EMPLOYER.UNKNOWN", "require_documents": ["EMPLOYMENT_LETTER","GOSI_CERTIFICATE"]}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M11: Short Employment (3-6 months)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M11: Employment 3-6 Months', 'ي11: توظيف 3-6 أشهر', 'Manual: probation period', TRUE, 36, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'employment_months', '>=', '3', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'employment_months', '<', '6', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.EMPLOYMENT.SHORT_TENURE"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M12: Loan Shopping (> 5 enquiries in 90 days)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M12: Loan Shopping (> 5 Enquiries)', 'ي12: تسوق قروض', 'SAMA flag: multiple enquiries', TRUE, 37, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_enquiry_count', '>', '5', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.SIMAH.LOAN_SHOPPING"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M13: Accounts in Arrears
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M13: Accounts in Arrears', 'ي13: حسابات متأخرة', 'Manual: has accounts in arrears', TRUE, 38, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'simah_arrears_count', '>', '0', 1, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.SIMAH.ARREARS"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M14: Borderline Credit Score (30-59)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M14: Credit Score 30-59', 'ي14: درجة ائتمان 30-59', 'Manual: borderline scorecard', TRUE, 39, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'credit_score', '>=', '30', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'credit_score', '<', '60', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.CREDIT_SCORE.BORDERLINE"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M15: Medium Risk + Significant Amount
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M15: Medium Risk + Amount > 50K', 'ي15: مخاطر متوسطة + مبلغ كبير', 'Manual: compound amber', TRUE, 40, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'risk_type', '=', 'MEDIUM', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'financing_amount', '>', '50000', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.MEDIUM_RISK_HIGH_AMOUNT"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M16: Low Salary + High DBR
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M16: Salary < 8K + DBR > 40%', 'ي16: راتب < 8 آلاف + دين > 40%', 'Manual: stretched capacity', TRUE, 41, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'monthly_salary', '<', '8000', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'dbr_percentage', '>', '40', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "UNDERWRITER", "reason_code": "MANUAL.LOW_SALARY_HIGH_DBR"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M17: Low Disposable Income (< 2000 SAR)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M17: Low Disposable Income', 'ي17: دخل متاح منخفض', 'Manual: tight after-loan budget', TRUE, 42, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'disposable_income', '>', '0', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'disposable_income', '<', '2000', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.LOW_DISPOSABLE_INCOME"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

-- M18: Near Maturity Age (55-60 for employees)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL',
    'M18: Near Maturity Age 55-60', 'ي18: قرب سن الاستحقاق 55-60', 'Manual: close to retirement age', TRUE, 43, NOW(), NOW(), 1)
RETURNING id INTO v_wf_id;
INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'age_at_maturity', '>=', '55', 1, NOW(), NOW()), (default_tenant, v_wf_id, 'age_at_maturity', '<=', '60', 2, NOW(), NOW());
INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
VALUES (default_tenant, v_wf_id, 'ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_OFFICER", "reason_code": "MANUAL.NEAR_MATURITY_AGE"}', 1, NOW(), NOW()),
       (default_tenant, v_wf_id, 'ESCALATE', '{"escalate_to": "SENIOR_CREDIT", "escalation_hours": 24}', 2, NOW(), NOW());

RAISE NOTICE 'V25: Complete approval workflows seeded — 18 rejection + 7 auto + 18 manual = 43 total using ALL 20 fields';
END $$;
