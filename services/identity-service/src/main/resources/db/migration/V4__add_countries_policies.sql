-- ============================================================================
-- V4: Add countries object policies for Product Service CountryController
-- ============================================================================

-- super_admin already has wildcard (*,*) — no change needed

-- admin: Full access to countries
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'countries', '*');

-- product_admin: Full access to countries (manages product catalog including country availability)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'countries', '*');

-- csa: Read-only access to countries
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'countries', 'read');

-- underwriter: Read-only access to countries
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'countries', 'read');

-- partner_admin: Read-only access to countries
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', 'countries', 'read');
