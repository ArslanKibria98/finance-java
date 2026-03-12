-- V8__add_customer_lending_policies.sql
-- Add Casbin policies for customer role: lending flow + product browsing

-- Customer can browse products (used in lending Step 1 product selection)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'products', 'read');

-- Customer can manage their own loan applications
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loan-applications', 'create');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loan-applications', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loan-applications', 'manage');
