-- V32__add_early_settlement_eligibility_policy.sql
-- Grants read access on the customer-facing early-settlement eligibility endpoint
-- (collections-service GET /api/v1/admin/delinquency-rules/eligibility), consumed by
-- lending-service to enrich GET /api/v1/loans/customer/{id} responses with the
-- earlySettlementEligible flag on behalf of the authenticated customer.
--
-- Object: loans.early-settlement (separate from admin-only delinquency.rules so
--         customers only see the per-loan boolean, not the rule definitions)
-- Action: read

-- super_admin / admin: wildcard for audit consistency
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'super_admin', 'loans.early-settlement', '*')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'admin', 'loans.early-settlement', '*')
ON CONFLICT DO NOTHING;

-- customer: the primary caller (via the lending-service loans list, which forwards the JWT)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'customer', 'loans.early-settlement', 'read')
ON CONFLICT DO NOTHING;

-- Collection/product staff who may assist customers: read-only
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_head', 'loans.early-settlement', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'collections_agent', 'loans.early-settlement', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'product_admin', 'loans.early-settlement', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'compliance_officer', 'loans.early-settlement', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'underwriter', 'loans.early-settlement', 'read')
ON CONFLICT DO NOTHING;
