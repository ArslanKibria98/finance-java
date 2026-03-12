-- ============================================================================
-- V8: Purpose of Finance reference data table
-- Replaces hardcoded enum with configurable reference data
-- ============================================================================

CREATE TABLE purpose_of_finance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(50) NOT NULL,
    name_en         VARCHAR(100) NOT NULL,
    name_ar         VARCHAR(100),
    description_en  VARCHAR(500),
    description_ar  VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    version         INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_purpose_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_purpose_tenant ON purpose_of_finance(tenant_id);
CREATE INDEX idx_purpose_active ON purpose_of_finance(tenant_id, is_active) WHERE is_active = TRUE;

CREATE TRIGGER trigger_purpose_updated
    BEFORE UPDATE ON purpose_of_finance FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed default purposes
INSERT INTO purpose_of_finance (tenant_id, code, name_en, name_ar, sort_order)
VALUES
    ('00000000-0000-0000-0000-000000000000', 'HOUSEHOLD', 'Household Expenses', 'مصاريف منزلية', 1),
    ('00000000-0000-0000-0000-000000000000', 'FAMILY_SUPPORT', 'Family Support', 'دعم الأسرة', 2),
    ('00000000-0000-0000-0000-000000000000', 'EDUCATION', 'Education', 'التعليم', 3),
    ('00000000-0000-0000-0000-000000000000', 'PERSONAL', 'Personal', 'شخصي', 4),
    ('00000000-0000-0000-0000-000000000000', 'MEDICAL_TREATMENT', 'Medical Treatment', 'العلاج الطبي', 5),
    ('00000000-0000-0000-0000-000000000000', 'VEHICLE_PURCHASE', 'Vehicle Purchase', 'شراء مركبة', 6),
    ('00000000-0000-0000-0000-000000000000', 'HOME_RENOVATION', 'Home Renovation', 'ترميم المنزل', 7),
    ('00000000-0000-0000-0000-000000000000', 'DEBT_CONSOLIDATION', 'Debt Consolidation', 'توحيد الديون', 8),
    ('00000000-0000-0000-0000-000000000000', 'BUSINESS', 'Business', 'أعمال تجارية', 9),
    ('00000000-0000-0000-0000-000000000000', 'OTHER', 'Other', 'أخرى', 10);

COMMENT ON TABLE purpose_of_finance IS 'Configurable purpose of finance reference data (per tenant)';
