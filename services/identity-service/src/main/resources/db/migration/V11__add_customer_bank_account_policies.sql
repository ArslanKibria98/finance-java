-- ============================================================================
-- V10: Add customer role permissions for bank accounts
-- Customer needs to read and create their own bank accounts
-- ============================================================================

-- customer: Read own bank accounts (used by lending-service bank account lookup)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'customers.bank-accounts', 'read');

-- customer: Add bank accounts during loan application flow
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'customers.bank-accounts', 'create');
