-- ============================================================================
-- V9: Dynamic Eligibility Fields
-- Product-specific eligibility questions (salary, dependents, etc.)
-- ============================================================================

-- Master field definitions (admin-configurable)
CREATE TABLE eligibility_field_definitions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    field_key       VARCHAR(100) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    name_ar         VARCHAR(255),
    description_en  VARCHAR(500),
    description_ar  VARCHAR(500),
    data_type       VARCHAR(30) NOT NULL DEFAULT 'NUMBER',
    input_type      VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    placeholder_en  VARCHAR(255),
    placeholder_ar  VARCHAR(255),
    unit            VARCHAR(30),
    min_value       NUMERIC(18,2),
    max_value       NUMERIC(18,2),
    is_required     BOOLEAN NOT NULL DEFAULT true,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_eligibility_field_tenant_key UNIQUE (tenant_id, field_key)
);

CREATE INDEX idx_elig_field_tenant ON eligibility_field_definitions(tenant_id);
CREATE INDEX idx_elig_field_active ON eligibility_field_definitions(tenant_id, is_active);

-- Product-to-field mapping (which fields are needed for which product)
CREATE TABLE product_eligibility_fields (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    product_id      UUID NOT NULL,
    field_id        UUID NOT NULL REFERENCES eligibility_field_definitions(id),
    is_required     BOOLEAN NOT NULL DEFAULT true,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_prod_elig_field UNIQUE (tenant_id, product_id, field_id)
);

CREATE INDEX idx_prod_elig_tenant_product ON product_eligibility_fields(tenant_id, product_id);

-- ============================================================================
-- Seed default eligibility fields
-- ============================================================================
-- Uses a CTE to get tenant_id=00000000-0000-0000-0000-000000000001 as system default.
-- Real tenants will clone or create their own.

INSERT INTO eligibility_field_definitions
    (tenant_id, field_key, name_en, name_ar, description_en, data_type, input_type, unit, min_value, max_value, is_required, sort_order)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'monthly_income', 'Monthly Income', 'الدخل الشهري',
     'Gross monthly salary in SAR', 'NUMBER', 'CURRENCY', 'SAR', 1000, 999999, true, 1),

    ('00000000-0000-0000-0000-000000000001', 'total_expenses', 'Total Monthly Expenses', 'إجمالي المصاريف الشهرية',
     'Total monthly expenses in SAR', 'NUMBER', 'CURRENCY', 'SAR', 0, 999999, true, 2),

    ('00000000-0000-0000-0000-000000000001', 'existing_liabilities', 'Existing Monthly Liabilities', 'الالتزامات الشهرية القائمة',
     'Existing monthly loan/credit obligations in SAR', 'NUMBER', 'CURRENCY', 'SAR', 0, 999999, true, 3),

    ('00000000-0000-0000-0000-000000000001', 'adult_dependents', 'Adult Dependents', 'المعالون البالغون',
     'Number of adult dependents', 'INTEGER', 'STEPPER', null, 0, 20, true, 4),

    ('00000000-0000-0000-0000-000000000001', 'child_dependents', 'Child Dependents', 'المعالون الأطفال',
     'Number of child dependents', 'INTEGER', 'STEPPER', null, 0, 20, true, 5),

    ('00000000-0000-0000-0000-000000000001', 'employment_sector', 'Employment Sector', 'قطاع العمل',
     'Government, Private, or Self-employed', 'STRING', 'SELECT', null, null, null, false, 6),

    ('00000000-0000-0000-0000-000000000001', 'employment_months', 'Employment Duration (Months)', 'مدة العمل (شهور)',
     'How many months at current employer', 'INTEGER', 'TEXT', 'months', 0, 600, false, 7),

    ('00000000-0000-0000-0000-000000000001', 'age', 'Age', 'العمر',
     'Customer age in years', 'INTEGER', 'TEXT', 'years', 18, 70, false, 8);
