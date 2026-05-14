-- V39: Casbin policies for API audit trail read access.
-- Maps @SecuredEndpoint(obj = "audit.api", act = "read") if/when an internal
-- "audit viewer" endpoint is exposed (Kibana itself is gated separately at the
-- network layer / nginx + Keycloak).

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    ('p', 'super_admin', 'audit.api', '*'),
    ('p', 'admin', 'audit.api', 'read'),
    ('p', 'compliance_officer', 'audit.api', 'read')
ON CONFLICT DO NOTHING;
