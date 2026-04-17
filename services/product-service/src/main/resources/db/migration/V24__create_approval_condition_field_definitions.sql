-- ============================================================================
-- V24: Create approval_condition_field_definitions + options tables
-- ============================================================================
-- Same pattern as credit_scoring_field_definitions but for approval workflow
-- condition dropdowns. STRING/BOOLEAN fields get predefined options;
-- NUMERIC fields use freetext input.
-- ============================================================================

CREATE TABLE approval_condition_field_definitions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    field_key   VARCHAR(100) NOT NULL,
    name_en     VARCHAR(255) NOT NULL,
    name_ar     VARCHAR(255) NOT NULL,
    data_type   VARCHAR(50)  NOT NULL,   -- NUMERIC, STRING, BOOLEAN
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version     INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_approval_field_key UNIQUE (tenant_id, field_key)
);

CREATE TABLE approval_condition_field_options (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    field_definition_id UUID NOT NULL REFERENCES approval_condition_field_definitions(id) ON DELETE CASCADE,
    option_key          VARCHAR(100) NOT NULL,
    label_en            VARCHAR(255) NOT NULL,
    label_ar            VARCHAR(255) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_approval_field_defs_tenant ON approval_condition_field_definitions(tenant_id);
CREATE INDEX idx_approval_field_opts_field ON approval_condition_field_options(field_definition_id);
CREATE INDEX idx_approval_field_opts_tenant ON approval_condition_field_options(tenant_id);
CREATE UNIQUE INDEX idx_approval_field_opts_unique ON approval_condition_field_options(tenant_id, field_definition_id, option_key);

-- Triggers
CREATE TRIGGER trg_approval_field_defs_updated
    BEFORE UPDATE ON approval_condition_field_definitions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_approval_field_opts_updated
    BEFORE UPDATE ON approval_condition_field_options FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- RLS
ALTER TABLE approval_condition_field_definitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_condition_field_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON approval_condition_field_definitions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON approval_condition_field_options
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- Comments
COMMENT ON TABLE approval_condition_field_definitions IS 'Master lookup for approval workflow condition fields (dropdown in UI)';
COMMENT ON TABLE approval_condition_field_options IS 'Predefined options for STRING/BOOLEAN approval condition fields';

-- ============================================================================
-- SEED FIELD DEFINITIONS (20 fields)
-- ============================================================================

INSERT INTO approval_condition_field_definitions (tenant_id, field_key, name_en, name_ar, data_type, sort_order)
VALUES
    -- ── Financial Metrics ──
    ('00000000-0000-0000-0000-000000000001', 'dbr_percentage',       'DBR Percentage',          'نسبة عبء الدين',              'NUMERIC',  1),
    ('00000000-0000-0000-0000-000000000001', 'financing_amount',     'Loan Amount',             'مبلغ التمويل',                'NUMERIC',  2),
    ('00000000-0000-0000-0000-000000000001', 'monthly_salary',       'Monthly Salary',          'الراتب الشهري',               'NUMERIC',  3),
    ('00000000-0000-0000-0000-000000000001', 'total_income',         'Total Income',            'إجمالي الدخل',               'NUMERIC',  4),
    ('00000000-0000-0000-0000-000000000001', 'disposable_income',    'Disposable Income',       'الدخل المتاح',               'NUMERIC',  5),

    -- ── Credit & Risk Scores ──
    ('00000000-0000-0000-0000-000000000001', 'simah_score',          'SIMAH Score',             'درجة سمة',                   'NUMERIC',  6),
    ('00000000-0000-0000-0000-000000000001', 'credit_score',         'Credit Score',            'درجة الائتمان',               'NUMERIC',  7),
    ('00000000-0000-0000-0000-000000000001', 'risk_score',           'Risk Score',              'درجة المخاطر',               'NUMERIC',  8),
    ('00000000-0000-0000-0000-000000000001', 'risk_type',            'Risk Type',               'نوع المخاطر',                'STRING',   9),

    -- ── SIMAH Details ──
    ('00000000-0000-0000-0000-000000000001', 'simah_max_dpd',        'SIMAH Max DPD',           'أقصى أيام تأخر سمة',         'NUMERIC', 10),
    ('00000000-0000-0000-0000-000000000001', 'simah_writeoff_count', 'SIMAH Write-Off Count',   'عدد الشطب في سمة',           'NUMERIC', 11),
    ('00000000-0000-0000-0000-000000000001', 'simah_arrears_count',  'SIMAH Accounts in Arrears','عدد الحسابات المتأخرة',      'NUMERIC', 12),
    ('00000000-0000-0000-0000-000000000001', 'simah_enquiry_count',  'SIMAH Enquiries (90 days)','استعلامات سمة (90 يوم)',      'NUMERIC', 13),

    -- ── Customer Profile ──
    ('00000000-0000-0000-0000-000000000001', 'customer_age',         'Customer Age',            'عمر العميل',                 'NUMERIC', 14),
    ('00000000-0000-0000-0000-000000000001', 'age_at_maturity',      'Age at Maturity',         'العمر عند الاستحقاق',         'NUMERIC', 15),
    ('00000000-0000-0000-0000-000000000001', 'employment_type',      'Employment Type',         'نوع التوظيف',                'STRING',  16),
    ('00000000-0000-0000-0000-000000000001', 'employment_months',    'Employment Duration (months)','مدة التوظيف (أشهر)',       'NUMERIC', 17),
    ('00000000-0000-0000-0000-000000000001', 'employer_category',    'Employer Category',       'فئة جهة العمل',              'STRING',  18),

    -- ── Compliance Flags ──
    ('00000000-0000-0000-0000-000000000001', 'pep',                  'PEP Status',              'حالة الشخص السياسي',          'BOOLEAN', 19),
    ('00000000-0000-0000-0000-000000000001', 'sanction_match',       'Sanctions Match',         'مطابقة العقوبات',            'BOOLEAN', 20);


-- ============================================================================
-- SEED OPTIONS FOR STRING/BOOLEAN FIELDS
-- ============================================================================

-- Risk Type options
INSERT INTO approval_condition_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM approval_condition_field_definitions d,
(VALUES
    ('LOW',      'Low',      'منخفض',  1),
    ('MEDIUM',   'Medium',   'متوسط',  2),
    ('HIGH',     'High',     'مرتفع',  3),
    ('CRITICAL', 'Critical', 'حرج',    4)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'risk_type' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- Employment Type options
INSERT INTO approval_condition_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM approval_condition_field_definitions d,
(VALUES
    ('GOVERNMENT',      'Government',      'حكومي',      1),
    ('MILITARY',        'Military',        'عسكري',      2),
    ('SEMI_GOVERNMENT', 'Semi-Government', 'شبه حكومي',  3),
    ('PRIVATE_SECTOR',  'Private Sector',  'قطاع خاص',   4),
    ('SELF_EMPLOYED',   'Self Employed',   'عمل حر',     5),
    ('RETIRED',         'Retired',         'متقاعد',     6)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'employment_type' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- Employer Category options
INSERT INTO approval_condition_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM approval_condition_field_definitions d,
(VALUES
    ('LISTED',   'Listed Company',   'شركة مدرجة',       1),
    ('APPROVED', 'Approved Company', 'شركة معتمدة',      2),
    ('UNKNOWN',  'Unknown',          'غير معروف',        3)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'employer_category' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- PEP Status options (BOOLEAN)
INSERT INTO approval_condition_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM approval_condition_field_definitions d,
(VALUES
    ('true',  'Yes - PEP Detected',  'نعم - شخص سياسي', 1),
    ('false', 'No - Not PEP',        'لا - ليس سياسي',  2)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'pep' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- Sanctions Match options (BOOLEAN)
INSERT INTO approval_condition_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM approval_condition_field_definitions d,
(VALUES
    ('true',  'Yes - Match Found',  'نعم - مطابقة موجودة',   1),
    ('false', 'No - Clear',         'لا - نظيف',             2)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'sanction_match' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';
