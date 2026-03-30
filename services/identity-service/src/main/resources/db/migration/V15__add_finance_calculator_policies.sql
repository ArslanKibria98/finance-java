-- ============================================================================
-- V15: Add Casbin policies for finance calculator and eligibility check
-- ============================================================================

-- customer: Use finance calculator and check eligibility (BRD UC#01, UC#02)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'finance.calculator', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'finance.eligibility', 'check');

-- admin: Full access
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'finance.calculator', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'finance.eligibility', 'check');

-- csa: Use calculator for customer support
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'finance.calculator', 'read');

-- underwriter: Use calculator for case evaluation
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'finance.calculator', 'read');
