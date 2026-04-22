-- COA LOV fields and product-wise COA configuration mappings.

CREATE TABLE coa_field_lovs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    field_key               VARCHAR(100) NOT NULL,
    field_label_en          VARCHAR(255) NOT NULL,
    field_label_ar          VARCHAR(255),
    category                VARCHAR(50) NOT NULL,
    is_mandatory_default    BOOLEAN NOT NULL DEFAULT FALSE,
    display_order           INT NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_coa_field_lov UNIQUE (tenant_id, field_key)
);

CREATE INDEX idx_coa_field_lovs_tenant ON coa_field_lovs (tenant_id);
CREATE INDEX idx_coa_field_lovs_tenant_status ON coa_field_lovs (tenant_id, status);
CREATE INDEX idx_coa_field_lovs_category ON coa_field_lovs (tenant_id, category);

CREATE TRIGGER trg_coa_field_lovs_updated_at
    BEFORE UPDATE ON coa_field_lovs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE coa_configuration_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    product_code            VARCHAR(50) NOT NULL,
    profile_name            VARCHAR(100) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    effective_from          DATE,
    effective_to            DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_coa_config_profiles_tenant ON coa_configuration_profiles (tenant_id);
CREATE INDEX idx_coa_config_profiles_product ON coa_configuration_profiles (tenant_id, product_code);
CREATE INDEX idx_coa_config_profiles_status ON coa_configuration_profiles (tenant_id, status);

CREATE TRIGGER trg_coa_config_profiles_updated_at
    BEFORE UPDATE ON coa_configuration_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE coa_configuration_mappings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    profile_id              UUID NOT NULL REFERENCES coa_configuration_profiles(id) ON DELETE CASCADE,
    coa_field_id            UUID NOT NULL REFERENCES coa_field_lovs(id),
    account_id              UUID NOT NULL REFERENCES accounts(id),
    is_mandatory_override   BOOLEAN,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_coa_config_profile_field UNIQUE (profile_id, coa_field_id)
);

CREATE INDEX idx_coa_config_mappings_tenant ON coa_configuration_mappings (tenant_id);
CREATE INDEX idx_coa_config_mappings_profile ON coa_configuration_mappings (tenant_id, profile_id);
CREATE INDEX idx_coa_config_mappings_field ON coa_configuration_mappings (tenant_id, coa_field_id);
CREATE INDEX idx_coa_config_mappings_account ON coa_configuration_mappings (tenant_id, account_id);

CREATE TRIGGER trg_coa_config_mappings_updated_at
    BEFORE UPDATE ON coa_configuration_mappings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
