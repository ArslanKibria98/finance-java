-- ============================================================================
-- Scotia RTP Settlement Clearing GL account
-- Used by wallet-service external fund transfers (Scotia RTP):
--   OUTBOUND: Dr Consumer Wallet (110401)            / Cr Scotia RTP Clearing (120601)
--   INBOUND : Dr Scotia RTP Clearing (120601)        / Cr Consumer Wallet (110401)
-- ============================================================================
DO $$
DECLARE
    test_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    system_user_id UUID := '00000000-0000-0000-0000-000000000000';
BEGIN
    INSERT INTO accounts (tenant_id, account_code, account_name, account_type, status, is_reconcilable, created_by, created_at)
    VALUES
        (test_tenant_id, '120601', 'Scotia RTP Settlement Clearing', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW())
    ON CONFLICT (tenant_id, account_code) DO NOTHING;
END $$;
