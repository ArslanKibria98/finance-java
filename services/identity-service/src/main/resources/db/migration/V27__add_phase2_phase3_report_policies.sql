-- V27: Casbin policies for Phase 2/3 ledger report endpoints
-- Covers objects used by @SecuredEndpoint in FineractReportsController:
--   - reports.product-wise-pnl
--   - reports.customer-wise-pnl
--   - reports.loan-history
--   - reports.daily-transaction-summary
--   - reports.npl
--   - reports.early-settlement
--   - reports.write-off-loans
--   - reports.collections-due
--   - reports.account
--   - reports.simah

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    -- admin
    ('p', 'admin', 'reports.product-wise-pnl', '*'),
    ('p', 'admin', 'reports.customer-wise-pnl', '*'),
    ('p', 'admin', 'reports.loan-history', '*'),
    ('p', 'admin', 'reports.daily-transaction-summary', '*'),
    ('p', 'admin', 'reports.npl', '*'),
    ('p', 'admin', 'reports.early-settlement', '*'),
    ('p', 'admin', 'reports.write-off-loans', '*'),
    ('p', 'admin', 'reports.collections-due', '*'),
    ('p', 'admin', 'reports.account', '*'),
    ('p', 'admin', 'reports.simah', '*'),

    -- head_of_accounts
    ('p', 'head_of_accounts', 'reports.product-wise-pnl', '*'),
    ('p', 'head_of_accounts', 'reports.customer-wise-pnl', '*'),
    ('p', 'head_of_accounts', 'reports.loan-history', '*'),
    ('p', 'head_of_accounts', 'reports.daily-transaction-summary', '*'),
    ('p', 'head_of_accounts', 'reports.npl', '*'),
    ('p', 'head_of_accounts', 'reports.early-settlement', '*'),
    ('p', 'head_of_accounts', 'reports.write-off-loans', '*'),
    ('p', 'head_of_accounts', 'reports.collections-due', '*'),
    ('p', 'head_of_accounts', 'reports.account', '*'),
    ('p', 'head_of_accounts', 'reports.simah', '*'),

    -- compliance_officer
    ('p', 'compliance_officer', 'reports.product-wise-pnl', 'read'),
    ('p', 'compliance_officer', 'reports.customer-wise-pnl', 'read'),
    ('p', 'compliance_officer', 'reports.loan-history', 'read'),
    ('p', 'compliance_officer', 'reports.daily-transaction-summary', 'read'),
    ('p', 'compliance_officer', 'reports.npl', 'read'),
    ('p', 'compliance_officer', 'reports.early-settlement', 'read'),
    ('p', 'compliance_officer', 'reports.write-off-loans', 'read'),
    ('p', 'compliance_officer', 'reports.collections-due', 'read'),
    ('p', 'compliance_officer', 'reports.account', 'read'),
    ('p', 'compliance_officer', 'reports.simah', 'read'),

    -- underwriter
    ('p', 'underwriter', 'reports.product-wise-pnl', 'read'),
    ('p', 'underwriter', 'reports.loan-history', 'read'),
    ('p', 'underwriter', 'reports.npl', 'read'),
    ('p', 'underwriter', 'reports.early-settlement', 'read'),
    ('p', 'underwriter', 'reports.collections-due', 'read'),
    ('p', 'underwriter', 'reports.simah', 'read'),

    -- csa
    ('p', 'csa', 'reports.loan-history', 'read'),
    ('p', 'csa', 'reports.collections-due', 'read'),
    ('p', 'csa', 'reports.early-settlement', 'read'),

    -- product_admin
    ('p', 'product_admin', 'reports.product-wise-pnl', 'read'),
    ('p', 'product_admin', 'reports.daily-transaction-summary', 'read'),
    ('p', 'product_admin', 'reports.npl', 'read'),
    ('p', 'product_admin', 'reports.account', 'read')
ON CONFLICT DO NOTHING;
