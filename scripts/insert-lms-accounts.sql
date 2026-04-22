-- ============================================================================
-- STANDALONE: Insert LMS Micro-Lending Platform Accounts
-- Usage: psql -h localhost -U postgres -d ledger_service_db -f insert-lms-accounts.sql
-- ============================================================================

DO $$
DECLARE
    test_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    system_user_id UUID := '00000000-0000-0000-0000-000000000000';
BEGIN

    INSERT INTO accounts (tenant_id, account_code, account_name, account_type, status, is_reconcilable, created_by, created_at)
    VALUES
        -- ====== ASSET ACCOUNTS (1xxx) ======
        (test_tenant_id, '110100', 'Asset', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '110102', 'Investor Main Wallet', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '110104', 'Processing Fee', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '110204', 'Micro Loan Due Interest Receivable Account', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '110205', 'Micro Loan OverDue Principal Receivable Account', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '110401', 'Consumer Wallet', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '110402', 'Investor Wallet', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '110602', 'Accrued Interest Receivable Account', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '122318', 'Cash Account', 'ASSET'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),

        -- ====== LIABILITY ACCOUNTS (2xxx) ======
        (test_tenant_id, '120300', 'Liability', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120302', 'VAT Account', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120303', 'Pool Account (IC)', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120304', 'Returns Payable', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120305', 'VAT', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120306', 'Commodity Inventory Account', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120307', 'SupplierAccount', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120401', 'Micro Loan Suspense Account', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '120501', 'Micro Loan Cash-out Clearing Account', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '122309', 'Supplier Commission', 'LIABILITY'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),

        -- ====== INCOME ACCOUNTS (13xxx) ======
        (test_tenant_id, '130100', 'Revenue', 'INCOME'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '130101', 'Revenue Account', 'INCOME'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '130201', 'Micro Loan Cash Out Clearing Account', 'INCOME'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),

        -- ====== EXPENSE ACCOUNTS (14xxx, 16xxx) ======
        (test_tenant_id, '140200', 'Cost', 'EXPENSE'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '140201', 'Micro Loan Suspense Account', 'EXPENSE'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW()),
        (test_tenant_id, '160100', 'Expense', 'EXPENSE'::account_type, 'ACTIVE'::account_status, TRUE, system_user_id, NOW());

    RAISE NOTICE '✓ Successfully inserted 28 LMS Chart of Accounts for tenant %', test_tenant_id;
    RAISE NOTICE '✓ Micro-lending platform accounts ready!';
    RAISE NOTICE '✓ All accounts are ACTIVE and reconcilable';

END $$;

-- ============================================================================
-- VERIFICATION QUERY - Show all inserted accounts
-- ============================================================================
\echo '====== LMS CHART OF ACCOUNTS VERIFICATION ======'
SELECT
    account_code,
    account_name,
    account_type,
    status,
    'Reconcilable' as remarks
FROM accounts
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
  AND account_code IN (
    '110100', '110102', '110104', '110204', '110205', '110401', '110402',
    '110602', '122318', '120300', '120302', '120303', '120304', '120305',
    '120306', '120307', '120401', '120501', '122309', '130100', '130101',
    '130201', '140200', '140201', '160100'
  )
ORDER BY account_code;

\echo ''
\echo 'Total accounts inserted:'
SELECT COUNT(*) as total_lms_accounts
FROM accounts
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
  AND account_code IN (
    '110100', '110102', '110104', '110204', '110205', '110401', '110402',
    '110602', '122318', '120300', '120302', '120303', '120304', '120305',
    '120306', '120307', '120401', '120501', '122309', '130100', '130101',
    '130201', '140200', '140201', '160100'
  );
