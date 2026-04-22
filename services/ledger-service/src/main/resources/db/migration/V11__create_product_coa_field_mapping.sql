-- ============================================================================
-- PRODUCT ↔ COA FIELD MAPPING
-- Maps COA fields to products and links selected accounts
-- ============================================================================

-- Table 1: COA Field Definitions (types of GL accounts for products)
CREATE TABLE coa_fields (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    field_code              VARCHAR(50) NOT NULL,
    field_name              VARCHAR(255) NOT NULL,
    field_name_ar           VARCHAR(255),
    field_type              VARCHAR(50) NOT NULL,
    description             TEXT,
    is_mandatory            BOOLEAN NOT NULL DEFAULT FALSE,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_coa_field_code UNIQUE (tenant_id, field_code)
);

CREATE INDEX idx_coa_fields_tenant ON coa_fields(tenant_id);
CREATE INDEX idx_coa_fields_active ON coa_fields(tenant_id, is_active);

-- Table 2: Product ↔ COA Field Assignment (which fields belong to which product)
CREATE TABLE product_coa_field_mappings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    product_id              UUID NOT NULL,
    coa_field_id            UUID NOT NULL REFERENCES coa_fields(id),
    assignment_order        INT NOT NULL DEFAULT 0,
    is_assigned             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_product_coa_field UNIQUE (tenant_id, product_id, coa_field_id)
);

CREATE INDEX idx_product_coa_tenant ON product_coa_field_mappings(tenant_id);
CREATE INDEX idx_product_coa_product ON product_coa_field_mappings(tenant_id, product_id);
CREATE INDEX idx_product_coa_field ON product_coa_field_mappings(tenant_id, coa_field_id);

-- Table 3: Product COA Field Account Selection (final mapping with selected accounts)
CREATE TABLE product_coa_account_mappings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    product_id              UUID NOT NULL,
    coa_field_id            UUID NOT NULL REFERENCES coa_fields(id),
    account_id              UUID NOT NULL REFERENCES accounts(id),
    account_code            VARCHAR(50) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_product_field_account UNIQUE (tenant_id, product_id, coa_field_id),
    CONSTRAINT fk_product_coa_account FOREIGN KEY (tenant_id, product_id, coa_field_id)
        REFERENCES product_coa_field_mappings(tenant_id, product_id, coa_field_id)
);

CREATE INDEX idx_product_acct_tenant ON product_coa_account_mappings(tenant_id);
CREATE INDEX idx_product_acct_product ON product_coa_account_mappings(tenant_id, product_id);
CREATE INDEX idx_product_acct_field ON product_coa_account_mappings(tenant_id, coa_field_id);
CREATE INDEX idx_product_acct_account ON product_coa_account_mappings(tenant_id, account_id);
CREATE INDEX idx_product_acct_status ON product_coa_account_mappings(tenant_id, status);

-- Table 4: Audit/Change log
CREATE TABLE product_coa_audit_log (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    product_id              UUID NOT NULL,
    action                  VARCHAR(50) NOT NULL,
    entity_type             VARCHAR(100) NOT NULL,
    entity_id               UUID,
    old_values              JSONB,
    new_values              JSONB,
    changed_by              UUID,
    changed_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_tenant ON product_coa_audit_log(tenant_id);
CREATE INDEX idx_audit_product ON product_coa_audit_log(tenant_id, product_id);
CREATE INDEX idx_audit_changed_at ON product_coa_audit_log(tenant_id, changed_at DESC);

-- ============================================================================
-- SAMPLE COA FIELDS
-- ============================================================================

DO $$
DECLARE
    test_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    system_user_id UUID := '00000000-0000-0000-0000-000000000000';
BEGIN
    INSERT INTO coa_fields (tenant_id, field_code, field_name, field_name_ar, field_type, description, is_mandatory)
    VALUES
        (test_tenant_id, 'ASSET_ACCOUNT', 'Asset Account', 'اثاثیات اکاؤنٹ', 'ASSET', 'GL Account for asset', TRUE),
        (test_tenant_id, 'LIABILITY_ACCOUNT', 'Liability Account', 'ذمہ داری اکاؤنٹ', 'LIABILITY', 'GL Account for liability', TRUE),
        (test_tenant_id, 'INCOME_ACCOUNT', 'Income/Profit Account', 'آمدنی اکاؤنٹ', 'INCOME', 'GL Account for income', TRUE),
        (test_tenant_id, 'EXPENSE_ACCOUNT', 'Expense Account', 'اخراجات اکاؤنٹ', 'EXPENSE', 'GL Account for expenses', FALSE),
        (test_tenant_id, 'CASH_ACCOUNT', 'Cash Account', 'نقد رقم اکاؤنٹ', 'ASSET', 'GL Account for cash', TRUE),
        (test_tenant_id, 'PROFIT_ACCOUNT', 'Murabaha Profit', 'مرابحہ منافع', 'INCOME', 'GL Account for Murabaha profit', TRUE);

    RAISE NOTICE '✓ Sample COA fields inserted';
END $$;
