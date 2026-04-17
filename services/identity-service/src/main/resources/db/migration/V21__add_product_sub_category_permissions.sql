-- ============================================================================
-- V21: Product Sub-Category Permissions
-- KSA Islamic Financing Platform
--
-- Adds:
--   1. Permissions catalog entries for product sub-categories (4 CRUD ops)
--   2. Casbin policies for roles: admin, product_admin, csa, underwriter
--   3. Role-permission assignments for all relevant roles
-- ============================================================================

-- ============================================================================
-- 1. Permissions catalog — product sub-categories
-- ============================================================================
INSERT INTO permissions (tenant_id, permission_code, permission_name, description, resource_type, action, module_id)
SELECT '00000000-0000-0000-0000-000000000001', p.code, p.name, p.descr, 'PRODUCT', p.act, m.id
FROM modules m,
(VALUES
    ('PRODUCT_SUB_CATEGORY_READ',   'View Product Sub Categories',   'Read product sub-category definitions',   'GET'),
    ('PRODUCT_SUB_CATEGORY_CREATE', 'Create Product Sub Category',   'Add new product sub-category',            'POST'),
    ('PRODUCT_SUB_CATEGORY_UPDATE', 'Update Product Sub Category',   'Edit product sub-category',               'PUT'),
    ('PRODUCT_SUB_CATEGORY_DELETE', 'Delete Product Sub Category',   'Remove product sub-category',             'DELETE')
) AS p(code, name, descr, act)
WHERE m.module_code = 'PRODUCT' AND m.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, permission_code) DO NOTHING;

-- ============================================================================
-- 2. Casbin policies — product-sub-categories resource
-- ============================================================================

-- admin: full access
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'product-sub-categories', '*')
ON CONFLICT DO NOTHING;

-- product_admin: full access (owns product catalog)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'product-sub-categories', '*')
ON CONFLICT DO NOTHING;

-- csa: read-only
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'product-sub-categories', 'read')
ON CONFLICT DO NOTHING;

-- underwriter: read-only
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'product-sub-categories', 'read')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 3. Role-permission assignments
-- ============================================================================

-- admin: full CRUD on sub-categories
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'PRODUCT_SUB_CATEGORY_READ', 'PRODUCT_SUB_CATEGORY_CREATE',
      'PRODUCT_SUB_CATEGORY_UPDATE', 'PRODUCT_SUB_CATEGORY_DELETE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- product_admin: full CRUD on sub-categories
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'product_admin'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code IN (
      'PRODUCT_SUB_CATEGORY_READ', 'PRODUCT_SUB_CATEGORY_CREATE',
      'PRODUCT_SUB_CATEGORY_UPDATE', 'PRODUCT_SUB_CATEGORY_DELETE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- csa: read-only
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'csa'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code = 'PRODUCT_SUB_CATEGORY_READ'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- underwriter: read-only
INSERT INTO role_permissions (tenant_id, role_id, permission_id, effect)
SELECT '00000000-0000-0000-0000-000000000001', r.id, p.id, 'ALLOW'
FROM roles r, permissions p
WHERE r.role_code = 'underwriter'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.permission_code = 'PRODUCT_SUB_CATEGORY_READ'
ON CONFLICT (role_id, permission_id) DO NOTHING;
