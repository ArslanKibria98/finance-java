-- ============================================================================
-- V7: Credit Scoring Engine Schema + Field Definitions Seed Data
-- Migrated from product-service to risk-service
-- ============================================================================

-- Table 1: credit_scoring_field_definitions (global reference data)
CREATE TABLE credit_scoring_field_definitions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    field_key       VARCHAR(100) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    name_ar         VARCHAR(255) NOT NULL,
    data_type       VARCHAR(20) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    UNIQUE(tenant_id, field_key)
);

CREATE INDEX idx_csfd_tenant ON credit_scoring_field_definitions(tenant_id);
CREATE INDEX idx_csfd_tenant_active ON credit_scoring_field_definitions(tenant_id, is_active);

-- Table 2: product_credit_scoring_criteria (per-product scoring criteria)
CREATE TABLE product_credit_scoring_criteria (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    product_id          UUID NOT NULL,
    field_definition_id UUID REFERENCES credit_scoring_field_definitions(id),
    custom_name         VARCHAR(255),
    is_custom           BOOLEAN NOT NULL DEFAULT false,
    is_enabled          BOOLEAN NOT NULL DEFAULT true,
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_pcsc_tenant_product ON product_credit_scoring_criteria(tenant_id, product_id);

-- Table 3: product_credit_scoring_rules (rules per criteria)
CREATE TABLE product_credit_scoring_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    criteria_id     UUID NOT NULL REFERENCES product_credit_scoring_criteria(id) ON DELETE CASCADE,
    operator        VARCHAR(20) NOT NULL,
    value           VARCHAR(500) NOT NULL,
    weight          NUMERIC(10,4) NOT NULL DEFAULT 0,
    percentage      NUMERIC(10,4) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pcsr_criteria ON product_credit_scoring_rules(criteria_id);

-- ============================================================================
-- Seed 40 predefined credit scoring field definitions
-- ============================================================================

DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
BEGIN

INSERT INTO credit_scoring_field_definitions (tenant_id, field_key, name_en, name_ar, data_type, sort_order)
VALUES
    (default_tenant, 'customer_type', 'Customer Type', 'نوع العميل', 'STRING', 1),
    (default_tenant, 'age', 'Age Of Customer', 'عمر العميل', 'NUMERIC', 2),
    (default_tenant, 'gender', 'Gender', 'الجنس', 'STRING', 3),
    (default_tenant, 'nationality', 'Nationality', 'الجنسية', 'STRING', 4),
    (default_tenant, 'residency_type', 'Residency Type', 'نوع الإقامة', 'STRING', 5),
    (default_tenant, 'marital_status', 'Marital Status', 'الحالة الاجتماعية', 'STRING', 6),
    (default_tenant, 'number_of_dependents', 'Number Of Dependents', 'عدد المعالين', 'NUMERIC', 7),
    (default_tenant, 'education_level', 'Education Level', 'المستوى التعليمي', 'STRING', 8),
    (default_tenant, 'salary', 'Salary', 'الراتب', 'NUMERIC', 9),
    (default_tenant, 'basic_salary', 'Basic Salary', 'الراتب الأساسي', 'NUMERIC', 10),
    (default_tenant, 'housing_allowance', 'Housing Allowance', 'بدل السكن', 'NUMERIC', 11),
    (default_tenant, 'transportation_allowance', 'Transportation Allowance', 'بدل النقل', 'NUMERIC', 12),
    (default_tenant, 'other_allowances', 'Other Allowances', 'بدلات أخرى', 'NUMERIC', 13),
    (default_tenant, 'total_income', 'Total Income', 'إجمالي الدخل', 'NUMERIC', 14),
    (default_tenant, 'employment_type', 'Employment Type', 'نوع التوظيف', 'STRING', 15),
    (default_tenant, 'employment_sector', 'Employment Sector', 'قطاع التوظيف', 'STRING', 16),
    (default_tenant, 'employer_name', 'Employer Name', 'اسم جهة العمل', 'STRING', 17),
    (default_tenant, 'years_of_employment', 'Years Of Employment', 'سنوات العمل', 'NUMERIC', 18),
    (default_tenant, 'months_in_current_job', 'Months In Current Job', 'أشهر في الوظيفة الحالية', 'NUMERIC', 19),
    (default_tenant, 'simah_score', 'SIMAH Score', 'درجة سمة', 'NUMERIC', 20),
    (default_tenant, 'simah_defaults', 'SIMAH Defaults Count', 'عدد التعثرات في سمة', 'NUMERIC', 21),
    (default_tenant, 'simah_enquiries', 'SIMAH Enquiries Count', 'عدد الاستعلامات في سمة', 'NUMERIC', 22),
    (default_tenant, 'existing_obligations', 'Existing Obligations', 'الالتزامات الحالية', 'NUMERIC', 23),
    (default_tenant, 'dbr_percentage', 'DBR Percentage', 'نسبة عبء الدين', 'NUMERIC', 24),
    (default_tenant, 'number_of_active_loans', 'Number Of Active Loans', 'عدد القروض النشطة', 'NUMERIC', 25),
    (default_tenant, 'number_of_credit_cards', 'Number Of Credit Cards', 'عدد بطاقات الائتمان', 'NUMERIC', 26),
    (default_tenant, 'loan_amount_requested', 'Loan Amount Requested', 'مبلغ القرض المطلوب', 'NUMERIC', 27),
    (default_tenant, 'loan_tenure_requested', 'Loan Tenure Requested', 'مدة القرض المطلوبة', 'NUMERIC', 28),
    (default_tenant, 'loan_to_value_ratio', 'Loan To Value Ratio', 'نسبة القرض إلى القيمة', 'NUMERIC', 29),
    (default_tenant, 'collateral_type', 'Collateral Type', 'نوع الضمان', 'STRING', 30),
    (default_tenant, 'collateral_value', 'Collateral Value', 'قيمة الضمان', 'NUMERIC', 31),
    (default_tenant, 'property_type', 'Property Type', 'نوع العقار', 'STRING', 32),
    (default_tenant, 'property_value', 'Property Value', 'قيمة العقار', 'NUMERIC', 33),
    (default_tenant, 'down_payment_percentage', 'Down Payment Percentage', 'نسبة الدفعة المقدمة', 'NUMERIC', 34),
    (default_tenant, 'bank_relationship_years', 'Bank Relationship Years', 'سنوات العلاقة مع البنك', 'NUMERIC', 35),
    (default_tenant, 'previous_defaults', 'Previous Defaults', 'التعثرات السابقة', 'BOOLEAN', 36),
    (default_tenant, 'guarantor_available', 'Guarantor Available', 'توفر كفيل', 'BOOLEAN', 37),
    (default_tenant, 'city', 'City / Region', 'المدينة / المنطقة', 'STRING', 38),
    (default_tenant, 'account_balance_avg', 'Average Account Balance', 'متوسط رصيد الحساب', 'NUMERIC', 39),
    (default_tenant, 'salary_transfer_bank', 'Salary Transfer To Bank', 'تحويل الراتب للبنك', 'BOOLEAN', 40);

END $$;
