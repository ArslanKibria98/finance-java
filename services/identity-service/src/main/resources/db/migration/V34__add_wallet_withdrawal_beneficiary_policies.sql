-- ============================================================================
-- V34: Casbin policies for wallet withdrawal + IBAN beneficiary endpoints
-- (objects: wallet.withdrawals, wallet.beneficiaries)
-- ============================================================================
-- super_admin already has ('*','*') wildcard via V3 — no additions needed.
-- ============================================================================

-- ─── wallet.withdrawals ─────────────────────────────────────────────────────
-- admin: full
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',           'wallet.withdrawals', '*');
-- head_of_accounts: full (ops/reconciliation)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts','wallet.withdrawals', '*');
-- compliance_officer: read-only (audit)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer','wallet.withdrawals', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer','wallet.withdrawals', 'list');
-- csa (customer service agent): read-only assistance
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',             'wallet.withdrawals', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',             'wallet.withdrawals', 'list');
-- customer: self-service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.withdrawals', 'create');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.withdrawals', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.withdrawals', 'list');

-- ─── wallet.beneficiaries ───────────────────────────────────────────────────
-- admin: full
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',           'wallet.beneficiaries', '*');
-- head_of_accounts: full
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts','wallet.beneficiaries', '*');
-- compliance_officer: read-only
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer','wallet.beneficiaries', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer','wallet.beneficiaries', 'list');
-- csa: read-only assistance
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',             'wallet.beneficiaries', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',             'wallet.beneficiaries', 'list');
-- customer: full self-service CRUD on own beneficiaries
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.beneficiaries', 'create');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.beneficiaries', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.beneficiaries', 'list');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.beneficiaries', 'delete');
