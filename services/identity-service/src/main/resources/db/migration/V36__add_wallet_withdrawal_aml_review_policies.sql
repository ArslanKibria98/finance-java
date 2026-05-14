-- ============================================================================
-- V36: Casbin policies for wallet.withdrawals.aml-review object.
-- Used by POST /api/v1/admin/wallets/withdrawals/{id}/release
-- (compliance officer manual approval of HELD_AML withdrawals)
-- ============================================================================
-- super_admin already has ('*','*') wildcard via V3 — no additions needed.
-- ============================================================================

-- compliance_officer: primary role (approve / read / list)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'wallet.withdrawals.aml-review', 'approve');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'wallet.withdrawals.aml-review', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'wallet.withdrawals.aml-review', 'list');

-- admin: can override
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'wallet.withdrawals.aml-review', '*');

-- head_of_accounts: visibility for ops/reconciliation
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'wallet.withdrawals.aml-review', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'wallet.withdrawals.aml-review', 'list');
