-- ============================================================================
-- V20: LOV (List of Values) Module — Separate permissions per LOV type
-- KSA Islamic Financing Platform
--
-- Adds:
--   1. New LOV module visible in the admin panel
--   2. Fine-grained permissions for each LOV type (5 types x 4 CRUD ops = 20)
--   3. Role-permission assignments for super_admin and admin roles
-- ============================================================================

-- ============================================================================
-- 1. LOV Module
-- ============================================================================
INSERT INTO modules (tenant_id, module_code, module_name, description, display_order)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'LOV',
    'LOV',
    'List of Values — EDD reference data (Source of Wealth, Funds, Income, Purpose of Finance, Net Worth)',
    7
) ON CONFLICT (tenant_id, module_code) DO NOTHING;

-- Shift display_order of modules after position 6 (Product onwards) to make room for LOV at 7
UPDATE modules
SET display_order = display_order + 1
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
  AND display_order >= 7
  AND module_code != 'LOV';

-- ============================================================================
-- 2. LOV Permissions (5 types x 4 CRUD = 20 permissions)
-- ============================================================================

-- Source of Wealth
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'LOV', p.act, m.id
FROM modules m,
(VALUES
    ('LOV_SOW_CREATE', 'Create Source of Wealth',  'Add new source of wealth dropdown option',    'POST'),
    ('LOV_SOW_READ',   'View Source of Wealth',    'Read source of wealth options (all/by ID)',   'GET'),
    ('LOV_SOW_UPDATE', 'Update Source of Wealth',  'Edit existing source of wealth option',       'PUT'),
    ('LOV_SOW_DELETE', 'Deactivate Source of Wealth', 'Soft-deactivate a source of wealth option', 'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'LOV' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- Source of Funds
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'LOV', p.act, m.id
FROM modules m,
(VALUES
    ('LOV_SOF_CREATE', 'Create Source of Funds',  'Add new source of funds dropdown option',    'POST'),
    ('LOV_SOF_READ',   'View Source of Funds',    'Read source of funds options (all/by ID)',   'GET'),
    ('LOV_SOF_UPDATE', 'Update Source of Funds',  'Edit existing source of funds option',       'PUT'),
    ('LOV_SOF_DELETE', 'Deactivate Source of Funds', 'Soft-deactivate a source of funds option', 'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'LOV' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- Source of Income
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'LOV', p.act, m.id
FROM modules m,
(VALUES
    ('LOV_SOI_CREATE', 'Create Source of Income',  'Add new source of income dropdown option',    'POST'),
    ('LOV_SOI_READ',   'View Source of Income',    'Read source of income options (all/by ID)',   'GET'),
    ('LOV_SOI_UPDATE', 'Update Source of Income',  'Edit existing source of income option',       'PUT'),
    ('LOV_SOI_DELETE', 'Deactivate Source of Income', 'Soft-deactivate a source of income option', 'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'LOV' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- Purpose of Finance
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'LOV', p.act, m.id
FROM modules m,
(VALUES
    ('LOV_POF_CREATE', 'Create Purpose of Finance',  'Add new purpose of finance dropdown option',    'POST'),
    ('LOV_POF_READ',   'View Purpose of Finance',    'Read purpose of finance options (all/by ID)',   'GET'),
    ('LOV_POF_UPDATE', 'Update Purpose of Finance',  'Edit existing purpose of finance option',       'PUT'),
    ('LOV_POF_DELETE', 'Deactivate Purpose of Finance', 'Soft-deactivate a purpose of finance option', 'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'LOV' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- Net Worth Ranges
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'LOV', p.act, m.id
FROM modules m,
(VALUES
    ('LOV_NWR_CREATE', 'Create Net Worth Range',  'Add new net worth range dropdown option',    'POST'),
    ('LOV_NWR_READ',   'View Net Worth Ranges',   'Read net worth range options (all/by ID)',   'GET'),
    ('LOV_NWR_UPDATE', 'Update Net Worth Range',  'Edit existing net worth range option',       'PUT'),
    ('LOV_NWR_DELETE', 'Deactivate Net Worth Range', 'Soft-deactivate a net worth range option', 'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'LOV' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 3. Assign LOV permissions to super_admin and admin roles
-- ============================================================================
INSERT INTO role_permissions (tenant_id, role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id
FROM roles r, permissions p
WHERE r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND r.role_code IN ('SUPER_ADMIN', 'ADMIN')
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOV_SOW_CREATE','LOV_SOW_READ','LOV_SOW_UPDATE','LOV_SOW_DELETE',
      'LOV_SOF_CREATE','LOV_SOF_READ','LOV_SOF_UPDATE','LOV_SOF_DELETE',
      'LOV_SOI_CREATE','LOV_SOI_READ','LOV_SOI_UPDATE','LOV_SOI_DELETE',
      'LOV_POF_CREATE','LOV_POF_READ','LOV_POF_UPDATE','LOV_POF_DELETE',
      'LOV_NWR_CREATE','LOV_NWR_READ','LOV_NWR_UPDATE','LOV_NWR_DELETE'
  )
ON CONFLICT DO NOTHING;

-- Also assign read-only LOV permissions to product_admin and csa
INSERT INTO role_permissions (tenant_id, role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id
FROM roles r, permissions p
WHERE r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND r.role_code IN ('PRODUCT_ADMIN', 'CSA')
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'LOV_SOW_READ','LOV_SOF_READ','LOV_SOI_READ','LOV_POF_READ','LOV_NWR_READ'
  )
ON CONFLICT DO NOTHING;

-- product_admin gets full CRUD on Purpose of Finance
INSERT INTO role_permissions (tenant_id, role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id
FROM roles r, permissions p
WHERE r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND r.role_code = 'PRODUCT_ADMIN'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN ('LOV_POF_CREATE','LOV_POF_UPDATE','LOV_POF_DELETE')
ON CONFLICT DO NOTHING;
