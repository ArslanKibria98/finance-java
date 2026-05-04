-- Seed Provision for Bad Debts account (code 3100).
-- Used by auto-journalization when LoanOverdue events are consumed.

DO $$
DECLARE
    test_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    system_user_id UUID := '00000000-0000-0000-0000-000000000000';
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM accounts
        WHERE tenant_id = test_tenant_id AND account_code = '3100'
    ) THEN
        INSERT INTO accounts (id, tenant_id, account_code, account_name, account_type, status, is_reconcilable, created_by, created_at)
        VALUES (
            gen_random_uuid(),
            test_tenant_id,
            '3100',
            'Provision for Bad Debts',
            'EXPENSE'::account_type,
            'ACTIVE'::account_status,
            FALSE,
            system_user_id,
            NOW()
        );
        RAISE NOTICE '✓ Seeded Provision for Bad Debts (3100) for tenant %', test_tenant_id;
    ELSE
        RAISE NOTICE 'Provision for Bad Debts (3100) already exists for tenant %', test_tenant_id;
    END IF;
END $$;
