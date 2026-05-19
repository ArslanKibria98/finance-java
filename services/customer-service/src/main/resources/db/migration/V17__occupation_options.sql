-- ============================================================================
-- V17: Occupation options (LOV)
-- Admin-managed lookup; same shape as source_of_income_options
-- ============================================================================

CREATE TABLE occupation_options (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(50) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    name_ar         VARCHAR(255) NOT NULL,
    description_en  TEXT,
    description_ar  TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_occupation_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_occupation_tenant ON occupation_options(tenant_id);
CREATE INDEX idx_occupation_active ON occupation_options(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_occupation_not_deleted ON occupation_options(tenant_id) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_occupation_updated
    BEFORE UPDATE ON occupation_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE occupation_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY occupation_tenant_isolation ON occupation_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

INSERT INTO occupation_options (tenant_id, code, name_en, name_ar, description_en, description_ar, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'EMPLOYED_PRIVATE',   'Employed (Private Sector)',    'موظف (قطاع خاص)',     'Salaried employment in private sector',        'وظيفة براتب في القطاع الخاص',     1),
    ('00000000-0000-0000-0000-000000000001', 'EMPLOYED_PUBLIC',    'Employed (Public Sector)',     'موظف (قطاع حكومي)',   'Salaried employment in government or public', 'وظيفة براتب في القطاع الحكومي',   2),
    ('00000000-0000-0000-0000-000000000001', 'SELF_EMPLOYED',      'Self-Employed / Business Owner','صاحب عمل / تاجر',    'Business owner or self-employed',              'صاحب عمل أو عمل حر',              3),
    ('00000000-0000-0000-0000-000000000001', 'RETIRED',            'Retired',                        'متقاعد',              'Retired from workforce',                       'متقاعد عن العمل',                 4),
    ('00000000-0000-0000-0000-000000000001', 'STUDENT',            'Student',                        'طالب',                'Full-time student',                            'طالب بدوام كامل',                 5),
    ('00000000-0000-0000-0000-000000000001', 'UNEMPLOYED',         'Unemployed / Homemaker',         'عاطل عن العمل / رب منزل', 'Not currently in paid employment',        'غير عامل براتب حالياً',           6),
    ('00000000-0000-0000-0000-000000000001', 'OTHER',              'Other',                          'أخرى',                'Occupation not listed above',                  'مهنة غير مدرجة أعلاه',            7);

COMMENT ON TABLE occupation_options IS 'Configurable occupation reference data (per tenant) for KYC / profile';
