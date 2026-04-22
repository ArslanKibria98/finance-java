-- V31__add_finance_suggestions_policy.sql
-- Casbin policy for financial-first product suggestion endpoint
-- POST /api/v1/finance/suggest-products (lending-service)
-- Allows customer to receive product suggestions based on salary/liabilities
-- without pre-selecting a product.

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'customer', 'finance.suggestions', 'read')
ON CONFLICT DO NOTHING;
