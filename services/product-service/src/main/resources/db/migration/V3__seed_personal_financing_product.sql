-- ============================================================================
-- V3: Seed a fully configured "Personal Financing" product under Tawarruq
-- ============================================================================

DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
    seed_user_id UUID := '00000000-0000-0000-0000-000000000099';

    -- Looked-up IDs
    tawarruq_cat_id UUID;
    personal_fin_sub_id UUID;
    v_product_id UUID;

    -- Credit scoring field definition IDs
    cs_customer_type UUID;
    cs_age UUID;
    cs_salary UUID;
    cs_simah_score UUID;
    cs_employment UUID;

BEGIN

-- ============================================================================
-- LOOK UP CATEGORY IDs
-- ============================================================================

SELECT id INTO tawarruq_cat_id
FROM product_master_categories
WHERE tenant_id = default_tenant AND code = 'TAWARRUQ';

SELECT id INTO personal_fin_sub_id
FROM product_sub_categories
WHERE tenant_id = default_tenant AND master_category_id = tawarruq_cat_id AND code = 'PERSONAL_FINANCING';

-- ============================================================================
-- STEP 1: CREATE THE PRODUCT
-- ============================================================================

INSERT INTO products (
    id, tenant_id, product_code, name_en, name_ar,
    description_en, description_ar,
    short_description_en, short_description_ar,
    product_type, target_segment, sharia_structure,
    master_category_id, sub_category_id,
    min_amount, max_amount, min_tenure_months, max_tenure_months,
    allowed_tenures, base_profit_rate, rate_type, repayment_frequency,
    grace_period_days, early_settlement_allowed, waive_unearned_profit,
    min_tenure_before_settlement,
    currency, notification_email,
    customer_types, involves_commodity, setup_method,
    commodity_required, visible_to_customers, visible_to_partners,
    wizard_step, wizard_completed,
    status, created_by, created_at, updated_at, version
)
VALUES (
    gen_random_uuid(), default_tenant, 'PF-001',
    'Personal Financing', 'التمويل الشخصي',
    'Tawarruq-based personal financing product for salaried individuals in the Kingdom of Saudi Arabia. Offers competitive profit rates with flexible tenure options and Sharia-compliant commodity structure.',
    'منتج تمويل شخصي قائم على التورق للموظفين في المملكة العربية السعودية. يقدم معدلات ربح تنافسية مع خيارات مدد مرنة وهيكل سلعي متوافق مع الشريعة.',
    'Sharia-compliant personal financing for salaried individuals',
    'تمويل شخصي متوافق مع الشريعة للموظفين',
    'TAWARRUQ', 'INDIVIDUAL', 'TAWARRUQ',
    tawarruq_cat_id, personal_fin_sub_id,
    5000.0000, 500000.0000, 6, 60,
    ARRAY[6, 12, 24, 36, 48, 60], 0.089900, 'REDUCING_BALANCE', 'MONTHLY',
    3, TRUE, TRUE, 3,
    'SAR', 'product-alerts@kfs.com',
    ARRAY['INDIVIDUAL']::VARCHAR(50)[], TRUE, 'CUSTOM',
    TRUE, FALSE, TRUE,
    5, TRUE,
    'DRAFT', seed_user_id, NOW(), NOW(), 1
)
RETURNING id INTO v_product_id;

-- ============================================================================
-- STEP 2: COMMODITY CONFIG (Tawarruq requires commodity)
-- ============================================================================

INSERT INTO product_commodity_configs (tenant_id, product_id, funding_types, created_at, updated_at, version)
VALUES (default_tenant, v_product_id, ARRAY['COMPANY_BACKED']::VARCHAR(50)[], NOW(), NOW(), 1);

-- ============================================================================
-- STEP 3: SETTINGS — All 8 tabs
-- ============================================================================

-- Tab 1: Application Steps (5 steps for the customer journey)
INSERT INTO product_application_steps (tenant_id, product_id, step_number, title_en, title_ar, description, is_required, sort_order, created_at, updated_at, version)
VALUES
    (default_tenant, v_product_id, 1, 'Identity Verification', 'التحقق من الهوية',
     'Verify customer identity via Nafath and Yakeen', TRUE, 1, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 2, 'Income Assessment', 'تقييم الدخل',
     'Verify salary and employment through GOSI/employer', TRUE, 2, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 3, 'Credit Check', 'فحص الائتمان',
     'Run SIMAH credit bureau check and calculate DBR', TRUE, 3, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 4, 'Offer Selection', 'اختيار العرض',
     'Present financing offers and let customer choose tenure', TRUE, 4, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 5, 'Contract Signing', 'توقيع العقد',
     'Digital contract signing and commodity purchase execution', TRUE, 5, NOW(), NOW(), 1);

-- Tab 2: Terms & Conditions
INSERT INTO product_terms_conditions (tenant_id, product_id, terms_en, terms_ar, created_at, updated_at, version)
VALUES (
    default_tenant, v_product_id,
    'By proceeding with this Personal Financing application, you agree to the following terms and conditions:

1. This financing facility is structured as a Tawarruq (commodity-based) transaction in compliance with Sharia principles approved by the Sharia Board.

2. The customer acknowledges that the financier will purchase commodities on behalf of the customer, who then authorizes the financier to sell them on their behalf to obtain the financing amount.

3. Monthly installments are calculated on a reducing balance basis and include both principal and profit components.

4. Early settlement is permitted after a minimum of 3 months from disbursement. Unearned profit will be waived in accordance with SAMA guidelines.

5. Late payment penalties will be donated to a charitable fund as per Sharia compliance requirements.

6. The customer is required to maintain adequate insurance coverage as specified in the financing agreement.

7. All disputes shall be governed by the laws of the Kingdom of Saudi Arabia and subject to the jurisdiction of the competent courts.',

    'بالمتابعة في طلب التمويل الشخصي هذا، توافق على الشروط والأحكام التالية:

1. تم هيكلة هذا التمويل على أساس التورق (المعاملة القائمة على السلع) وفقاً لمبادئ الشريعة الإسلامية المعتمدة من الهيئة الشرعية.

2. يقر العميل بأن الممول سيقوم بشراء سلع نيابة عن العميل، الذي يفوض الممول ببيعها نيابة عنه للحصول على مبلغ التمويل.

3. يتم احتساب الأقساط الشهرية على أساس الرصيد المتناقص وتشمل مكونات الأصل والربح.

4. يُسمح بالسداد المبكر بعد مرور 3 أشهر على الأقل من الصرف. سيتم التنازل عن الأرباح غير المكتسبة وفقاً لتوجيهات ساما.

5. سيتم التبرع بغرامات التأخر في السداد لصندوق خيري وفقاً لمتطلبات الامتثال الشرعي.

6. يُطلب من العميل الحفاظ على تغطية تأمينية كافية كما هو محدد في اتفاقية التمويل.

7. تخضع جميع النزاعات لقوانين المملكة العربية السعودية واختصاص المحاكم المختصة.',
    NOW(), NOW(), 1
);

-- Tab 3: Fee Settings
INSERT INTO product_fee_settings (tenant_id, product_id, min_financing_amount, max_financing_amount,
    vat_percentage, revenue_eligibility_threshold, max_dbr_percentage, dbr_calculation_method, dbr_exceptions,
    created_at, updated_at, version)
VALUES (
    default_tenant, v_product_id,
    5000.0000, 500000.0000,
    15.00, 3000.0000, 33.00, 'GROSS_INCOME',
    'Military personnel: DBR up to 45%. Government employees with salary assignment: DBR up to 40%.',
    NOW(), NOW(), 1
);

-- Tab 4: Admin Fee Slabs (3 tiers)
INSERT INTO product_admin_fee_slabs (tenant_id, product_id, min_amount, max_amount, profit_percentage, processing_fee, partner_scope, status, sort_order, created_at, updated_at, version)
VALUES
    (default_tenant, v_product_id, 5000.0000, 50000.0000, 1.2500, 500.0000, 'ALL_PARTNERS', 'ACTIVE', 1, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 50000.0001, 200000.0000, 1.0000, 750.0000, 'ALL_PARTNERS', 'ACTIVE', 2, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 200000.0001, 500000.0000, 0.7500, 1000.0000, 'ALL_PARTNERS', 'ACTIVE', 3, NOW(), NOW(), 1);

-- Tab 5: Environment Configs — seed master configs first, then link to product

INSERT INTO environment_configs (id, tenant_id, config_code, configuration_name, configuration_name_ar, http_method, api_endpoint, parameters, credentials, headers, test_mode, is_active, sort_order, created_by, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), default_tenant, 'SIMAH_API', 'SIMAH Credit Bureau', 'مكتب سمة للائتمان', 'POST',
     'https://api.simah.com/v2/credit-report',
     '{"timeout_seconds": 30, "retry_count": 3}'::jsonb,
     '{"auth_type": "BEARER", "api_key": "simah-test-key"}'::jsonb,
     '{"Content-Type": "application/json", "Accept": "application/json"}'::jsonb,
     TRUE, TRUE, 1, seed_user_id, NOW(), NOW(), 1),
    (gen_random_uuid(), default_tenant, 'NAFATH_API', 'Nafath Identity', 'نفاذ للهوية', 'POST',
     'https://api.nafath.sa/v1/verify',
     '{"timeout_seconds": 15}'::jsonb,
     '{"auth_type": "API_KEY", "api_key": "nafath-test-key"}'::jsonb,
     '{"Content-Type": "application/json"}'::jsonb,
     TRUE, TRUE, 2, seed_user_id, NOW(), NOW(), 1),
    (gen_random_uuid(), default_tenant, 'YAKEEN_API', 'Yakeen Identity Lookup', 'ياقين للتحقق من الهوية', 'POST',
     'https://api.yakeen.sa/v1/lookup',
     '{"timeout_seconds": 15}'::jsonb,
     '{"auth_type": "API_KEY", "api_key": "yakeen-test-key"}'::jsonb,
     '{"Content-Type": "application/json"}'::jsonb,
     TRUE, TRUE, 3, seed_user_id, NOW(), NOW(), 1);

-- Link all 3 environment configs to the product
INSERT INTO product_environment_configs (tenant_id, product_id, environment_config_id, is_active, sort_order, created_at, updated_at, version)
SELECT default_tenant, v_product_id, ec.id, TRUE, ec.sort_order, NOW(), NOW(), 1
FROM environment_configs ec
WHERE ec.tenant_id = default_tenant AND ec.config_code IN ('SIMAH_API', 'NAFATH_API', 'YAKEEN_API');

-- Tab 6: Duration Settings
INSERT INTO product_duration_settings (tenant_id, product_id, request_duration_days, approval_duration_days,
    disbursement_duration_days, repayment_duration_days, created_at, updated_at, version)
VALUES (default_tenant, v_product_id, 30, 5, 2, 1825, NOW(), NOW(), 1);

-- Tab 7: Approval Workflows (3 workflows with conditions and actions)

-- Workflow 1: Auto-Approval (score > 700, salary > 10k, amount < 100k)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'AUTO_APPROVAL', 'Auto Approval - Low Risk',
    'الموافقة التلقائية - مخاطر منخفضة',
    'Automatically approve applications with high credit score, adequate salary, and moderate financing amount',
    TRUE, 1, NOW(), NOW(), 1);

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
SELECT default_tenant, w.id, cond.field, cond.operator, cond.value::jsonb, cond.sort_order, NOW(), NOW()
FROM product_approval_workflows w,
(VALUES
    ('simah_score', 'GTE', '"700"', 1),
    ('monthly_salary', 'GTE', '"10000"', 2),
    ('financing_amount', 'LTE', '"100000"', 3)
) AS cond(field, operator, value, sort_order)
WHERE w.product_id = v_product_id AND w.workflow_type = 'AUTO_APPROVAL';

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
SELECT default_tenant, w.id, 'APPROVE', '{"notify_customer": true, "notify_email": "approvals@kfs.com"}'::jsonb, 1, NOW(), NOW()
FROM product_approval_workflows w
WHERE w.product_id = v_product_id AND w.workflow_type = 'AUTO_APPROVAL';

-- Workflow 2: Manual Review (score 500-699 or amount > 100k)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'MANUAL_APPROVAL', 'Manual Review - Medium Risk',
    'المراجعة اليدوية - مخاطر متوسطة',
    'Route to credit committee for manual review when score is moderate or amount is high',
    TRUE, 2, NOW(), NOW(), 1);

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
SELECT default_tenant, w.id, cond.field, cond.operator, cond.value::jsonb, cond.sort_order, NOW(), NOW()
FROM product_approval_workflows w,
(VALUES
    ('simah_score', 'BETWEEN', '"500-699"', 1),
    ('financing_amount', 'GT', '"100000"', 2)
) AS cond(field, operator, value, sort_order)
WHERE w.product_id = v_product_id AND w.workflow_type = 'MANUAL_APPROVAL';

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
SELECT default_tenant, w.id, act.action_type, act.configuration::jsonb, act.sort_order, NOW(), NOW()
FROM product_approval_workflows w,
(VALUES
    ('ASSIGN_REVIEWER', '{"reviewer_role": "CREDIT_COMMITTEE"}', 1),
    ('NOTIFY', '{"notify_email": "credit-committee@kfs.com", "escalation_hours": 24}', 2)
) AS act(action_type, configuration, sort_order)
WHERE w.product_id = v_product_id AND w.workflow_type = 'MANUAL_APPROVAL';

-- Workflow 3: Auto-Rejection (score < 500 or DBR > 33%)
INSERT INTO product_approval_workflows (id, tenant_id, product_id, workflow_type, name_en, name_ar, description, is_active, priority, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, 'REJECTION_SCENARIO', 'Auto Rejection - High Risk',
    'الرفض التلقائي - مخاطر عالية',
    'Automatically reject applications with low credit score or excessive DBR',
    TRUE, 3, NOW(), NOW(), 1);

INSERT INTO product_approval_conditions (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
SELECT default_tenant, w.id, cond.field, cond.operator, cond.value::jsonb, cond.sort_order, NOW(), NOW()
FROM product_approval_workflows w,
(VALUES
    ('simah_score', 'LT', '"500"', 1),
    ('dbr_percentage', 'GT', '"33"', 2)
) AS cond(field, operator, value, sort_order)
WHERE w.product_id = v_product_id AND w.workflow_type = 'REJECTION_SCENARIO';

INSERT INTO product_approval_actions (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
SELECT default_tenant, w.id, 'REJECT', '{"reason_code": "HIGH_RISK", "notify_customer": true}'::jsonb, 1, NOW(), NOW()
FROM product_approval_workflows w
WHERE w.product_id = v_product_id AND w.workflow_type = 'REJECTION_SCENARIO';

-- Tab 8: Credit Scoring Criteria (5 criteria with rules)

-- Look up field definition IDs
SELECT id INTO cs_customer_type FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'customer_type';
SELECT id INTO cs_age FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'age';
SELECT id INTO cs_salary FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'salary';
SELECT id INTO cs_simah_score FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'simah_score';
SELECT id INTO cs_employment FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'employment_type';

-- Criteria 1: Customer Type (weight: high)
INSERT INTO product_credit_scoring_criteria (id, tenant_id, product_id, field_definition_id, is_custom, is_enabled, sort_order, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, cs_customer_type, FALSE, TRUE, 1, NOW(), NOW(), 1);

INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage, created_at, updated_at)
SELECT default_tenant, c.id, r.operator, r.value, r.weight, r.percentage, NOW(), NOW()
FROM product_credit_scoring_criteria c,
(VALUES
    ('EQ', 'CITIZEN', 3.00, 20.0000),
    ('EQ', 'RESIDENT', 2.00, 15.0000),
    ('EQ', 'GCC_NATIONAL', 2.50, 17.0000)
) AS r(operator, value, weight, percentage)
WHERE c.product_id = v_product_id AND c.field_definition_id = cs_customer_type;

-- Criteria 2: Age
INSERT INTO product_credit_scoring_criteria (id, tenant_id, product_id, field_definition_id, is_custom, is_enabled, sort_order, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, cs_age, FALSE, TRUE, 2, NOW(), NOW(), 1);

INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage, created_at, updated_at)
SELECT default_tenant, c.id, r.operator, r.value, r.weight, r.percentage, NOW(), NOW()
FROM product_credit_scoring_criteria c,
(VALUES
    ('BETWEEN', '21-30', 2.00, 12.0000),
    ('BETWEEN', '31-45', 3.00, 18.0000),
    ('BETWEEN', '46-60', 2.50, 15.0000),
    ('GT', '60', 1.00, 5.0000)
) AS r(operator, value, weight, percentage)
WHERE c.product_id = v_product_id AND c.field_definition_id = cs_age;

-- Criteria 3: Salary
INSERT INTO product_credit_scoring_criteria (id, tenant_id, product_id, field_definition_id, is_custom, is_enabled, sort_order, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, cs_salary, FALSE, TRUE, 3, NOW(), NOW(), 1);

INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage, created_at, updated_at)
SELECT default_tenant, c.id, r.operator, r.value, r.weight, r.percentage, NOW(), NOW()
FROM product_credit_scoring_criteria c,
(VALUES
    ('BETWEEN', '3000-7999', 1.50, 10.0000),
    ('BETWEEN', '8000-14999', 2.50, 18.0000),
    ('BETWEEN', '15000-29999', 3.00, 22.0000),
    ('GTE', '30000', 3.50, 25.0000)
) AS r(operator, value, weight, percentage)
WHERE c.product_id = v_product_id AND c.field_definition_id = cs_salary;

-- Criteria 4: SIMAH Score
INSERT INTO product_credit_scoring_criteria (id, tenant_id, product_id, field_definition_id, is_custom, is_enabled, sort_order, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, cs_simah_score, FALSE, TRUE, 4, NOW(), NOW(), 1);

INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage, created_at, updated_at)
SELECT default_tenant, c.id, r.operator, r.value, r.weight, r.percentage, NOW(), NOW()
FROM product_credit_scoring_criteria c,
(VALUES
    ('LT', '500', 0.50, 2.0000),
    ('BETWEEN', '500-599', 1.50, 8.0000),
    ('BETWEEN', '600-699', 2.50, 15.0000),
    ('BETWEEN', '700-799', 3.50, 22.0000),
    ('GTE', '800', 4.00, 25.0000)
) AS r(operator, value, weight, percentage)
WHERE c.product_id = v_product_id AND c.field_definition_id = cs_simah_score;

-- Criteria 5: Employment Type
INSERT INTO product_credit_scoring_criteria (id, tenant_id, product_id, field_definition_id, is_custom, is_enabled, sort_order, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, v_product_id, cs_employment, FALSE, TRUE, 5, NOW(), NOW(), 1);

INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage, created_at, updated_at)
SELECT default_tenant, c.id, r.operator, r.value, r.weight, r.percentage, NOW(), NOW()
FROM product_credit_scoring_criteria c,
(VALUES
    ('EQ', 'GOVERNMENT', 3.50, 22.0000),
    ('EQ', 'MILITARY', 3.00, 20.0000),
    ('EQ', 'PRIVATE_SECTOR', 2.50, 15.0000),
    ('EQ', 'SELF_EMPLOYED', 1.50, 8.0000),
    ('EQ', 'RETIRED', 2.00, 12.0000)
) AS r(operator, value, weight, percentage)
WHERE c.product_id = v_product_id AND c.field_definition_id = cs_employment;

-- ============================================================================
-- STEP 4: PARTNERS (seed one demo partner)
-- ============================================================================

INSERT INTO partners (id, tenant_id, partner_code, name_en, name_ar, email, phone, contact_person, status, created_by, created_at, updated_at, version)
VALUES (gen_random_uuid(), default_tenant, 'KFS-MAIN', 'KFS Main Branch', 'فرع KFS الرئيسي',
    'partners@kfs.com', '+966-11-000-0001', 'Partner Admin', 'ACTIVE', seed_user_id, NOW(), NOW(), 1);

INSERT INTO product_partner_affiliations (tenant_id, product_id, partner_id, affiliation_type, commission_percentage, status, created_at, updated_at, version)
SELECT default_tenant, v_product_id, p.id, 'PRIMARY', 2.50, 'ACTIVE', NOW(), NOW(), 1
FROM partners p
WHERE p.tenant_id = default_tenant AND p.partner_code = 'KFS-MAIN';

-- ============================================================================
-- STEP 5: REQUIRED DOCUMENTS
-- ============================================================================

INSERT INTO product_documents (tenant_id, product_id, name_en, name_ar, document_type, created_by_name, status, is_required, sort_order, created_at, updated_at, version)
VALUES
    (default_tenant, v_product_id, 'National ID (front & back)', 'الهوية الوطنية (الوجه والخلف)', 'TEMPLATE', 'System', 'ACTIVE', TRUE, 1, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 'Salary Certificate', 'شهادة الراتب', 'TEMPLATE', 'System', 'ACTIVE', TRUE, 2, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 'Bank Statement (3 months)', 'كشف حساب بنكي (3 أشهر)', 'TEMPLATE', 'System', 'ACTIVE', TRUE, 3, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 'GOSI Certificate', 'شهادة التأمينات الاجتماعية', 'TEMPLATE', 'System', 'ACTIVE', TRUE, 4, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 'Financing Agreement', 'اتفاقية التمويل', 'AGREEMENT', 'System', 'ACTIVE', TRUE, 5, NOW(), NOW(), 1),
    (default_tenant, v_product_id, 'Product Guide', 'دليل المنتج', 'GUIDE', 'System', 'ACTIVE', FALSE, 6, NOW(), NOW(), 1);

RAISE NOTICE 'Personal Financing product created with id: %', v_product_id;

END $$;
