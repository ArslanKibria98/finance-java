-- ============================================================================
-- V7: Casbin policies for Employee management
-- ============================================================================

-- admin: Full employee management
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'employees', '*');

-- csa (customer service agent): Read-only employee list
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'employees', 'read');
