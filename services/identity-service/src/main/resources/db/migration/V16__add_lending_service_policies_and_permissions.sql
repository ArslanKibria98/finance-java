-- ============================================================================
-- V16: Lending Service — Complete Casbin Policies + Permissions + Module
-- KSA Islamic Financing Platform
--
-- Adds:
--   1. LENDING module to modules table
--   2. All lending-service permissions to permissions table
--   3. Role-permission assignments (role_permissions)
--   4. All missing casbin_rule entries for admin panel roles
-- ============================================================================

-- ============================================================================
-- 1. LENDING Module
-- ============================================================================
INSERT INTO modules (tenant_id, module_code, module_name, description, display_order)
VALUES ('00000000-0000-0000-0000-000000000001', 'LENDING', 'Lending', 'Loan origination, applications, and portfolio management', 13)
ON CONFLICT (tenant_id, module_code) DO NOTHING;

-- ============================================================================
-- 2. Lending Permissions Catalog
-- Maps directly to every @SecuredEndpoint in lending-service controllers
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT
    '00000000-0000-0000-0000-000000000001',
    p.permission_code,
    p.permission_name,
    p.description,
    'LENDING',
    p.action,
    m.id
FROM modules m,
(VALUES
    -- Loan Applications (LoanApplicationController)
    ('LOAN_APPLICATION_CREATE',  'Create Loan Application',        'Initiate a new loan application workflow',                    'POST'),
    ('LOAN_APPLICATION_READ',    'View Loan Applications',         'Read loan application details and lists',                     'GET'),
    ('LOAN_APPLICATION_MANAGE',  'Manage Loan Applications',       'Submit steps, approve, reject, disburse, cancel applications','POST'),
    ('LOAN_APP_TRACKER_READ',    'View Application Tracker',       'Read signal-driven workflow tracker status',                  'GET'),

    -- Loans (LoanController)
    ('LOAN_READ',                'View Loans',                     'Read active/historical loan records',                         'GET'),
    ('LOAN_OVERVIEW_READ',       'View Loan Overview',             'Read loan overview summary card',                             'GET'),
    ('LOAN_INSTALLMENTS_READ',   'View Loan Installments',         'Read loan installment/repayment schedule',                    'GET'),
    ('LOAN_CONTRACT_READ',       'View Loan Contract',             'Read/download loan contract document',                        'GET'),
    ('LOAN_RECEIPTS_READ',       'View Loan Receipts',             'Read payment receipts for a loan',                            'GET'),

    -- Finance Calculator (FinanceCalculatorController)
    ('FINANCE_CALCULATOR_READ',  'Use Finance Calculator',         'Calculate Islamic financing amounts and schedules',           'GET'),
    ('FINANCE_ELIGIBILITY_CHECK','Check Finance Eligibility',      'Run eligibility check for a product',                        'GET'),

    -- Dashboard (DashboardController)
    ('LENDING_DASHBOARD_READ',   'View Lending Dashboard',         'Read admin dashboard KPIs, portfolio stats, and recent apps', 'GET'),

    -- Banks (BankController)
    ('BANKS_READ',               'View Banks',                     'Read active bank reference data for disbursement/IBAN',       'GET'),

    -- Purpose of Finance (PurposeOfFinanceController)
    ('PURPOSE_OF_FINANCE_READ',  'View Purpose of Finance',        'Read purpose of finance reference data',                      'GET'),
    ('PURPOSE_OF_FINANCE_CREATE','Create Purpose of Finance',      'Add new purpose of finance entry',                            'POST'),
    ('PURPOSE_OF_FINANCE_UPDATE','Update Purpose of Finance',      'Edit existing purpose of finance entry',                      'PUT'),
    ('PURPOSE_OF_FINANCE_DELETE','Delete Purpose of Finance',      'Remove purpose of finance entry',                             'DELETE'),

    -- Eligibility Fields (EligibilityFieldController)
    ('ELIGIBILITY_FIELDS_READ',  'View Eligibility Fields',        'Read field definitions and product-field mappings',           'GET'),
    ('ELIGIBILITY_FIELDS_CREATE','Create Eligibility Fields',      'Define new dynamic eligibility field',                        'POST'),
    ('ELIGIBILITY_FIELDS_UPDATE','Update Eligibility Fields',      'Edit existing eligibility field definition',                  'PUT'),
    ('ELIGIBILITY_FIELDS_DELETE','Delete Eligibility Fields',      'Remove eligibility field definition',                         'DELETE'),
    ('ELIGIBILITY_FIELDS_MANAGE','Manage Product Field Assignments','Assign/unassign eligibility fields to products',             'POST')
) AS p(permission_code, permission_name, description, action)
WHERE m.module_code = 'LENDING'
  AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 3. Role-Permission Assignments
-- ============================================================================

-- super_admin: all lending permissions (already has wildcard in Casbin, but explicit here)
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'super_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.resource_type = 'LENDING'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- admin: all lending permissions
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.resource_type = 'LENDING'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- product_admin: manage config (purpose-of-finance, eligibility-fields) + read-only lending data
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'product_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOAN_APPLICATION_READ', 'LENDING_DASHBOARD_READ', 'BANKS_READ',
      'FINANCE_CALCULATOR_READ', 'FINANCE_ELIGIBILITY_CHECK',
      'PURPOSE_OF_FINANCE_READ', 'PURPOSE_OF_FINANCE_CREATE',
      'PURPOSE_OF_FINANCE_UPDATE', 'PURPOSE_OF_FINANCE_DELETE',
      'ELIGIBILITY_FIELDS_READ', 'ELIGIBILITY_FIELDS_CREATE',
      'ELIGIBILITY_FIELDS_UPDATE', 'ELIGIBILITY_FIELDS_DELETE',
      'ELIGIBILITY_FIELDS_MANAGE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- underwriter: review and decision on applications + portfolio visibility
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'underwriter'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOAN_APPLICATION_READ', 'LOAN_APPLICATION_MANAGE', 'LOAN_APP_TRACKER_READ',
      'LOAN_READ', 'LOAN_OVERVIEW_READ', 'LOAN_INSTALLMENTS_READ',
      'FINANCE_CALCULATOR_READ', 'FINANCE_ELIGIBILITY_CHECK',
      'LENDING_DASHBOARD_READ', 'BANKS_READ',
      'PURPOSE_OF_FINANCE_READ', 'ELIGIBILITY_FIELDS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- csa: customer support — assist with applications
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'csa'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOAN_APPLICATION_READ', 'LOAN_APPLICATION_MANAGE', 'LOAN_APP_TRACKER_READ',
      'LOAN_READ', 'LOAN_OVERVIEW_READ', 'LOAN_INSTALLMENTS_READ',
      'FINANCE_CALCULATOR_READ', 'FINANCE_ELIGIBILITY_CHECK',
      'LENDING_DASHBOARD_READ', 'BANKS_READ',
      'PURPOSE_OF_FINANCE_READ', 'ELIGIBILITY_FIELDS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- compliance_officer: audit read-only access
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'compliance_officer'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOAN_APPLICATION_READ', 'LOAN_APP_TRACKER_READ',
      'LOAN_READ', 'LOAN_OVERVIEW_READ', 'LOAN_INSTALLMENTS_READ',
      'FINANCE_CALCULATOR_READ',
      'LENDING_DASHBOARD_READ', 'BANKS_READ',
      'PURPOSE_OF_FINANCE_READ', 'ELIGIBILITY_FIELDS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- head_of_accounts: disbursement approval + financial visibility
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'head_of_accounts'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOAN_APPLICATION_READ', 'LOAN_APPLICATION_MANAGE', 'LOAN_APP_TRACKER_READ',
      'LOAN_READ', 'LOAN_OVERVIEW_READ', 'LOAN_INSTALLMENTS_READ',
      'FINANCE_CALCULATOR_READ',
      'LENDING_DASHBOARD_READ', 'BANKS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- customer: self-service — apply, track, view own loans
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'customer'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOAN_APPLICATION_CREATE', 'LOAN_APPLICATION_READ', 'LOAN_APPLICATION_MANAGE',
      'LOAN_APP_TRACKER_READ',
      'LOAN_READ', 'LOAN_OVERVIEW_READ', 'LOAN_INSTALLMENTS_READ',
      'LOAN_CONTRACT_READ', 'LOAN_RECEIPTS_READ',
      'FINANCE_CALCULATOR_READ', 'FINANCE_ELIGIBILITY_CHECK',
      'BANKS_READ', 'PURPOSE_OF_FINANCE_READ', 'ELIGIBILITY_FIELDS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================================
-- 4. Missing Casbin Rules (casbin_rule table)
--
-- Existing (from V8, V9, V13, V14, V15 — do NOT duplicate):
--   customer: loan-applications (create/read/manage), loans (read),
--             loan-applications.tracker (read), loans.* sub-resources,
--             finance.calculator (read), finance.eligibility (check)
--   admin:    loans (*), loan-applications.tracker (read), loans.* sub-resources,
--             finance.calculator (read), finance.eligibility (check)
--   underwriter: loans (read), loan-applications.tracker (read),
--               loans.overview+installments (read), finance.calculator (read)
--   csa:     loans (read), loan-applications.tracker (read),
--            loans.overview+installments (read), finance.calculator (read)
--   head_of_accounts: loans (read), loans.overview+installments (read)
--   compliance_officer: loans (read), loans.overview (read)
-- ============================================================================

-- ── ADMIN: Missing lending resources ────────────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'loan-applications', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'banks', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'dashboard', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'purpose-of-finance', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'eligibility-fields', '*');

-- ── PRODUCT_ADMIN: Lending config + read access ─────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'loan-applications', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'loan-applications.tracker', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'banks', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'dashboard', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'finance.calculator', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'finance.eligibility', 'check');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'purpose-of-finance', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'eligibility-fields', '*');

-- ── UNDERWRITER: Review & decision + portfolio ───────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'loan-applications', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'loan-applications', 'manage');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'banks', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'dashboard', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'finance.eligibility', 'check');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'purpose-of-finance', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'eligibility-fields', 'read');

-- ── CSA: Customer support ────────────────────────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'loan-applications', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'loan-applications', 'manage');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'banks', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'dashboard', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'finance.eligibility', 'check');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'purpose-of-finance', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'eligibility-fields', 'read');

-- ── COMPLIANCE_OFFICER: Audit read-only ──────────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'loan-applications', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'loan-applications.tracker', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'banks', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'dashboard', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'finance.calculator', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'purpose-of-finance', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'eligibility-fields', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'loans.contract', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'loans.receipts', 'read');

-- ── HEAD_OF_ACCOUNTS: Disbursement + financial ───────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loan-applications', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loan-applications', 'manage');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loan-applications.tracker', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'banks', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'dashboard', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'finance.calculator', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loans.contract', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loans.receipts', 'read');

-- ── CUSTOMER: Self-service form data ─────────────────────────────────────────
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'banks', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'purpose-of-finance', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'eligibility-fields', 'read');
