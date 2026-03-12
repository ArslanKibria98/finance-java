-- ============================================================================
-- V9: Add missing Casbin policies for new services and endpoints
-- KSA Islamic Financing Platform - Identity Service
-- ============================================================================
-- Reference: Blueprint 11_USER_ROLES_PERMISSIONS.md
-- super_admin already has wildcard (*,*) via V3 — no additions needed.
-- ============================================================================

-- ============================================================================
-- 1. FRAUD SERVICE (fraud.rules)
-- Blueprint: Fraud/AML falls under compliance + admin scope
-- ============================================================================
-- admin: Full fraud rule management
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'fraud.rules', '*');

-- compliance_officer: Read-only (view fraud rules for compliance review)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'fraud.rules', 'read');

-- ============================================================================
-- 2. MIDDLEWARE THIRD-PARTY (middleware.*)
-- Blueprint: API credential management is system configuration
-- Section 2.1 Super Admin: "manage_api_credentials (Nafath, Simah, Sadad)"
-- Section 6 Maker-Checker: "System Config Change: Maker=Admin, Checker=Super Admin"
-- ============================================================================
-- admin: Full middleware management
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'middleware.providers', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'middleware.provider-apis', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'middleware.clients', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'middleware.client-access', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'middleware.env-configs', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'middleware.callbacks', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'middleware.logs', '*');

-- ============================================================================
-- 3. LENDING SERVICE (loans)
-- Blueprint Section 2.6 Underwriter: "view_assigned, approve (<=100K)"
-- Blueprint Section 2.7 CSA: "view_loan_status, view_repayment_schedule" (read-only)
-- Blueprint Section 4.1 Customer: "view_own_loans, view_repayment_schedule"
-- Blueprint Section 2.5 Head of Accounts: "approve_disbursement, hold_disbursement"
-- Blueprint Section 2.4 Compliance Officer: compliance audit visibility
-- ============================================================================
-- admin: Full loan access
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'loans', '*');

-- underwriter: Read loan details (assigned cases)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'loans', 'read');

-- csa: Read-only loan status viewing
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'loans', 'read');

-- customer: View own loans
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'loans', 'read');

-- head_of_accounts: View loans for disbursement approval
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'loans', 'read');

-- compliance_officer: View loans for compliance audit
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'loans', 'read');

-- ============================================================================
-- 4. RISK SERVICE - Credit Scoring Field Definitions
-- Blueprint: Risk/scoring configuration is admin scope
-- product_admin already has risk.credit-scoring|read (V3) — extend to sub-resource
-- ============================================================================
-- admin: Full field definition management (already has risk|* but explicit is clearer)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'risk.credit-scoring.field-definitions', '*');

-- product_admin: Read-only (needs to see scoring config for product setup)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'risk.credit-scoring.field-definitions', 'read');

-- ============================================================================
-- 5. IDENTITY SERVICE - Missing policy/permission/role CRUD
-- Blueprint Section 6: "System Config Change: Maker=Admin, Checker=Super Admin"
-- Admin manages policies, permissions, roles within own tenant
-- ============================================================================
-- admin: Full CRUD on permissions
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'permissions', '*');

-- admin: Full CRUD on policies
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'policies', '*');

-- admin: Full CRUD on roles (currently only has create + read)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'roles', '*');
