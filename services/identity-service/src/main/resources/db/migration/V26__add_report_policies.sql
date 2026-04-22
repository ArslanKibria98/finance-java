-- V26: Casbin policies for ledger-service report endpoints
-- Covers objects used by @SecuredEndpoint in FineractReportsController:
--   Existing GL reports:
--     - reports.trial-balance
--     - reports.portfolio
--     - reports.dpd-buckets
--     - reports.collections
--     - reports.profit-revenue
--     - reports.write-off-provisions
--     - reports.cash-flow
--     - reports.reconciliation
--   New loan-level reports (Phase 1):
--     - reports.loan-disbursement
--     - reports.overdue-loans
--     - reports.due-loans
--     - reports.repayment-schedule
--     - reports.loan-balance-outstanding
--     - reports.customer-statement
--
-- super_admin already has ('*', '*') wildcard from V3, so no entries needed for it.

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    -- ── admin: full access to all reports ────────────────────────────────
    ('p', 'admin', 'reports.trial-balance', '*'),
    ('p', 'admin', 'reports.portfolio', '*'),
    ('p', 'admin', 'reports.dpd-buckets', '*'),
    ('p', 'admin', 'reports.collections', '*'),
    ('p', 'admin', 'reports.profit-revenue', '*'),
    ('p', 'admin', 'reports.write-off-provisions', '*'),
    ('p', 'admin', 'reports.cash-flow', '*'),
    ('p', 'admin', 'reports.reconciliation', '*'),
    ('p', 'admin', 'reports.loan-disbursement', '*'),
    ('p', 'admin', 'reports.overdue-loans', '*'),
    ('p', 'admin', 'reports.due-loans', '*'),
    ('p', 'admin', 'reports.repayment-schedule', '*'),
    ('p', 'admin', 'reports.loan-balance-outstanding', '*'),
    ('p', 'admin', 'reports.customer-statement', '*'),

    -- ── head_of_accounts: full access (finance owns reporting) ──────────
    ('p', 'head_of_accounts', 'reports.trial-balance', '*'),
    ('p', 'head_of_accounts', 'reports.portfolio', '*'),
    ('p', 'head_of_accounts', 'reports.dpd-buckets', '*'),
    ('p', 'head_of_accounts', 'reports.collections', '*'),
    ('p', 'head_of_accounts', 'reports.profit-revenue', '*'),
    ('p', 'head_of_accounts', 'reports.write-off-provisions', '*'),
    ('p', 'head_of_accounts', 'reports.cash-flow', '*'),
    ('p', 'head_of_accounts', 'reports.reconciliation', '*'),
    ('p', 'head_of_accounts', 'reports.loan-disbursement', '*'),
    ('p', 'head_of_accounts', 'reports.overdue-loans', '*'),
    ('p', 'head_of_accounts', 'reports.due-loans', '*'),
    ('p', 'head_of_accounts', 'reports.repayment-schedule', '*'),
    ('p', 'head_of_accounts', 'reports.loan-balance-outstanding', '*'),
    ('p', 'head_of_accounts', 'reports.customer-statement', '*'),

    -- ── compliance_officer: read-only across all reports ────────────────
    ('p', 'compliance_officer', 'reports.trial-balance', 'read'),
    ('p', 'compliance_officer', 'reports.portfolio', 'read'),
    ('p', 'compliance_officer', 'reports.dpd-buckets', 'read'),
    ('p', 'compliance_officer', 'reports.collections', 'read'),
    ('p', 'compliance_officer', 'reports.profit-revenue', 'read'),
    ('p', 'compliance_officer', 'reports.write-off-provisions', 'read'),
    ('p', 'compliance_officer', 'reports.cash-flow', 'read'),
    ('p', 'compliance_officer', 'reports.reconciliation', 'read'),
    ('p', 'compliance_officer', 'reports.loan-disbursement', 'read'),
    ('p', 'compliance_officer', 'reports.overdue-loans', 'read'),
    ('p', 'compliance_officer', 'reports.due-loans', 'read'),
    ('p', 'compliance_officer', 'reports.repayment-schedule', 'read'),
    ('p', 'compliance_officer', 'reports.loan-balance-outstanding', 'read'),
    ('p', 'compliance_officer', 'reports.customer-statement', 'read'),

    -- ── underwriter: read-only on operational loan reports ──────────────
    ('p', 'underwriter', 'reports.portfolio', 'read'),
    ('p', 'underwriter', 'reports.dpd-buckets', 'read'),
    ('p', 'underwriter', 'reports.loan-disbursement', 'read'),
    ('p', 'underwriter', 'reports.overdue-loans', 'read'),
    ('p', 'underwriter', 'reports.due-loans', 'read'),
    ('p', 'underwriter', 'reports.repayment-schedule', 'read'),
    ('p', 'underwriter', 'reports.loan-balance-outstanding', 'read'),
    ('p', 'underwriter', 'reports.customer-statement', 'read'),

    -- ── csa: read-only on customer-facing reports ───────────────────────
    ('p', 'csa', 'reports.loan-disbursement', 'read'),
    ('p', 'csa', 'reports.overdue-loans', 'read'),
    ('p', 'csa', 'reports.due-loans', 'read'),
    ('p', 'csa', 'reports.repayment-schedule', 'read'),
    ('p', 'csa', 'reports.loan-balance-outstanding', 'read'),
    ('p', 'csa', 'reports.customer-statement', 'read'),

    -- ── product_admin: read-only on portfolio/product-level reports ─────
    ('p', 'product_admin', 'reports.portfolio', 'read'),
    ('p', 'product_admin', 'reports.dpd-buckets', 'read'),
    ('p', 'product_admin', 'reports.profit-revenue', 'read'),
    ('p', 'product_admin', 'reports.loan-disbursement', 'read'),
    ('p', 'product_admin', 'reports.loan-balance-outstanding', 'read')
ON CONFLICT DO NOTHING;
