-- ============================================================================
-- V41: Casbin policies for external fund transfers (Scotia RTP)
-- Object: wallet.transfers (reused by the new external transfer endpoints:
--   POST   /api/v1/wallets/transfers/external
--   GET    /api/v1/wallets/transfers/external/{id}
--   GET    /api/v1/wallets/transfers/external/by-wallet/{walletId}
-- )
-- These rows also backfill the wallet.transfers policies that were previously
-- only seeded manually in DEV (Flyway drift). Idempotent — safe to re-run.
-- super_admin already has ('*','*') via V3 — no addition needed.
-- ============================================================================

INSERT INTO casbin_rule (ptype, v0, v1, v2)
SELECT v.ptype, v.v0, v.v1, v.v2
FROM (VALUES
    ('p', 'customer', 'wallet.transfers', 'create'),
    ('p', 'customer', 'wallet.transfers', 'read'),
    ('p', 'customer', 'wallet.transfers', 'list'),
    ('p', 'customer', 'wallet.transfers', 'lookup'),
    ('p', 'admin',    'wallet.transfers', '*'),
    ('p', 'csa',      'wallet.transfers', 'read'),
    ('p', 'csa',      'wallet.transfers', 'list'),
    ('p', 'head_of_accounts', 'wallet.transfers', '*')
) AS v(ptype, v0, v1, v2)
WHERE NOT EXISTS (
    SELECT 1 FROM casbin_rule c
    WHERE c.ptype = v.ptype AND c.v0 = v.v0 AND c.v1 = v.v1 AND c.v2 = v.v2
);
