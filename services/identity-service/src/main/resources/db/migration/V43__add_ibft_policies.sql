-- ============================================================================
-- V43: Casbin policies for IBFT (Scotia EFT) — beneficiaries + transfers.
-- Objects: ibft.beneficiaries, ibft.transfers. Idempotent. super_admin has ('*','*') via V3.
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2)
SELECT v.ptype, v.v0, v.v1, v.v2
FROM (VALUES
    -- beneficiaries
    ('p', 'customer', 'ibft.beneficiaries', 'create'),
    ('p', 'customer', 'ibft.beneficiaries', 'read'),
    ('p', 'customer', 'ibft.beneficiaries', 'list'),
    ('p', 'customer', 'ibft.beneficiaries', 'delete'),
    ('p', 'admin',    'ibft.beneficiaries', '*'),
    ('p', 'csa',      'ibft.beneficiaries', 'read'),
    ('p', 'csa',      'ibft.beneficiaries', 'list'),
    ('p', 'compliance_officer', 'ibft.beneficiaries', 'read'),
    ('p', 'compliance_officer', 'ibft.beneficiaries', 'list'),
    -- transfers
    ('p', 'customer', 'ibft.transfers', 'create'),
    ('p', 'customer', 'ibft.transfers', 'read'),
    ('p', 'customer', 'ibft.transfers', 'list'),
    ('p', 'admin',    'ibft.transfers', '*'),
    ('p', 'head_of_accounts', 'ibft.transfers', '*'),
    ('p', 'csa',      'ibft.transfers', 'read'),
    ('p', 'csa',      'ibft.transfers', 'list'),
    ('p', 'compliance_officer', 'ibft.transfers', 'read'),
    ('p', 'compliance_officer', 'ibft.transfers', 'list')
) AS v(ptype, v0, v1, v2)
WHERE NOT EXISTS (
    SELECT 1 FROM casbin_rule c
    WHERE c.ptype = v.ptype AND c.v0 = v.v0 AND c.v1 = v.v1 AND c.v2 = v.v2
);
