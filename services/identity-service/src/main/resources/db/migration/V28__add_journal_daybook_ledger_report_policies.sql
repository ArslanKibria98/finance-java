-- V28: Casbin policies for additional ledger report endpoints
-- Covers:
--   - reports.journal-vouchers
--   - reports.day-book
--   - reports.ledger

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    -- admin
    ('p', 'admin', 'reports.journal-vouchers', '*'),
    ('p', 'admin', 'reports.day-book', '*'),
    ('p', 'admin', 'reports.ledger', '*'),

    -- head_of_accounts
    ('p', 'head_of_accounts', 'reports.journal-vouchers', '*'),
    ('p', 'head_of_accounts', 'reports.day-book', '*'),
    ('p', 'head_of_accounts', 'reports.ledger', '*'),

    -- compliance_officer
    ('p', 'compliance_officer', 'reports.journal-vouchers', 'read'),
    ('p', 'compliance_officer', 'reports.day-book', 'read'),
    ('p', 'compliance_officer', 'reports.ledger', 'read'),

    -- underwriter
    ('p', 'underwriter', 'reports.journal-vouchers', 'read'),
    ('p', 'underwriter', 'reports.day-book', 'read'),
    ('p', 'underwriter', 'reports.ledger', 'read'),

    -- csa
    ('p', 'csa', 'reports.journal-vouchers', 'read'),
    ('p', 'csa', 'reports.day-book', 'read'),
    ('p', 'csa', 'reports.ledger', 'read'),

    -- product_admin
    ('p', 'product_admin', 'reports.journal-vouchers', 'read'),
    ('p', 'product_admin', 'reports.day-book', 'read'),
    ('p', 'product_admin', 'reports.ledger', 'read')
ON CONFLICT DO NOTHING;
