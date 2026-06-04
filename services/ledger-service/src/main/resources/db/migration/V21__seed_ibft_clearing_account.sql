-- ============================================================================
-- IBFT (Scotia EFT) Settlement Clearing GL account. Used by wallet-service IBFT:
--   HOLD/SETTLE: Dr Consumer Wallet (110401)  / Cr IBFT Clearing (120602)
--   RELEASE:     Dr IBFT Clearing (120602)     / Cr Consumer Wallet (110401)
-- ============================================================================
DO $$
DECLARE
    test_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    system_user_id UUID := '00000000-0000-0000-0000-000000000000';
BEGIN
    INSERT INTO accounts (tenant_id, account_code, account_name, account_type, status, is_reconcilable, created_by, created_at)
    VALUES
        (test_tenant_id, '120602', 'IBFT Settlement Clearing', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW())
    ON CONFLICT (tenant_id, account_code) DO NOTHING;
END $$;
