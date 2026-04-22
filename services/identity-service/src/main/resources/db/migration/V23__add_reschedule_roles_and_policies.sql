-- V23__add_reschedule_roles_and_policies.sql
-- Adds ops_head, underwriter, credit_committee roles + loan.reschedules Casbin policies
-- Per Blueprint 17: Maker-Checker for Loan Rescheduling
--
-- Approval matrix (SECURITY_ARCHITECTURE.md §Maker-Checker):
--   SKIP_PAYMENT      → customer self-service (no approval)
--   TENURE_EXTENSION  → underwriter (maker) → ops_head (checker/approver)
--   PAYMENT_HOLIDAY   → underwriter (maker) → ops_head (checker/approver)
--   RESTRUCTURING     → underwriter (maker) → credit_committee (checker/approver)

-- ══════════════════════════════════════════════════════════════
-- 1. INSERT MISSING ROLES
-- ══════════════════════════════════════════════════════════════

INSERT INTO roles (tenant_id, role_code, role_name, description, is_system)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ops_head',          'Operations Head',    'Approves TENURE_EXTENSION and PAYMENT_HOLIDAY reschedule requests', true),
    ('00000000-0000-0000-0000-000000000001', 'underwriter',       'Underwriter',         'Initiates reschedule requests as maker role', true),
    ('00000000-0000-0000-0000-000000000001', 'credit_committee',  'Credit Committee',    'Approves RESTRUCTURING reschedules with GL write-off', true),
    ('00000000-0000-0000-0000-000000000001', 'compliance_officer','Compliance Officer',  'Read-only audit access across all modules', true)
ON CONFLICT DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- 2. CASBIN POLICIES — loan.reschedules object
-- ══════════════════════════════════════════════════════════════

-- customer: create own reschedule request (SKIP_PAYMENT self-service)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'customer', 'loan.reschedules', 'create')
ON CONFLICT DO NOTHING;

-- customer: read own reschedule status
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'customer', 'loan.reschedules', 'read')
ON CONFLICT DO NOTHING;

-- underwriter (maker): create + read reschedule requests on behalf of ops
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'underwriter', 'loan.reschedules', 'create')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'underwriter', 'loan.reschedules', 'read')
ON CONFLICT DO NOTHING;

-- ops_head (checker): approve / reject TENURE_EXTENSION and PAYMENT_HOLIDAY
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'ops_head', 'loan.reschedules', 'create')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'ops_head', 'loan.reschedules', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'ops_head', 'loan.reschedules', 'update')   -- approve / reject signal
ON CONFLICT DO NOTHING;

-- credit_committee (checker): approve / reject RESTRUCTURING + GL write-off
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'credit_committee', 'loan.reschedules', 'create')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'credit_committee', 'loan.reschedules', 'read')
ON CONFLICT DO NOTHING;

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'credit_committee', 'loan.reschedules', 'update')   -- approve / reject signal
ON CONFLICT DO NOTHING;

-- admin: full access
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'admin', 'loan.reschedules', '*')
ON CONFLICT DO NOTHING;

-- super_admin: full access
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'super_admin', 'loan.reschedules', '*')
ON CONFLICT DO NOTHING;

-- compliance_officer: read-only (audit)
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'compliance_officer', 'loan.reschedules', 'read')
ON CONFLICT DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- 3. EXISTING ROLES — give read access for monitoring
-- ══════════════════════════════════════════════════════════════

-- csa (Customer Service Agent): read-only to see status for customers
INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES ('p', 'csa', 'loan.reschedules', 'read')
ON CONFLICT DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- 4. SUMMARY
-- ══════════════════════════════════════════════════════════════
-- Roles added:        ops_head, underwriter, credit_committee, compliance_officer
-- Casbin policies:    loan.reschedules → create/read/update per role
-- Approval matrix:
--   SKIP_PAYMENT      → customer self-service (no approval needed)
--   TENURE_EXTENSION  → ops_head can update (approve/reject)
--   PAYMENT_HOLIDAY   → ops_head can update (approve/reject)
--   RESTRUCTURING     → credit_committee can update (approve/reject)
