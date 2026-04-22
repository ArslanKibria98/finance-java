-- V29__add_dunning_policy_roles_and_policies.sql
-- Adds collections_head, collections_agent roles + dunning.policies Casbin policies
-- Per Blueprint 17 & ProdDocs/06 — Admin-configurable product-wise delinquency rules.
--
-- Object: dunning.policies (collections-service — AdminDunningPolicyController)
-- Actions: create | read | update | delete
--
-- Endpoint map:
--   POST   /api/v1/admin/dunning-policies                 -> create
--   PUT    /api/v1/admin/dunning-policies/{id}            -> update
--   POST   /api/v1/admin/dunning-policies/{id}/activate   -> update
--   POST   /api/v1/admin/dunning-policies/{id}/deactivate -> update
--   POST   /api/v1/admin/dunning-policies/{id}/mark-default -> update
--   GET    /api/v1/admin/dunning-policies                 -> read
--   GET    /api/v1/admin/dunning-policies/{id}            -> read
--   GET    /api/v1/admin/dunning-policies/simulate        -> read
--   DELETE /api/v1/admin/dunning-policies/{id}            -> delete

-- ══════════════════════════════════════════════════════════════
-- 1. INSERT MISSING ROLES
-- ══════════════════════════════════════════════════════════════

INSERT INTO roles (tenant_id, role_code, role_name, description, is_system)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'collections_head',  'Collections Head',  'Owns dunning policy configuration: thresholds, late fees, escalation rules', true),
    ('00000000-0000-0000-0000-000000000001', 'collections_agent', 'Collections Agent', 'Executes collections activities; read-only on dunning policies', true)
ON CONFLICT DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- 2. CASBIN POLICIES — dunning.policies object
-- ══════════════════════════════════════════════════════════════

-- super_admin / admin: full access
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'super_admin', 'dunning.policies', '*')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'admin', 'dunning.policies', '*')
ON CONFLICT DO NOTHING;

-- collections_head: full CRUD on dunning policies
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'dunning.policies', 'create')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'dunning.policies', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'dunning.policies', 'update')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'dunning.policies', 'delete')
ON CONFLICT DO NOTHING;

-- collections_agent: read-only (to see what's enforced)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_agent', 'dunning.policies', 'read')
ON CONFLICT DO NOTHING;

-- product_admin: read + update (needs to bind product codes to policies)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'product_admin', 'dunning.policies', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'product_admin', 'dunning.policies', 'update')
ON CONFLICT DO NOTHING;

-- compliance_officer: read-only for audit
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'compliance_officer', 'dunning.policies', 'read')
ON CONFLICT DO NOTHING;

-- head_of_accounts: read-only (late fees sweep to charity fund GL account)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'head_of_accounts', 'dunning.policies', 'read')
ON CONFLICT DO NOTHING;

-- underwriter: read-only (uses product thresholds during origination)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'underwriter', 'dunning.policies', 'read')
ON CONFLICT DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- 3. SUMMARY
-- ══════════════════════════════════════════════════════════════
-- Roles added:        collections_head, collections_agent
-- Casbin object:      dunning.policies
-- Actions:            create | read | update | delete
-- Full access:        super_admin, admin, collections_head
-- Read-only:          collections_agent, compliance_officer, head_of_accounts, underwriter
-- Read + update:      product_admin (to bind product codes to existing policies)
