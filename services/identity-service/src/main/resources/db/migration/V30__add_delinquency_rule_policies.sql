-- V30__add_delinquency_rule_policies.sql
-- Adds delinquency.rules Casbin policies for collections-service per-product
-- delinquency configuration covering all 6 LMS lifecycle stages:
--   1=EARLY_SETTLEMENT, 2=DUE_LOAN, 3=LATE_PAYMENT,
--   4=WRITE_OFFS, 5=NON_PERFORMING_LOAN, 6=BROKEN_PROMISES
--
-- Per Blueprint 17 & ProdDocs/06 — Sharia-compliant late fee handling
-- routes penalties to a charity fund GL account (never revenue).
--
-- Object: delinquency.rules (collections-service — DelinquencyRuleController)
-- Actions: manage | read | delete
--
-- Endpoint map:
--   PUT    /api/v1/admin/delinquency-rules                       -> manage
--   POST   /api/v1/admin/delinquency-rules/{id}/configs/single   -> manage
--   POST   /api/v1/admin/delinquency-rules/{id}/configs/range    -> manage
--   DELETE /api/v1/admin/delinquency-rules/{id}/configs/{cid}    -> manage
--   GET    /api/v1/admin/delinquency-rules/{id}                  -> read
--   GET    /api/v1/admin/delinquency-rules?productId=...         -> read
--   DELETE /api/v1/admin/delinquency-rules/{id}                  -> delete

-- ══════════════════════════════════════════════════════════════
-- CASBIN POLICIES — delinquency.rules object
-- ══════════════════════════════════════════════════════════════

-- super_admin / admin: full access (wildcard already covers, explicit for clarity)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'super_admin', 'delinquency.rules', '*')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'admin', 'delinquency.rules', '*')
ON CONFLICT DO NOTHING;

-- collections_head: owns delinquency rule configuration
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'delinquency.rules', 'manage')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'delinquency.rules', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'delinquency.rules', 'delete')
ON CONFLICT DO NOTHING;

-- collections_agent: read-only (to see enforced thresholds)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_agent', 'delinquency.rules', 'read')
ON CONFLICT DO NOTHING;

-- product_admin: manage + read (binds product to delinquency rule set)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'product_admin', 'delinquency.rules', 'manage')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'product_admin', 'delinquency.rules', 'read')
ON CONFLICT DO NOTHING;

-- compliance_officer: read-only (audits Sharia charity-fund routing)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'compliance_officer', 'delinquency.rules', 'read')
ON CONFLICT DO NOTHING;

-- head_of_accounts: read-only (charity fund accounts are GL accounts)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'head_of_accounts', 'delinquency.rules', 'read')
ON CONFLICT DO NOTHING;

-- underwriter: read-only (origination references delinquency thresholds)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'underwriter', 'delinquency.rules', 'read')
ON CONFLICT DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUMMARY
-- ══════════════════════════════════════════════════════════════
-- Casbin object:      delinquency.rules
-- Actions:            manage | read | delete
-- Full access:        super_admin, admin, collections_head
-- Manage + read:      product_admin
-- Read-only:          collections_agent, compliance_officer, head_of_accounts, underwriter
