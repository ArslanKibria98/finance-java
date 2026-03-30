-- ============================================================================
-- V5: Purpose of Finance Options
-- Admin-managed dynamic lookup for Purpose of Finance (LOV)
-- ============================================================================

CREATE TABLE purpose_of_finance_options (
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

    CONSTRAINT uq_pof_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_pof_tenant ON purpose_of_finance_options(tenant_id);
CREATE INDEX idx_pof_active ON purpose_of_finance_options(tenant_id) WHERE is_active = TRUE;

CREATE TRIGGER trg_pof_updated
    BEFORE UPDATE ON purpose_of_finance_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE purpose_of_finance_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY pof_tenant_isolation ON purpose_of_finance_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- ============================================================================
-- SEED DATA: Default tenant
-- ============================================================================

INSERT INTO purpose_of_finance_options (tenant_id, code, name_en, name_ar, description_en, description_ar, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'HOUSEHOLD',          'Household Expenses',   'مصاريف منزلية',      'Expenses related to household needs',               'مصاريف متعلقة بالاحتياجات المنزلية',        1),
    ('00000000-0000-0000-0000-000000000001', 'FAMILY_SUPPORT',     'Family Support',       'دعم الأسرة',         'Financial support for family members',               'دعم مالي لأفراد الأسرة',                    2),
    ('00000000-0000-0000-0000-000000000001', 'EDUCATION',          'Education',            'التعليم',             'Education and training expenses',                    'مصاريف التعليم والتدريب',                    3),
    ('00000000-0000-0000-0000-000000000001', 'PERSONAL',           'Personal',             'شخصي',               'Personal financing needs',                          'احتياجات تمويل شخصية',                       4),
    ('00000000-0000-0000-0000-000000000001', 'MEDICAL_TREATMENT',  'Medical Treatment',    'العلاج الطبي',        'Medical and healthcare expenses',                   'مصاريف طبية ورعاية صحية',                   5),
    ('00000000-0000-0000-0000-000000000001', 'VEHICLE_PURCHASE',   'Vehicle Purchase',     'شراء مركبة',         'Purchase of a vehicle',                             'شراء مركبة',                                 6),
    ('00000000-0000-0000-0000-000000000001', 'HOME_RENOVATION',    'Home Renovation',      'ترميم المنزل',        'Home improvement and renovation',                   'تحسين وترميم المنزل',                        7),
    ('00000000-0000-0000-0000-000000000001', 'DEBT_CONSOLIDATION', 'Debt Consolidation',   'توحيد الديون',        'Consolidation of existing debts',                   'توحيد الديون القائمة',                       8),
    ('00000000-0000-0000-0000-000000000001', 'BUSINESS',           'Business',             'أعمال تجارية',        'Business-related financing',                        'تمويل متعلق بالأعمال التجارية',              9),
    ('00000000-0000-0000-0000-000000000001', 'OTHER',              'Other',                'أخرى',               'Other purpose not listed above',                    'غرض آخر غير مدرج أعلاه',                    10);

COMMENT ON TABLE purpose_of_finance_options IS 'Configurable purpose of finance reference data (per tenant) for loan applications';
