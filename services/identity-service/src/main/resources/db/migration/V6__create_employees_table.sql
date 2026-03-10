-- ============================================================================
-- EMPLOYEES TABLE
-- Employee management with Keycloak user linking
-- ============================================================================

CREATE TYPE employee_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED',
    'TERMINATED'
);

CREATE TABLE employees (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    keycloak_user_id UUID,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    address         TEXT,
    role_id         UUID REFERENCES roles(id),
    status          employee_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_employee_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_employees_tenant ON employees(tenant_id);
CREATE INDEX idx_employees_status ON employees(tenant_id, status);
CREATE INDEX idx_employees_keycloak ON employees(keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;
CREATE INDEX idx_employees_role ON employees(role_id) WHERE role_id IS NOT NULL;
