-- customer: Read own customer profile (used by lending-service to resolve customer ID by NID)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'customers', 'read');
