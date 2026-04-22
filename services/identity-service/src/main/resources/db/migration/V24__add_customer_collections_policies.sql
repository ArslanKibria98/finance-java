-- V24: Grant customer role access to collections-service APIs
-- Covers:
--   - Payments: initiate, fetch, and payment-flow updates
--   - Settlements: quote, create, confirm, fetch
--   - Repayment schedules: read-only access

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    ('p', 'customer', 'payments', 'create'),
    ('p', 'customer', 'payments', 'read'),
    ('p', 'customer', 'payments', 'update'),
    ('p', 'customer', 'settlements', 'create'),
    ('p', 'customer', 'settlements', 'read'),
    ('p', 'customer', 'settlements', 'update'),
    ('p', 'customer', 'repayment-schedules', 'read')
ON CONFLICT DO NOTHING;
