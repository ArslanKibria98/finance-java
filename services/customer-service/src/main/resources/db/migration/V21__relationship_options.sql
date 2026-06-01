-- ============================================================================
-- V21: Relationship options (LOV)
-- Admin-managed lookup; same shape as occupation_options
-- Used for next-of-kin / family member relationship dropdowns
-- ============================================================================

CREATE TABLE relationship_options (
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

    CONSTRAINT uq_relationship_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_relationship_tenant ON relationship_options(tenant_id);
CREATE INDEX idx_relationship_active ON relationship_options(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_relationship_not_deleted ON relationship_options(tenant_id) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_relationship_updated
    BEFORE UPDATE ON relationship_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE relationship_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY relationship_tenant_isolation ON relationship_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

INSERT INTO relationship_options (tenant_id, code, name_en, name_ar, description_en, description_ar, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'FATHER',       'Father',          'الأب',             'Biological or adoptive father',         'الأب البيولوجي أو بالتبني',         1),
    ('00000000-0000-0000-0000-000000000001', 'MOTHER',       'Mother',          'الأم',             'Biological or adoptive mother',         'الأم البيولوجية أو بالتبني',        2),
    ('00000000-0000-0000-0000-000000000001', 'SPOUSE',       'Spouse',          'الزوج / الزوجة',   'Husband or wife',                        'الزوج أو الزوجة',                    3),
    ('00000000-0000-0000-0000-000000000001', 'SON',          'Son',             'الابن',            'Biological or adoptive son',            'الابن البيولوجي أو بالتبني',         4),
    ('00000000-0000-0000-0000-000000000001', 'DAUGHTER',     'Daughter',        'الابنة',           'Biological or adoptive daughter',       'الابنة البيولوجية أو بالتبني',      5),
    ('00000000-0000-0000-0000-000000000001', 'BROTHER',      'Brother',         'الأخ',             'Full or half brother',                  'الأخ الشقيق أو غير الشقيق',         6),
    ('00000000-0000-0000-0000-000000000001', 'SISTER',       'Sister',          'الأخت',            'Full or half sister',                   'الأخت الشقيقة أو غير الشقيقة',       7),
    ('00000000-0000-0000-0000-000000000001', 'GRANDFATHER',  'Grandfather',     'الجد',             'Paternal or maternal grandfather',      'الجد لأب أو لأم',                    8),
    ('00000000-0000-0000-0000-000000000001', 'GRANDMOTHER',  'Grandmother',     'الجدة',            'Paternal or maternal grandmother',      'الجدة لأب أو لأم',                   9),
    ('00000000-0000-0000-0000-000000000001', 'UNCLE',        'Uncle',           'العم / الخال',     'Paternal or maternal uncle',            'العم أو الخال',                       10),
    ('00000000-0000-0000-0000-000000000001', 'AUNT',         'Aunt',            'العمة / الخالة',   'Paternal or maternal aunt',             'العمة أو الخالة',                     11),
    ('00000000-0000-0000-0000-000000000001', 'COUSIN',       'Cousin',          'ابن / ابنة العم',  'Cousin (any side)',                     'ابن أو ابنة العم أو الخال',          12),
    ('00000000-0000-0000-0000-000000000001', 'FRIEND',       'Friend',          'صديق',             'Close friend',                          'صديق مقرب',                          13),
    ('00000000-0000-0000-0000-000000000001', 'OTHER',        'Other',           'أخرى',             'Relationship not listed above',         'علاقة غير مدرجة أعلاه',              14);

COMMENT ON TABLE relationship_options IS 'Configurable relationship reference data (per tenant) for next-of-kin / family contacts';
