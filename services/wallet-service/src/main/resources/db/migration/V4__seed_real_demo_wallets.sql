-- ============================================================================
-- V4: Top up existing real-user wallets for demo + freeze one for negative test
--
-- Strategy: Real Keycloak users (CompanyRealm, PIN 147852) already have
-- onboarding-created wallets in tenant 00000000-0000-0000-0000-000000000001.
-- Top them up so wallet-to-wallet transfer demo works end-to-end.
--
-- Demo roles:
--   - Wallet A: NID 1010219431  customerId 62d36093-c494-4133-8a11-79fecad3b465
--               (existing wallet) → 10,000 SAR ACTIVE
--   - Wallet B: NID 1211111111  customerId c0a61112-3d36-49ab-9256-16f26eb69b35
--               (existing wallet) →  5,000 SAR ACTIVE
--   - Wallet C: NID 1234567010  customerId b01a5560-f62e-49c5-8f74-b916b28b7762
--               (existing wallet) →      0 SAR FROZEN
--
-- The old V3 demo wallets (WLT-DEMO-A/B/C in tenant 11111111-...) are removed
-- since they used a non-existent tenant and can't be reached via real JWTs.
-- ============================================================================

-- 1. Remove unreachable V3 demo wallets (different tenant, no Keycloak owner)
DELETE FROM wallet_movements
 WHERE wallet_id IN (
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     'cccccccc-cccc-cccc-cccc-cccccccccccc');
DELETE FROM wallet_status_history
 WHERE wallet_id IN (
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     'cccccccc-cccc-cccc-cccc-cccccccccccc');
DELETE FROM wallets
 WHERE id IN (
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     'cccccccc-cccc-cccc-cccc-cccccccccccc');

-- 2. Top up existing real-user wallets (assign demo balances)
UPDATE wallets
SET available_balance = 10000.000000,
    status            = 'ACTIVE',
    single_transfer_limit  = 5000,
    daily_transfer_limit   = 25000,
    monthly_transfer_limit = 100000
WHERE tenant_id   = '00000000-0000-0000-0000-000000000001'
  AND customer_id = '62d36093-c494-4133-8a11-79fecad3b465';

UPDATE wallets
SET available_balance = 5000.000000,
    status            = 'ACTIVE',
    single_transfer_limit  = 5000,
    daily_transfer_limit   = 25000,
    monthly_transfer_limit = 100000
WHERE tenant_id   = '00000000-0000-0000-0000-000000000001'
  AND customer_id = 'c0a61112-3d36-49ab-9256-16f26eb69b35';

UPDATE wallets
SET available_balance = 0,
    status            = 'FROZEN',
    single_transfer_limit  = 5000,
    daily_transfer_limit   = 25000,
    monthly_transfer_limit = 100000
WHERE tenant_id   = '00000000-0000-0000-0000-000000000001'
  AND customer_id = 'b01a5560-f62e-49c5-8f74-b916b28b7762';
