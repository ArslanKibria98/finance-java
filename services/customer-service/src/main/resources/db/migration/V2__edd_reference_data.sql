-- ============================================================================
-- EDD REFERENCE DATA TABLES
-- Source of Wealth, Source of Funds, Net Worth Ranges
-- Admin-managed dynamic lookup options for Enhanced Due Diligence
-- ============================================================================

-- ============================================================================
-- TABLE: source_of_wealth_options
-- ============================================================================

CREATE TABLE source_of_wealth_options (
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

    CONSTRAINT uq_sow_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_sow_tenant ON source_of_wealth_options(tenant_id);
CREATE INDEX idx_sow_active ON source_of_wealth_options(tenant_id) WHERE is_active = TRUE;

-- ============================================================================
-- TABLE: source_of_funds_options
-- ============================================================================

CREATE TABLE source_of_funds_options (
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

    CONSTRAINT uq_sof_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_sof_tenant ON source_of_funds_options(tenant_id);
CREATE INDEX idx_sof_active ON source_of_funds_options(tenant_id) WHERE is_active = TRUE;

-- ============================================================================
-- TABLE: net_worth_range_options
-- ============================================================================

CREATE TABLE net_worth_range_options (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(50) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    name_ar         VARCHAR(255) NOT NULL,
    description_en  TEXT,
    description_ar  TEXT,
    min_value       NUMERIC(19, 4),
    max_value       NUMERIC(19, 4),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_nwr_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_nwr_tenant ON net_worth_range_options(tenant_id);
CREATE INDEX idx_nwr_active ON net_worth_range_options(tenant_id) WHERE is_active = TRUE;

-- ============================================================================
-- TRIGGERS: Reuse existing update_updated_at_column() for version/timestamp
-- ============================================================================

CREATE TRIGGER trg_sow_updated
    BEFORE UPDATE ON source_of_wealth_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_sof_updated
    BEFORE UPDATE ON source_of_funds_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_nwr_updated
    BEFORE UPDATE ON net_worth_range_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE source_of_wealth_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE source_of_funds_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE net_worth_range_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY sow_tenant_isolation ON source_of_wealth_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY sof_tenant_isolation ON source_of_funds_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY nwr_tenant_isolation ON net_worth_range_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- ============================================================================
-- SEED DATA: Default tenant
-- ============================================================================

-- Source of Wealth options
INSERT INTO source_of_wealth_options (tenant_id, code, name_en, name_ar, description_en, description_ar, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SALARY',              'Salary / Employment Income',     'الراتب / دخل العمل',           'Regular income from employment',                   'الدخل المنتظم من العمل',                1),
    ('00000000-0000-0000-0000-000000000001', 'BUSINESS_INCOME',     'Business Income / Ownership',    'دخل الأعمال / الملكية',        'Income from owning or operating a business',       'الدخل من امتلاك أو تشغيل عمل تجاري',    2),
    ('00000000-0000-0000-0000-000000000001', 'INHERITANCE',         'Inheritance',                    'الميراث',                       'Wealth inherited from family',                     'الثروة الموروثة من العائلة',             3),
    ('00000000-0000-0000-0000-000000000001', 'INVESTMENTS',         'Investments',                    'الاستثمارات',                   'Returns from stocks, bonds, or other investments', 'عوائد الأسهم أو السندات أو استثمارات أخرى', 4),
    ('00000000-0000-0000-0000-000000000001', 'REAL_ESTATE',         'Real Estate',                    'العقارات',                      'Income from property ownership or sales',          'الدخل من ملكية العقارات أو بيعها',       5),
    ('00000000-0000-0000-0000-000000000001', 'RETIREMENT_PENSION',  'Retirement / Pension',           'التقاعد / المعاش',              'Pension or retirement fund payments',              'مدفوعات التقاعد أو صندوق المعاشات',     6),
    ('00000000-0000-0000-0000-000000000001', 'GOVERNMENT_GRANTS',   'Government Grants / Benefits',   'المنح الحكومية / الإعانات',      'Government assistance or grant programs',           'برامج المساعدة أو المنح الحكومية',       7),
    ('00000000-0000-0000-0000-000000000001', 'OTHER',               'Other',                          'أخرى',                          'Other source of wealth not listed above',          'مصدر ثروة آخر غير مدرج أعلاه',           8);

-- Source of Funds options
INSERT INTO source_of_funds_options (tenant_id, code, name_en, name_ar, description_en, description_ar, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'EMPLOYMENT_SALARY',   'Employment Salary',              'راتب العمل',                   'Monthly salary from employer',                     'الراتب الشهري من صاحب العمل',            1),
    ('00000000-0000-0000-0000-000000000001', 'BUSINESS_REVENUE',    'Business Revenue',               'إيرادات الأعمال',               'Revenue from business operations',                 'إيرادات من العمليات التجارية',            2),
    ('00000000-0000-0000-0000-000000000001', 'SAVINGS',             'Savings',                        'المدخرات',                      'Personal savings accumulated over time',            'المدخرات الشخصية المتراكمة',             3),
    ('00000000-0000-0000-0000-000000000001', 'LOAN_PROCEEDS',       'Loan Proceeds',                  'عائدات القرض',                  'Funds received from an approved loan',             'الأموال المستلمة من قرض معتمد',          4),
    ('00000000-0000-0000-0000-000000000001', 'INVESTMENT_RETURNS',  'Investment Returns',             'عوائد الاستثمار',               'Returns from investment portfolio',                'عوائد من محفظة استثمارية',               5),
    ('00000000-0000-0000-0000-000000000001', 'PROPERTY_SALE',       'Property Sale',                  'بيع العقارات',                  'Proceeds from sale of real estate',                'عائدات بيع العقارات',                    6),
    ('00000000-0000-0000-0000-000000000001', 'INHERITANCE_FUNDS',   'Inheritance',                    'أموال الميراث',                 'Funds received through inheritance',               'أموال مستلمة من خلال الميراث',            7),
    ('00000000-0000-0000-0000-000000000001', 'GIFT',                'Gift',                           'هدية',                          'Funds received as a gift',                         'أموال مستلمة كهدية',                     8);

-- Net Worth Range options
INSERT INTO net_worth_range_options (tenant_id, code, name_en, name_ar, description_en, description_ar, min_value, max_value, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'UNDER_50K',    'Under 50,000 SAR',          'أقل من 50,000 ريال',          NULL, NULL, 0,          50000,      1),
    ('00000000-0000-0000-0000-000000000001', 'RANGE_50K_100K', '50,000 - 100,000 SAR',    '50,000 - 100,000 ريال',       NULL, NULL, 50000,      100000,     2),
    ('00000000-0000-0000-0000-000000000001', 'RANGE_100K_500K', '100,000 - 500,000 SAR',  '100,000 - 500,000 ريال',      NULL, NULL, 100000,     500000,     3),
    ('00000000-0000-0000-0000-000000000001', 'RANGE_500K_1M', '500,000 - 1,000,000 SAR',  '500,000 - 1,000,000 ريال',    NULL, NULL, 500000,     1000000,    4),
    ('00000000-0000-0000-0000-000000000001', 'RANGE_1M_5M',  '1,000,000 - 5,000,000 SAR', '1,000,000 - 5,000,000 ريال', NULL, NULL, 1000000,    5000000,    5),
    ('00000000-0000-0000-0000-000000000001', 'RANGE_5M_10M', '5,000,000 - 10,000,000 SAR','5,000,000 - 10,000,000 ريال', NULL, NULL, 5000000,    10000000,   6),
    ('00000000-0000-0000-0000-000000000001', 'OVER_10M',     'Over 10,000,000 SAR',       'أكثر من 10,000,000 ريال',     NULL, NULL, 10000000,   NULL,       7);
