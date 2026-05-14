-- ============================================================================
-- V35: Casbin policy — 'update' action on wallet.beneficiaries
-- Used by POST /api/v1/wallets/beneficiaries/{id}/activate
-- ============================================================================

INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'admin',           'wallet.beneficiaries', 'update');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'head_of_accounts','wallet.beneficiaries', 'update');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'csa',             'wallet.beneficiaries', 'update');
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES ('p', 'customer',        'wallet.beneficiaries', 'update');
