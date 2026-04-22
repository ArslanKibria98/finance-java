-- V25: Add Casbin policies for ledger COA endpoints
-- Covers objects used by @SecuredEndpoint in ledger-service:
--   - ledger.coa-fields
--   - ledger.coa-config

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    -- Full access for administrative roles
    ('p', 'super_admin', 'ledger.coa-fields', '*'),
    ('p', 'super_admin', 'ledger.coa-config', '*'),
    ('p', 'admin', 'ledger.coa-fields', '*'),
    ('p', 'admin', 'ledger.coa-config', '*'),
    ('p', 'head_of_accounts', 'ledger.coa-fields', '*'),
    ('p', 'head_of_accounts', 'ledger.coa-config', '*'),
    ('p', 'product_admin', 'ledger.coa-fields', '*'),
    ('p', 'product_admin', 'ledger.coa-config', '*'),

    -- Read access for operational/review roles
    ('p', 'csa', 'ledger.coa-fields', 'read'),
    ('p', 'csa', 'ledger.coa-config', 'read'),
    ('p', 'underwriter', 'ledger.coa-fields', 'read'),
    ('p', 'underwriter', 'ledger.coa-config', 'read'),
    ('p', 'compliance_officer', 'ledger.coa-fields', 'read'),
    ('p', 'compliance_officer', 'ledger.coa-config', 'read')
ON CONFLICT DO NOTHING;
