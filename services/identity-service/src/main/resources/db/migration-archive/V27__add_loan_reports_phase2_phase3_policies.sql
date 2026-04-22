-- V27: Casbin policies for Phase 2 & Phase 3 loan reports in ledger-service
-- Objects:
--   Phase 2: reports.product-wise-pnl, reports.customer-wise-pnl,
--            reports.loan-history, reports.daily-transaction-summary
--   Phase 3: reports.npl, reports.early-settlement, reports.write-off-loans,
--            reports.collections-due, reports.account-report, reports.simah
--
-- super_admin already has ('*', '*') wildcard from V3.

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    -- ── admin: full access ───────────────────────────────────────────────
    ('p', 'admin', 'reports.product-wise-pnl',           '*'),
    ('p', 'admin', 'reports.customer-wise-pnl',          '*'),
    ('p', 'admin', 'reports.loan-history',               '*'),
    ('p', 'admin', 'reports.daily-transaction-summary',  '*'),
    ('p', 'admin', 'reports.npl',                        '*'),
    ('p', 'admin', 'reports.early-settlement',           '*'),
    ('p', 'admin', 'reports.write-off-loans',            '*'),
    ('p', 'admin', 'reports.collections-due',            '*'),
    ('p', 'admin', 'reports.account-report',             '*'),
    ('p', 'admin', 'reports.simah',                      '*'),

    -- ── head_of_accounts: full access ───────────────────────────────────
    ('p', 'head_of_accounts', 'reports.product-wise-pnl',          '*'),
    ('p', 'head_of_accounts', 'reports.customer-wise-pnl',         '*'),
    ('p', 'head_of_accounts', 'reports.loan-history',              '*'),
    ('p', 'head_of_accounts', 'reports.daily-transaction-summary', '*'),
    ('p', 'head_of_accounts', 'reports.npl',                       '*'),
    ('p', 'head_of_accounts', 'reports.early-settlement',          '*'),
    ('p', 'head_of_accounts', 'reports.write-off-loans',           '*'),
    ('p', 'head_of_accounts', 'reports.collections-due',           '*'),
    ('p', 'head_of_accounts', 'reports.account-report',            '*'),
    ('p', 'head_of_accounts', 'reports.simah',                     '*'),

    -- ── compliance_officer: read-only ───────────────────────────────────
    ('p', 'compliance_officer', 'reports.product-wise-pnl',          'read'),
    ('p', 'compliance_officer', 'reports.customer-wise-pnl',         'read'),
    ('p', 'compliance_officer', 'reports.loan-history',              'read'),
    ('p', 'compliance_officer', 'reports.daily-transaction-summary', 'read'),
    ('p', 'compliance_officer', 'reports.npl',                       'read'),
    ('p', 'compliance_officer', 'reports.early-settlement',          'read'),
    ('p', 'compliance_officer', 'reports.write-off-loans',           'read'),
    ('p', 'compliance_officer', 'reports.collections-due',           'read'),
    ('p', 'compliance_officer', 'reports.account-report',            'read'),
    ('p', 'compliance_officer', 'reports.simah',                     'read'),

    -- ── underwriter: read-only on loan-facing reports ────────────────────
    ('p', 'underwriter', 'reports.loan-history',              'read'),
    ('p', 'underwriter', 'reports.npl',                       'read'),
    ('p', 'underwriter', 'reports.early-settlement',          'read'),
    ('p', 'underwriter', 'reports.account-report',            'read'),
    ('p', 'underwriter', 'reports.simah',                     'read'),

    -- ── csa: read-only on customer-facing & collections reports ─────────
    ('p', 'csa', 'reports.loan-history',              'read'),
    ('p', 'csa', 'reports.daily-transaction-summary', 'read'),
    ('p', 'csa', 'reports.early-settlement',          'read'),
    ('p', 'csa', 'reports.collections-due',           'read'),
    ('p', 'csa', 'reports.account-report',            'read'),

    -- ── product_admin: read-only on product/portfolio analytics ─────────
    ('p', 'product_admin', 'reports.product-wise-pnl',          'read'),
    ('p', 'product_admin', 'reports.customer-wise-pnl',         'read'),
    ('p', 'product_admin', 'reports.daily-transaction-summary', 'read'),
    ('p', 'product_admin', 'reports.write-off-loans',           'read')
ON CONFLICT DO NOTHING;
