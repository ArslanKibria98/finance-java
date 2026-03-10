-- ============================================================================
-- V2: Casbin Policy Rules (jCasbin JDBC Adapter)
-- KSA Islamic Financing Platform - Identity Service
-- ============================================================================

-- The casbin_rule table is auto-created by jCasbin JDBC Adapter.
-- We only seed initial policy rules here.

CREATE TABLE IF NOT EXISTS casbin_rule (
    id    SERIAL PRIMARY KEY,
    ptype VARCHAR(100) NOT NULL DEFAULT '',
    v0    VARCHAR(100) NOT NULL DEFAULT '',
    v1    VARCHAR(100) NOT NULL DEFAULT '',
    v2    VARCHAR(100) NOT NULL DEFAULT '',
    v3    VARCHAR(100) NOT NULL DEFAULT '',
    v4    VARCHAR(100) NOT NULL DEFAULT '',
    v5    VARCHAR(100) NOT NULL DEFAULT ''
);

CREATE INDEX idx_casbin_rule_ptype ON casbin_rule(ptype);
CREATE INDEX idx_casbin_rule_v0 ON casbin_rule(v0);

-- ============================================================================
-- SEED: Policy rules (p = policy, g = role grouping)
-- Format: ptype, v0 (subject/role), v1 (object/path), v2 (action/method)
-- ============================================================================

-- ── super_admin: Full access to all APIs ────────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'super_admin', '/api/v1/**', '*');

-- ── admin: Broad management access ──────────────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/workflows/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/workflows/**', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/workflows/**', 'DELETE');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/admin/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/admin/**', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/profiles/**', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/policies/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/authorize', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/token-info', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/roles/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/permissions/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/customers/**', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/partners/**', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/products/**', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/risk/**', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', '/api/v1/wallets/**', '*');

-- ── product_admin: Product and workflow management ──────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/workflows/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/workflows/start', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/admin/reports/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/profiles/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/authorize', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/token-info', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/products/**', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', '/api/v1/partners/**', 'GET');

-- ── partner_admin: Partner-scoped access ────────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/workflows/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/workflows/start', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/admin/reports/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/profiles/**', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/authorize', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/token-info', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/partners/**', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', '/api/v1/products/**', 'GET');

-- ── customer: Limited self-service access ───────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', '/api/v1/workflows/*/status', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', '/api/v1/workflows/start', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', '/api/v1/profiles/me', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', '/api/v1/profiles/me', 'PUT');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', '/api/v1/authorize', 'POST');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', '/api/v1/token-info', 'GET');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', '/api/v1/wallets/me/**', '*');

-- ============================================================================
-- SEED: Roles in the roles table (sync with Keycloak realm roles)
-- ============================================================================
INSERT INTO roles (tenant_id, role_code, role_name, role_name_ar, description, is_active, is_system)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'super_admin', 'Super Administrator', 'المدير الأعلى', 'Full system access with all privileges', true, true),
    ('00000000-0000-0000-0000-000000000001', 'admin', 'Administrator', 'المدير', 'Platform administration with broad management access', true, true),
    ('00000000-0000-0000-0000-000000000001', 'product_admin', 'Product Administrator', 'مدير المنتجات', 'Product catalog and configuration management', true, true),
    ('00000000-0000-0000-0000-000000000001', 'partner_admin', 'Partner Administrator', 'مدير الشركاء', 'Partner management and scoped access', true, true),
    ('00000000-0000-0000-0000-000000000001', 'customer', 'Customer', 'العميل', 'End-user customer with self-service access', true, true)
ON CONFLICT (tenant_id, role_code) DO NOTHING;

-- ============================================================================
-- SEED: Permissions catalog
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'WORKFLOW_READ', 'View Workflows', 'View workflow status and history', 'WORKFLOW', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'WORKFLOW_START', 'Start Workflows', 'Initiate new workflows', 'WORKFLOW', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'WORKFLOW_DELETE', 'Delete Workflows', 'Cancel or delete workflows', 'WORKFLOW', 'DELETE'),
    ('00000000-0000-0000-0000-000000000001', 'PROFILE_READ', 'View Profiles', 'Read customer/user profiles', 'PROFILE', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'PROFILE_WRITE', 'Edit Profiles', 'Create and update profiles', 'PROFILE', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'CUSTOMER_READ', 'View Customers', 'Read customer records', 'CUSTOMER', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'CUSTOMER_WRITE', 'Manage Customers', 'Create and update customer records', 'CUSTOMER', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'PRODUCT_READ', 'View Products', 'Read product catalog', 'PRODUCT', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'PRODUCT_WRITE', 'Manage Products', 'Create and update products', 'PRODUCT', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'PARTNER_READ', 'View Partners', 'Read partner profiles', 'PARTNER', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'PARTNER_WRITE', 'Manage Partners', 'Create and update partners', 'PARTNER', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'RISK_READ', 'View Risk Assessments', 'Read risk assessment results', 'RISK', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'RISK_WRITE', 'Run Risk Assessments', 'Execute risk checks and manage blacklists', 'RISK', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'WALLET_READ', 'View Wallets', 'Read wallet balances and transactions', 'WALLET', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'WALLET_WRITE', 'Manage Wallets', 'Top-up, debit, and manage wallets', 'WALLET', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'ROLE_READ', 'View Roles', 'Read role definitions', 'ROLE', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'ROLE_WRITE', 'Manage Roles', 'Create and update roles', 'ROLE', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'PERMISSION_READ', 'View Permissions', 'Read permission catalog', 'PERMISSION', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'PERMISSION_WRITE', 'Manage Permissions', 'Create and update permissions', 'PERMISSION', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'POLICY_READ', 'View Policies', 'Read Casbin policy rules', 'POLICY', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'POLICY_WRITE', 'Manage Policies', 'Create, update, and delete policy rules', 'POLICY', 'POST'),
    ('00000000-0000-0000-0000-000000000001', 'ADMIN_READ', 'View Admin Panel', 'Access admin dashboard and reports', 'ADMIN', 'GET'),
    ('00000000-0000-0000-0000-000000000001', 'ADMIN_WRITE', 'Admin Operations', 'Execute admin operations', 'ADMIN', 'POST')
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- SEED: Role-Permission assignments
-- ============================================================================

-- super_admin gets all permissions (via Casbin wildcard, but also map explicitly)
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'super_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- admin gets most permissions (excluding POLICY_WRITE)
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code NOT IN ('POLICY_WRITE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- product_admin: products, partners (read), workflows, profiles (read)
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'product_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN ('PRODUCT_READ', 'PRODUCT_WRITE', 'PARTNER_READ', 'WORKFLOW_READ', 'WORKFLOW_START', 'PROFILE_READ', 'ADMIN_READ')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- partner_admin: partners, products (read), workflows, profiles (read)
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'partner_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN ('PARTNER_READ', 'PARTNER_WRITE', 'PRODUCT_READ', 'WORKFLOW_READ', 'WORKFLOW_START', 'PROFILE_READ', 'ADMIN_READ')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- customer: limited self-service
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'customer'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN ('WORKFLOW_READ', 'WORKFLOW_START', 'PROFILE_READ', 'WALLET_READ', 'WALLET_WRITE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
