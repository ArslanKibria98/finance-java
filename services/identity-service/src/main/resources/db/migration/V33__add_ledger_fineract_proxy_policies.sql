-- ============================================================================
-- V33: Casbin policies for ledger-service Fineract proxy endpoints.
-- All Fineract operations route through ledger-service. Wallet, lending,
-- product, and customer services call /api/v1/fineract-proxy/savings/**
-- (or /loans/**, etc) instead of calling Fineract directly.
-- ============================================================================
-- super_admin already has wildcard (*,*) via V3 — no additions needed.
-- ============================================================================

-- ─── ledger.fineract-proxy.savings ───────────────────────────────────────
-- admin: full access
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',          'ledger.fineract-proxy.savings', '*');

-- head_of_accounts: full access for reconciliation/operations
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts','ledger.fineract-proxy.savings', '*');

-- compliance_officer: read-only
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer','ledger.fineract-proxy.savings', 'read');

-- csa (customer service agent): create/read for top-up & balance enquiry
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',            'ledger.fineract-proxy.savings', 'create');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',            'ledger.fineract-proxy.savings', 'read');

-- customer: read-only on own account (additional row-level scoping enforced in service layer)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',       'ledger.fineract-proxy.savings', 'read');

-- ─── ledger.fineract-proxy (parent — for future loans/products/clients controllers) ─
-- admin & head_of_accounts: full
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',          'ledger.fineract-proxy', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts','ledger.fineract-proxy', '*');
