-- ============================================================================
-- V3: Casbin Object-Action Policies (replaces path-based rules)
-- KSA Islamic Financing Platform - Identity Service
-- ============================================================================
-- Policy format: (ptype='p', v0=role, v1=object, v2=action)
-- Each @SecuredEndpoint(obj=..., act=...) maps to a policy here.
-- Wildcard: act='*' means all actions on that object.
-- ============================================================================

-- Remove old path-based rules (V2 style)
DELETE FROM casbin_rule WHERE ptype = 'p';

-- ============================================================================
-- super_admin: Full access to everything
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'super_admin', '*', '*');

-- ============================================================================
-- admin: Broad management access across all services
-- ============================================================================
-- Customer Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'customers', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'customers.bank-accounts', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'customers.employment', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'customers.kyc-status', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'reference-data', '*');

-- Global Profile Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'profiles', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'profiles.regional', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'profiles.access-tokens', '*');

-- PII Vault Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'pii', '*');

-- KYC Adapter Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'kyc.tahakuk', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'kyc.nafath', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'kyc.yakeen', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'kyc.screening', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'kyc.gosi', '*');

-- Wallet Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'wallets', '*');

-- Risk Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'risk', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'risk.blacklist', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'risk.credit-scoring', '*');

-- Product Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'products', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'product-categories', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'product-settings', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'product-partners', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'product-documents', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'partners', '*');

-- Identity Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'roles', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'roles', 'create');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'permissions', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'policies', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'policies', 'authorize');

-- Onboarding Service
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin', 'onboarding', '*');

-- ============================================================================
-- csa (Customer Service Agent): Customer operations + KYC
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'customers', 'create');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'customers', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'customers', 'update');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'customers.bank-accounts', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'customers.employment', 'create');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'kyc.tahakuk', 'verify');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'kyc.nafath', 'initiate');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'kyc.nafath', 'status');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'kyc.yakeen', 'verify');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'kyc.screening', 'check');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'kyc.gosi', 'fetch');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'profiles', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'wallets', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'risk.blacklist', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'products', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa', 'product-categories', 'read');

-- ============================================================================
-- compliance_officer: Compliance & audit operations
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'customers', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'customers.kyc-status', 'update');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'pii', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'risk.blacklist', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'compliance_officer', 'profiles', 'read');

-- ============================================================================
-- underwriter: Read-only access to customer & product data
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'customers', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'products', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'underwriter', 'product-categories', 'read');

-- ============================================================================
-- product_admin: Product catalog management
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'products', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'product-categories', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'product-settings', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'product-partners', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'product-documents', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'partners', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'product_admin', 'risk.credit-scoring', 'read');

-- ============================================================================
-- partner_admin: Partner management + read-only products
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', 'partners', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'partner_admin', 'products', 'read');

-- ============================================================================
-- head_of_accounts: Wallet & financial operations
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts', 'wallets', '*');

-- ============================================================================
-- customer: Self-service limited access
-- ============================================================================
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'wallets.me', '*');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'profiles.me', 'read');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'profiles.me', 'update');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer', 'onboarding', 'status');
