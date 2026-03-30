-- ============================================================================
-- V13: Add Casbin policies for loan-applications.tracker
-- Customer and admin roles need access to application tracker endpoint
-- ============================================================================

-- customer: View own application tracker (BRD UC#03)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loan-applications.tracker', 'read');

-- admin: View all application trackers
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'loan-applications.tracker', 'read');

-- csa: View application trackers (customer support)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'loan-applications.tracker', 'read');

-- underwriter: View application trackers (assigned cases)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'loan-applications.tracker', 'read');
