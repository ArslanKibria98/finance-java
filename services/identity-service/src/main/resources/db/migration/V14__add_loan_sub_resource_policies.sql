-- ============================================================================
-- V14: Add Casbin policies for loan sub-resource endpoints
-- loans.overview, loans.installments, loans.contract, loans.receipts
-- ============================================================================

-- customer: View own finance overview, installments, contract, receipts (BRD UC#04)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loans.overview', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loans.installments', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loans.contract', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loans.receipts', 'read');

-- admin: Full access to loan sub-resources
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'loans.overview', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'loans.installments', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'loans.contract', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'loans.receipts', 'read');

-- csa: View overview and installments for customer support
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'loans.overview', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'loans.installments', 'read');

-- underwriter: View overview and installments for assigned cases
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'loans.overview', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'loans.installments', 'read');

-- head_of_accounts: View overview and installments for disbursement approval
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loans.overview', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loans.installments', 'read');

-- compliance_officer: View overview for compliance audit
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'loans.overview', 'read');
