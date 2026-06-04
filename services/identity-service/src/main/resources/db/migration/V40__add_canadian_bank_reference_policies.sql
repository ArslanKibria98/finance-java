-- ============================================================================
-- V40: Casbin policies for Canadian bank reference-data (LOV) read access.
-- Maps @SecuredEndpoint(obj = "reference-data.canadian-bank", act = "read")
-- on ReferenceDataController (customer-service).
-- Object matching is exact-string, so the existing 'admin'/'reference-data'
-- wildcard does NOT cover the 'reference-data.canadian-bank' sub-resource.
-- ============================================================================

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    ('p', 'super_admin',   'reference-data.canadian-bank', '*'),
    ('p', 'admin',         'reference-data.canadian-bank', 'read'),
    ('p', 'csa',           'reference-data.canadian-bank', 'read'),
    ('p', 'customer',      'reference-data.canadian-bank', 'read')
ON CONFLICT DO NOTHING;
