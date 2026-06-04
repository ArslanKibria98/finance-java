-- ============================================================================
-- V42: Casbin policies — wallet transaction-limit feature
--
-- Customer (own wallet):
--   GET  /api/v1/wallets/{id}/limits           -> wallets.limits:read
--   POST /api/v1/wallets/{id}/limit-requests    -> wallets.limits:request
--   GET  /api/v1/wallets/{id}/limit-requests    -> wallets.limits:read
--
-- Admin (platform bounds + approvals) — note obj prefix is 'wallet.' (singular),
-- so the existing 'wallets:*' wildcard does NOT cover these (exact-match objects):
--   GET/PUT /api/v1/admin/wallets/limit-bounds              -> wallet.limit-bounds:read|manage
--   GET     /api/v1/admin/wallets/limit-requests            -> wallet.limit-requests:read
--   POST    /api/v1/admin/wallets/limit-requests/{id}/approve|reject -> wallet.limit-requests:approve
-- super_admin already has *:* and needs no rows here.
-- ============================================================================

-- Customer: read own limits + raise change requests
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'wallets.limits', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'wallets.limits', 'request');

-- CSA / staff visibility into limits (read-only)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',              'wallets.limits',       'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',              'wallet.limit-requests', 'read');

-- Admin: configure platform bounds
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',            'wallet.limit-bounds',  'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',            'wallet.limit-bounds',  'manage');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'wallet.limit-bounds',  'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'wallet.limit-bounds',  'manage');

-- Admin: review + decide limit-change requests
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',            'wallet.limit-requests', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',            'wallet.limit-requests', 'approve');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'wallet.limit-requests', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'wallet.limit-requests', 'approve');
