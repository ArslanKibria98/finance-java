-- V37: Casbin policies for middleware cost-tracking endpoints (admin only).
--      Adds:
--        admin → middleware.cost-reports : read
--      'admin → middleware.provider-apis : *' is already present from V9;
--      super_admin is covered by the global '*:*' wildcard from V3.
--
--      casbin_rule has no unique constraint on (ptype,v0,v1,v2), so we use
--      "INSERT ... WHERE NOT EXISTS" to stay idempotent on re-runs.

INSERT INTO casbin_rule (ptype, v0, v1, v2)
SELECT 'p', 'admin', 'middleware.cost-reports', 'read'
WHERE NOT EXISTS (
    SELECT 1 FROM casbin_rule
    WHERE ptype = 'p' AND v0 = 'admin' AND v1 = 'middleware.cost-reports' AND v2 = 'read'
);
