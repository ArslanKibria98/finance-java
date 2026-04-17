-- ============================================================================
-- V17: Complete Admin Panel Permissions — All Modules
-- KSA Islamic Financing Platform
--
-- Adds:
--   1. Missing modules: KYC, MIDDLEWARE, EMPLOYEE, ONBOARDING, PII, FRAUD
--   2. Fine-grained permissions for every @SecuredEndpoint across all services
--   3. Role-permission assignments for admin panel roles
-- ============================================================================

-- ============================================================================
-- 1. New Modules (KYC, MIDDLEWARE, EMPLOYEE, ONBOARDING, PII, FRAUD)
-- ============================================================================
INSERT INTO modules (tenant_id, module_code, module_name, description, display_order)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'KYC',        'KYC',         'Government identity verification (Nafath, Yakeen, Simah, GOSI)',  14),
    ('00000000-0000-0000-0000-000000000001', 'MIDDLEWARE',  'Middleware',  'Third-party API provider and client management',                   15),
    ('00000000-0000-0000-0000-000000000001', 'EMPLOYEE',    'Employee',    'Employee accounts and management',                                16),
    ('00000000-0000-0000-0000-000000000001', 'ONBOARDING',  'Onboarding',  'Customer onboarding workflow management',                          17),
    ('00000000-0000-0000-0000-000000000001', 'PII',         'PII Vault',   'Encrypted PII storage and erasure management',                    18),
    ('00000000-0000-0000-0000-000000000001', 'FRAUD',       'Fraud',       'Fraud rules and detection management',                            19)
ON CONFLICT (tenant_id, module_code) DO NOTHING;

-- ============================================================================
-- 2a. CUSTOMER module — customer-service fine-grained permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'CUSTOMER', p.act, m.id
FROM modules m,
(VALUES
    ('CUSTOMER_CREATE',               'Create Customer',              'Create new customer profile (CIF)',             'POST'),
    ('CUSTOMER_UPDATE',               'Update Customer',              'Update existing customer profile',              'PUT'),
    ('CUSTOMER_BANK_ACCOUNT_READ',    'View Customer Bank Accounts',  'Read customer bank account details',            'GET'),
    ('CUSTOMER_BANK_ACCOUNT_CREATE',  'Add Customer Bank Account',    'Add bank account to customer profile',          'POST'),
    ('CUSTOMER_EMPLOYMENT_CREATE',    'Add Customer Employment',      'Add employment record for customer',            'POST'),
    ('CUSTOMER_KYC_STATUS_UPDATE',    'Update Customer KYC Status',   'Update KYC verification status for customer',   'PUT'),
    ('REFERENCE_DATA_READ',           'View Reference Data',          'Read EDD reference data (source of wealth etc)','GET'),
    ('REFERENCE_DATA_CREATE',         'Create Reference Data',        'Add new reference data entry',                  'POST'),
    ('REFERENCE_DATA_UPDATE',         'Update Reference Data',        'Edit existing reference data entry',            'PUT'),
    ('REFERENCE_DATA_DELETE',         'Delete Reference Data',        'Remove reference data entry',                   'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'CUSTOMER' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2b. RISK module — risk-service fine-grained permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'RISK', p.act, m.id
FROM modules m,
(VALUES
    -- Assessment
    ('RISK_ASSESSMENT_READ',            'View Risk Assessments',              'Read customer risk assessment results',                  'GET'),
    ('RISK_ASSESSMENT_CREATE',          'Run Risk Assessment',                'Execute risk assessment on a customer',                  'POST'),
    ('RISK_ASSESSMENT_MANAGE',          'Manage Risk Assessments',            'Override, approve, or flag risk assessments',            'POST'),
    -- Audit
    ('RISK_AUDIT_READ',                 'View Risk Audit Trail',              'Read risk service audit logs',                           'GET'),
    -- Blacklist
    ('RISK_BLACKLIST_READ',             'View Blacklist',                     'Read sanctions and blacklist entries',                   'GET'),
    ('RISK_BLACKLIST_CHECK',            'Check Blacklist',                    'Screen entity against blacklist',                        'GET'),
    ('RISK_BLACKLIST_CREATE',           'Add to Blacklist',                   'Add entity to blacklist',                               'POST'),
    ('RISK_BLACKLIST_DELETE',           'Remove from Blacklist',              'Remove entity from blacklist',                           'DELETE'),
    -- Credit Scoring
    ('RISK_CREDIT_SCORING_READ',        'View Credit Scores',                 'Read credit scoring results and models',                 'GET'),
    ('RISK_CREDIT_SCORING_CREATE',      'Create Credit Score',                'Run credit scoring engine for customer',                 'POST'),
    ('RISK_CREDIT_SCORING_DELETE',      'Delete Credit Score',                'Remove credit scoring record',                           'DELETE'),
    -- Credit Scoring Fields
    ('RISK_CREDIT_SCORING_FIELDS_READ',   'View Credit Scoring Fields',       'Read scoring field definitions',                        'GET'),
    ('RISK_CREDIT_SCORING_FIELDS_CREATE', 'Create Credit Scoring Field',      'Define new scoring field',                              'POST'),
    ('RISK_CREDIT_SCORING_FIELDS_UPDATE', 'Update Credit Scoring Field',      'Edit scoring field definition',                         'PUT'),
    ('RISK_CREDIT_SCORING_FIELDS_DELETE', 'Delete Credit Scoring Field',      'Remove scoring field definition',                        'DELETE'),
    -- Devices
    ('RISK_DEVICES_READ',               'View Device Registry',               'Read registered device records',                        'GET'),
    ('RISK_DEVICES_CREATE',             'Register Device',                    'Register new device in risk registry',                  'POST'),
    ('RISK_DEVICES_UPDATE',             'Update Device',                      'Update device registry entry',                          'PUT'),
    ('RISK_DEVICES_DELETE',             'Remove Device',                      'Remove device from registry',                           'DELETE'),
    -- Entity Status
    ('RISK_ENTITY_STATUS_READ',         'View Entity Status',                 'Read entity freeze/block status',                       'GET'),
    ('RISK_ENTITY_STATUS_MANAGE',       'Manage Entity Status',               'Freeze, block, or unblock entities',                    'POST'),
    -- Fraud Rules (risk-service internal)
    ('RISK_FRAUD_RULES_READ',           'View Fraud Rules',                   'Read fraud detection rules (risk-service)',              'GET'),
    ('RISK_FRAUD_RULES_CREATE',         'Create Fraud Rule',                  'Define new fraud detection rule',                       'POST'),
    ('RISK_FRAUD_RULES_UPDATE',         'Update Fraud Rule',                  'Edit fraud detection rule',                             'PUT'),
    ('RISK_FRAUD_RULES_DELETE',         'Delete Fraud Rule',                  'Remove fraud detection rule',                           'DELETE'),
    -- LOV (List of Values)
    ('RISK_LOV_READ',                   'View Risk LOV',                      'Read risk list-of-values reference data',               'GET'),
    ('RISK_LOV_CREATE',                 'Create Risk LOV',                    'Add risk LOV entry',                                    'POST'),
    ('RISK_LOV_UPDATE',                 'Update Risk LOV',                    'Edit risk LOV entry',                                   'PUT'),
    ('RISK_LOV_MANAGE',                 'Manage Risk LOV',                    'Bulk manage risk LOV entries',                          'POST'),
    -- Parameters
    ('RISK_PARAMETERS_READ',            'View Risk Parameters',               'Read risk scoring parameters and weights',              'GET'),
    ('RISK_PARAMETERS_CREATE',          'Create Risk Parameter',              'Add new risk parameter',                                'POST'),
    ('RISK_PARAMETERS_UPDATE',          'Update Risk Parameter',              'Edit existing risk parameter',                          'PUT'),
    ('RISK_PARAMETERS_MANAGE',          'Manage Risk Parameters',             'Bulk manage risk parameters',                           'POST'),
    -- Reviews
    ('RISK_REVIEWS_READ',               'View Risk Reviews',                  'Read risk review tasks',                                'GET'),
    ('RISK_REVIEWS_MANAGE',             'Manage Risk Reviews',                'Assign, approve, or reject risk review tasks',          'POST'),
    -- Scenarios
    ('RISK_SCENARIOS_READ',             'View Risk Scenarios',                'Read AML/fraud scenario rules',                         'GET'),
    ('RISK_SCENARIOS_CREATE',           'Create Risk Scenario',               'Define new risk scenario rule',                         'POST'),
    ('RISK_SCENARIOS_UPDATE',           'Update Risk Scenario',               'Edit risk scenario rule',                               'PUT'),
    ('RISK_SCENARIOS_MANAGE',           'Manage Risk Scenarios',              'Activate, deactivate risk scenarios',                   'POST'),
    -- Tenant Config
    ('RISK_TENANT_CONFIG_READ',         'View Risk Tenant Config',            'Read tenant risk configuration',                        'GET'),
    ('RISK_TENANT_CONFIG_CREATE',       'Create Risk Tenant Config',          'Add risk tenant configuration entry',                   'POST'),
    ('RISK_TENANT_CONFIG_UPDATE',       'Update Risk Tenant Config',          'Edit risk tenant configuration',                        'PUT'),
    ('RISK_TENANT_CONFIG_MANAGE',       'Manage Risk Tenant Config',          'Manage all risk tenant config settings',                'POST'),
    -- Thresholds
    ('RISK_THRESHOLDS_READ',            'View Risk Thresholds',               'Read risk scoring thresholds',                          'GET'),
    ('RISK_THRESHOLDS_CREATE',          'Create Risk Threshold',              'Define new risk threshold',                             'POST'),
    ('RISK_THRESHOLDS_UPDATE',          'Update Risk Threshold',              'Edit risk threshold',                                   'PUT'),
    ('RISK_THRESHOLDS_MANAGE',          'Manage Risk Thresholds',             'Bulk manage risk thresholds',                           'POST')
) AS p(code, name, descr, act)
WHERE m.module_code = 'RISK' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2c. PRODUCT module — product-service fine-grained permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'PRODUCT', p.act, m.id
FROM modules m,
(VALUES
    -- Products
    ('PRODUCT_CREATE',                   'Create Product',                    'Create new Islamic financing product',                   'POST'),
    ('PRODUCT_UPDATE',                   'Update Product',                    'Edit existing product details',                          'PUT'),
    ('PRODUCT_DELETE',                   'Delete Product',                    'Remove product from catalog',                            'DELETE'),
    ('PRODUCT_MANAGE',                   'Manage Product',                    'Activate, deactivate, publish product',                  'POST'),
    -- Categories
    ('PRODUCT_CATEGORY_READ',            'View Product Categories',           'Read product category definitions',                      'GET'),
    ('PRODUCT_CATEGORY_CREATE',          'Create Product Category',           'Add new product category',                               'POST'),
    ('PRODUCT_CATEGORY_UPDATE',          'Update Product Category',           'Edit product category',                                  'PUT'),
    ('PRODUCT_CATEGORY_DELETE',          'Delete Product Category',           'Remove product category',                                'DELETE'),
    -- Documents
    ('PRODUCT_DOCUMENT_READ',            'View Product Documents',            'Read product document templates',                        'GET'),
    ('PRODUCT_DOCUMENT_CREATE',          'Upload Product Document',           'Upload product document or template',                    'POST'),
    ('PRODUCT_DOCUMENT_UPDATE',          'Update Product Document',           'Edit product document',                                  'PUT'),
    ('PRODUCT_DOCUMENT_DELETE',          'Delete Product Document',           'Remove product document',                                'DELETE'),
    -- Product-Partner assignments
    ('PRODUCT_PARTNER_CREATE',           'Assign Partner to Product',         'Link a partner to a product',                           'POST'),
    ('PRODUCT_PARTNER_DELETE',           'Remove Partner from Product',       'Unlink partner from product',                            'DELETE'),
    -- Product Settings
    ('PRODUCT_SETTINGS_UPDATE',          'Update Product Settings',           'Update product configuration settings',                  'PUT'),
    -- Countries
    ('COUNTRY_READ',                     'View Countries',                    'Read country configuration list',                        'GET'),
    ('COUNTRY_CREATE',                   'Create Country',                    'Add country configuration',                              'POST'),
    ('COUNTRY_UPDATE',                   'Update Country',                    'Edit country configuration',                             'PUT'),
    ('COUNTRY_DELETE',                   'Delete Country',                    'Remove country configuration',                           'DELETE'),
    -- Contract Templates
    ('CONTRACT_TEMPLATE_READ',           'View Contract Templates',           'Read financing contract templates',                      'GET'),
    ('CONTRACT_TEMPLATE_CREATE',         'Create Contract Template',          'Add new contract template',                              'POST'),
    ('CONTRACT_TEMPLATE_UPDATE',         'Update Contract Template',          'Edit contract template',                                 'PUT'),
    ('CONTRACT_TEMPLATE_DELETE',         'Delete Contract Template',          'Remove contract template',                               'DELETE'),
    -- Template Types
    ('TEMPLATE_TYPE_READ',               'View Template Types',               'Read contract template type definitions',                'GET'),
    ('TEMPLATE_TYPE_CREATE',             'Create Template Type',              'Add new template type',                                  'POST'),
    ('TEMPLATE_TYPE_UPDATE',             'Update Template Type',              'Edit template type',                                     'PUT'),
    ('TEMPLATE_TYPE_DELETE',             'Delete Template Type',              'Remove template type',                                   'DELETE'),
    -- Approval Condition Fields
    ('APPROVAL_CONDITION_FIELD_READ',    'View Approval Condition Fields',    'Read product approval condition field definitions',      'GET'),
    ('APPROVAL_CONDITION_FIELD_CREATE',  'Create Approval Condition Field',   'Define new approval condition field',                    'POST'),
    ('APPROVAL_CONDITION_FIELD_UPDATE',  'Update Approval Condition Field',   'Edit approval condition field',                         'PUT'),
    ('APPROVAL_CONDITION_FIELD_DELETE',  'Delete Approval Condition Field',   'Remove approval condition field',                        'DELETE'),
    -- Credit Scoring Fields (product side)
    ('PRODUCT_CREDIT_SCORING_FIELDS_READ', 'View Product Credit Scoring Fields', 'Read credit scoring field definitions in product',   'GET')
) AS p(code, name, descr, act)
WHERE m.module_code = 'PRODUCT' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2d. PARTNER module — partner-service fine-grained permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'PARTNER', p.act, m.id
FROM modules m,
(VALUES
    ('PARTNER_CREATE',  'Create Partner',  'Add new partner organization',  'POST'),
    ('PARTNER_UPDATE',  'Update Partner',  'Edit partner details',          'PUT'),
    ('PARTNER_MANAGE',  'Manage Partner',  'Activate or deactivate partner','POST')
) AS p(code, name, descr, act)
WHERE m.module_code = 'PARTNER' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2e. WALLET module — wallet-service fine-grained permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'WALLET', p.act, m.id
FROM modules m,
(VALUES
    ('WALLET_CREATE', 'Create Wallet',  'Create new customer digital wallet', 'POST'),
    ('WALLET_MANAGE', 'Manage Wallet',  'Top-up, block, or adjust wallet',    'POST')
) AS p(code, name, descr, act)
WHERE m.module_code = 'WALLET' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2f. PROFILE module — global-profile-service fine-grained permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'PROFILE', p.act, m.id
FROM modules m,
(VALUES
    ('PROFILE_CREATE',              'Create Global Profile',        'Create a new global customer profile (zero-PII)', 'POST'),
    ('PROFILE_REGIONAL_CREATE',     'Create Regional Profile',      'Create regional profile linked to global UID',    'POST'),
    ('PROFILE_ACCESS_TOKEN_CREATE', 'Generate PII Access Token',    'Issue a time-limited PII vault access token',     'POST')
) AS p(code, name, descr, act)
WHERE m.module_code = 'PROFILE' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2g. KYC module (new) — kyc-adapter-service permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'KYC', p.act, m.id
FROM modules m,
(VALUES
    ('KYC_TAHAKUK_VERIFY',   'Verify Tahakuk Identity',  'Verify national identity via Tahakuk API',          'POST'),
    ('KYC_NAFATH_INITIATE',  'Initiate Nafath Auth',     'Initiate Nafath digital identity authentication',   'POST'),
    ('KYC_NAFATH_STATUS',    'Check Nafath Status',      'Poll Nafath authentication session status',         'GET'),
    ('KYC_YAKEEN_VERIFY',    'Verify via Yakeen',        'Fetch and verify NIC data via Yakeen API',          'POST'),
    ('KYC_SCREENING_CHECK',  'Run Screening Check',      'Screen customer against AML/CFT sanctions lists',   'POST'),
    ('KYC_GOSI_FETCH',       'Fetch GOSI Data',          'Retrieve employment data from GOSI integration',    'GET')
) AS p(code, name, descr, act)
WHERE m.module_code = 'KYC' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2h. MIDDLEWARE module (new) — middleware-third-party permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'MIDDLEWARE', p.act, m.id
FROM modules m,
(VALUES
    ('MIDDLEWARE_PROVIDERS_READ',       'View API Providers',       'Read third-party API provider catalog',             'GET'),
    ('MIDDLEWARE_PROVIDERS_MANAGE',     'Manage API Providers',     'Create, edit, activate/deactivate API providers',   'POST'),
    ('MIDDLEWARE_PROVIDER_APIS_READ',   'View Provider APIs',       'Read API definitions for providers',                'GET'),
    ('MIDDLEWARE_PROVIDER_APIS_MANAGE', 'Manage Provider APIs',     'Create and configure provider API definitions',     'POST'),
    ('MIDDLEWARE_CLIENTS_MANAGE',       'Manage API Clients',       'Create and manage middleware API clients',          'POST'),
    ('MIDDLEWARE_CLIENT_ACCESS_MANAGE', 'Manage Client Access',     'Grant/revoke client access to provider APIs',       'POST'),
    ('MIDDLEWARE_ENV_CONFIGS_READ',     'View Env Configs',         'Read middleware environment configurations',         'GET'),
    ('MIDDLEWARE_ENV_CONFIGS_MANAGE',   'Manage Env Configs',       'Create and manage environment configurations',      'POST'),
    ('MIDDLEWARE_CALLBACKS_MANAGE',     'Manage Callbacks',         'Configure webhook callback endpoints',              'POST'),
    ('MIDDLEWARE_LOGS_READ',            'View Request Logs',        'Read third-party API request/response logs',        'GET')
) AS p(code, name, descr, act)
WHERE m.module_code = 'MIDDLEWARE' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2i. EMPLOYEE module (new) — identity-service employee management
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'EMPLOYEE', p.act, m.id
FROM modules m,
(VALUES
    ('EMPLOYEE_READ',   'View Employees',   'Read employee list and profiles',   'GET'),
    ('EMPLOYEE_CREATE', 'Create Employee',  'Create new employee account',       'POST'),
    ('EMPLOYEE_UPDATE', 'Update Employee',  'Edit employee profile or status',   'PUT'),
    ('EMPLOYEE_DELETE', 'Delete Employee',  'Remove employee account',           'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'EMPLOYEE' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2j. ONBOARDING module (new) — onboarding-workflow-service permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'ONBOARDING', p.act, m.id
FROM modules m,
(VALUES
    ('ONBOARDING_STATUS_READ', 'View Onboarding Status', 'Read customer onboarding workflow status', 'GET'),
    ('ONBOARDING_UPDATE',      'Update Onboarding',      'Send signals to onboarding workflow',      'POST')
) AS p(code, name, descr, act)
WHERE m.module_code = 'ONBOARDING' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2k. PII module (new) — pii-vault-service permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'PII', p.act, m.id
FROM modules m,
(VALUES
    ('PII_READ',   'Read PII Data',   'Read encrypted PII from vault (with access token)', 'GET'),
    ('PII_CREATE', 'Store PII Data',  'Store encrypted PII in vault',                      'POST'),
    ('PII_DELETE', 'Erase PII Data',  'Execute GDPR/PDPL right-to-erasure on PII vault',  'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'PII' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2l. FRAUD module (new) — fraud-service permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'FRAUD', p.act, m.id
FROM modules m,
(VALUES
    ('FRAUD_RULES_READ',   'View Fraud Rules',   'Read fraud detection rules (fraud-service)', 'GET'),
    ('FRAUD_RULES_UPDATE', 'Update Fraud Rules', 'Edit fraud detection rules',                 'PUT')
) AS p(code, name, descr, act)
WHERE m.module_code = 'FRAUD' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2m. ROLE/PERMISSION/POLICY — fine-grained identity-service permissions
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, p.rt, p.act, m.id
FROM modules m,
(VALUES
    ('PERMISSION_DELETE',   'PERMISSION', 'Delete Permission',    'Remove a permission from the catalog',           'DELETE'),
    ('POLICY_CREATE',       'POLICY',     'Create Policy',        'Add a new Casbin access policy rule',            'POST'),
    ('POLICY_DELETE',       'POLICY',     'Delete Policy',        'Remove a Casbin access policy rule',             'DELETE'),
    ('POLICY_MANAGE',       'POLICY',     'Manage Policies',      'Reload and manage all policy rules',             'POST'),
    ('POLICY_AUTHORIZE',    'POLICY',     'Authorize Request',    'Perform inline authorization check',             'POST'),
    ('ROLE_DELETE',         'ROLE',       'Delete Role',          'Remove a role definition',                       'DELETE')
) AS p(code, rt, name, descr, act)
WHERE m.module_code = p.rt AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 3. Role-Permission Assignments for new permissions
-- ============================================================================

-- ── super_admin: ALL new permissions ─────────────────────────────────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'super_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.resource_type IN ('CUSTOMER','RISK','PRODUCT','PARTNER','WALLET','PROFILE',
                          'KYC','MIDDLEWARE','EMPLOYEE','ONBOARDING','PII','FRAUD',
                          'PERMISSION','POLICY','ROLE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── admin: ALL new permissions (full management) ──────────────────────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.resource_type IN ('CUSTOMER','RISK','PRODUCT','PARTNER','WALLET','PROFILE',
                          'KYC','MIDDLEWARE','EMPLOYEE','ONBOARDING','PII','FRAUD',
                          'PERMISSION','POLICY','ROLE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── product_admin: Product catalog + country management ───────────────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'product_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'PRODUCT_CREATE','PRODUCT_UPDATE','PRODUCT_DELETE','PRODUCT_MANAGE',
      'PRODUCT_CATEGORY_READ','PRODUCT_CATEGORY_CREATE','PRODUCT_CATEGORY_UPDATE','PRODUCT_CATEGORY_DELETE',
      'PRODUCT_DOCUMENT_READ','PRODUCT_DOCUMENT_CREATE','PRODUCT_DOCUMENT_UPDATE','PRODUCT_DOCUMENT_DELETE',
      'PRODUCT_PARTNER_CREATE','PRODUCT_PARTNER_DELETE','PRODUCT_SETTINGS_UPDATE',
      'COUNTRY_READ','COUNTRY_CREATE','COUNTRY_UPDATE','COUNTRY_DELETE',
      'CONTRACT_TEMPLATE_READ','CONTRACT_TEMPLATE_CREATE','CONTRACT_TEMPLATE_UPDATE','CONTRACT_TEMPLATE_DELETE',
      'TEMPLATE_TYPE_READ','TEMPLATE_TYPE_CREATE','TEMPLATE_TYPE_UPDATE','TEMPLATE_TYPE_DELETE',
      'APPROVAL_CONDITION_FIELD_READ','APPROVAL_CONDITION_FIELD_CREATE','APPROVAL_CONDITION_FIELD_UPDATE','APPROVAL_CONDITION_FIELD_DELETE',
      'PRODUCT_CREDIT_SCORING_FIELDS_READ',
      'PARTNER_READ','PARTNER_CREATE','PARTNER_UPDATE','PARTNER_MANAGE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── underwriter: Read-only across all modules + risk management ───────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'underwriter'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'CUSTOMER_BANK_ACCOUNT_READ','REFERENCE_DATA_READ',
      'RISK_ASSESSMENT_READ','RISK_ASSESSMENT_CREATE','RISK_ASSESSMENT_MANAGE',
      'RISK_BLACKLIST_READ','RISK_BLACKLIST_CHECK',
      'RISK_CREDIT_SCORING_READ','RISK_CREDIT_SCORING_FIELDS_READ',
      'RISK_ENTITY_STATUS_READ','RISK_FRAUD_RULES_READ',
      'RISK_PARAMETERS_READ','RISK_REVIEWS_READ','RISK_REVIEWS_MANAGE',
      'RISK_SCENARIOS_READ','RISK_THRESHOLDS_READ',
      'PRODUCT_CATEGORY_READ','PRODUCT_DOCUMENT_READ',
      'COUNTRY_READ','CONTRACT_TEMPLATE_READ','TEMPLATE_TYPE_READ',
      'APPROVAL_CONDITION_FIELD_READ','PRODUCT_CREDIT_SCORING_FIELDS_READ',
      'PARTNER_READ','KYC_SCREENING_CHECK','KYC_GOSI_FETCH'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── csa: Customer support operations ─────────────────────────────────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'csa'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'CUSTOMER_CREATE','CUSTOMER_UPDATE','CUSTOMER_BANK_ACCOUNT_READ','CUSTOMER_BANK_ACCOUNT_CREATE',
      'CUSTOMER_EMPLOYMENT_CREATE','CUSTOMER_KYC_STATUS_UPDATE','REFERENCE_DATA_READ',
      'RISK_BLACKLIST_READ','RISK_BLACKLIST_CHECK',
      'KYC_TAHAKUK_VERIFY','KYC_NAFATH_INITIATE','KYC_NAFATH_STATUS',
      'KYC_YAKEEN_VERIFY','KYC_SCREENING_CHECK','KYC_GOSI_FETCH',
      'PROFILE_READ','WALLET_READ','ONBOARDING_STATUS_READ','ONBOARDING_UPDATE',
      'PRODUCT_CATEGORY_READ','PARTNER_READ','COUNTRY_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── compliance_officer: Audit read-only ──────────────────────────────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'compliance_officer'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'CUSTOMER_BANK_ACCOUNT_READ','REFERENCE_DATA_READ','CUSTOMER_KYC_STATUS_UPDATE',
      'RISK_ASSESSMENT_READ','RISK_AUDIT_READ','RISK_BLACKLIST_READ','RISK_BLACKLIST_CHECK',
      'RISK_BLACKLIST_CREATE','RISK_BLACKLIST_DELETE',
      'RISK_CREDIT_SCORING_READ','RISK_FRAUD_RULES_READ',
      'RISK_LOV_READ','RISK_PARAMETERS_READ','RISK_REVIEWS_READ',
      'RISK_SCENARIOS_READ','RISK_THRESHOLDS_READ',
      'FRAUD_RULES_READ','PII_READ','PROFILE_READ',
      'ONBOARDING_STATUS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── head_of_accounts: Wallet + financial oversight ───────────────────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'head_of_accounts'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'CUSTOMER_BANK_ACCOUNT_READ','REFERENCE_DATA_READ',
      'WALLET_CREATE','WALLET_MANAGE',
      'RISK_CREDIT_SCORING_READ','RISK_AUDIT_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── partner_admin: Partner management ────────────────────────────────────────
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'partner_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'PARTNER_CREATE','PARTNER_UPDATE','PARTNER_MANAGE',
      'PRODUCT_CATEGORY_READ','COUNTRY_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
