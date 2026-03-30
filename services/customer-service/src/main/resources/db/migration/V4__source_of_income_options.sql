-- ============================================================================
-- V4: Source of Income Options
-- Admin-managed dynamic lookup for Source of Income (LOV)
-- ============================================================================

CREATE TABLE source_of_income_options (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(50) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    name_ar         VARCHAR(255) NOT NULL,
    description_en  TEXT,
    description_ar  TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_soi_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_soi_tenant ON source_of_income_options(tenant_id);
CREATE INDEX idx_soi_active ON source_of_income_options(tenant_id) WHERE is_active = TRUE;

CREATE TRIGGER trg_soi_updated
    BEFORE UPDATE ON source_of_income_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE source_of_income_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY soi_tenant_isolation ON source_of_income_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- ============================================================================
-- SEED DATA: Default tenant
-- ============================================================================

INSERT INTO source_of_income_options (tenant_id, code, name_en, name_ar, description_en, description_ar, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'EMPLOYMENT_SALARY',    'Employment Salary',              'الراتب الوظيفي',               'Regular monthly salary from employer',              'الراتب الشهري المنتظم من صاحب العمل',     1),
    ('00000000-0000-0000-0000-000000000001', 'BUSINESS_INCOME',      'Business Income',                'دخل الأعمال التجارية',          'Income from owning or operating a business',        'الدخل من امتلاك أو تشغيل عمل تجاري',      2),
    ('00000000-0000-0000-0000-000000000001', 'FREELANCE',            'Freelance / Self-Employed',      'العمل الحر',                    'Income from freelance or self-employment',          'الدخل من العمل الحر',                      3),
    ('00000000-0000-0000-0000-000000000001', 'RENTAL_INCOME',        'Rental Income',                  'دخل الإيجار',                   'Income from property rentals',                      'الدخل من تأجير العقارات',                  4),
    ('00000000-0000-0000-0000-000000000001', 'INVESTMENT_RETURNS',   'Investment Returns',             'عوائد الاستثمار',               'Returns from stocks, bonds, or other investments',  'عوائد الأسهم أو السندات أو استثمارات أخرى', 5),
    ('00000000-0000-0000-0000-000000000001', 'RETIREMENT_PENSION',   'Retirement / Pension',           'التقاعد / المعاش',              'Pension or retirement fund payments',               'مدفوعات التقاعد أو صندوق المعاشات',       6),
    ('00000000-0000-0000-0000-000000000001', 'GOVERNMENT_ALLOWANCE', 'Government Allowance',           'بدل حكومي',                     'Government assistance or social security',          'مساعدات حكومية أو ضمان اجتماعي',          7),
    ('00000000-0000-0000-0000-000000000001', 'OTHER',                'Other',                          'أخرى',                          'Other source of income not listed above',           'مصدر دخل آخر غير مدرج أعلاه',             8);

COMMENT ON TABLE source_of_income_options IS 'Configurable source of income reference data (per tenant) for EDD';
