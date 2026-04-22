-- ============================================================================
-- STANDALONE SCRIPT: Insert Standard Chart of Accounts
-- Run this directly in PostgreSQL if you don't want to wait for Flyway migration
-- Usage: psql -h localhost -U postgres -d ledger_service_db -f insert-coa-accounts.sql
-- ============================================================================

-- Test tenant ID (same as used in GL Reports test data)
-- Change this if you need to insert accounts for a different tenant
-- SELECT id FROM tenants LIMIT 1;  -- Or get your actual tenant ID

DO $$
DECLARE
    test_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    system_user_id UUID := '00000000-0000-0000-0000-000000000000';

    -- Asset accounts
    assets_header_id UUID := gen_random_uuid();
    current_assets_header_id UUID := gen_random_uuid();
    cash_bank_id UUID := gen_random_uuid();
    bank_account_sar_id UUID := gen_random_uuid();
    investments_id UUID := gen_random_uuid();
    non_current_assets_header_id UUID := gen_random_uuid();
    ppe_id UUID := gen_random_uuid();
    intangible_id UUID := gen_random_uuid();

    -- Liability accounts
    liabilities_header_id UUID := gen_random_uuid();
    current_liab_header_id UUID := gen_random_uuid();
    customer_deposits_id UUID := gen_random_uuid();
    accounts_payable_id UUID := gen_random_uuid();
    non_current_liab_header_id UUID := gen_random_uuid();
    long_term_financing_id UUID := gen_random_uuid();

    -- Equity accounts
    equity_header_id UUID := gen_random_uuid();
    share_capital_id UUID := gen_random_uuid();
    retained_earnings_id UUID := gen_random_uuid();
    zakat_fund_id UUID := gen_random_uuid();

    -- Income accounts
    income_header_id UUID := gen_random_uuid();
    murabaha_income_header_id UUID := gen_random_uuid();
    murabaha_profit_id UUID := gen_random_uuid();
    ijara_income_id UUID := gen_random_uuid();
    service_fees_id UUID := gen_random_uuid();
    riba_income_id UUID := gen_random_uuid();

    -- Expense accounts
    expense_header_id UUID := gen_random_uuid();
    salaries_id UUID := gen_random_uuid();
    admin_expenses_id UUID := gen_random_uuid();
    bad_debts_provision_id UUID := gen_random_uuid();
    depreciation_id UUID := gen_random_uuid();

BEGIN
    -- ====================================================================
    -- ASSETS (1000 series)
    -- ====================================================================
    INSERT INTO accounts (id, tenant_id, account_code, account_name, account_name_ar, account_type, status, parent_account_id, is_control_account, created_by, created_at)
    VALUES
        -- Assets Header
        (assets_header_id, test_tenant_id, '1000', 'ASSETS', 'اثاثیات', 'ASSET'::account_type, 'ACTIVE'::account_status, NULL, TRUE, system_user_id, NOW()),

        -- Current Assets Header
        (current_assets_header_id, test_tenant_id, '1100', 'Current Assets', 'موجودہ اثاثیات', 'ASSET'::account_type, 'ACTIVE'::account_status, assets_header_id, TRUE, system_user_id, NOW()),
        (cash_bank_id, test_tenant_id, '1110', 'Cash & Bank Balances', 'نقد اور بینک میں رقم', 'ASSET'::account_type, 'ACTIVE'::account_status, current_assets_header_id, FALSE, system_user_id, NOW()),
        (bank_account_sar_id, test_tenant_id, '1111', 'Bank Account - SAR', 'بینک اکاؤنٹ - ریال', 'ASSET'::account_type, 'ACTIVE'::account_status, cash_bank_id, FALSE, system_user_id, NOW()),
        (investments_id, test_tenant_id, '1120', 'Investment Securities', 'سرمایہ کاری کے اوراق', 'ASSET'::account_type, 'ACTIVE'::account_status, current_assets_header_id, FALSE, system_user_id, NOW()),

        -- Non-Current Assets Header
        (non_current_assets_header_id, test_tenant_id, '1200', 'Non-Current Assets', 'غیر موجودہ اثاثیات', 'ASSET'::account_type, 'ACTIVE'::account_status, assets_header_id, TRUE, system_user_id, NOW()),
        (ppe_id, test_tenant_id, '1210', 'Property, Plant & Equipment', 'سازوسامان اور عمارت', 'ASSET'::account_type, 'ACTIVE'::account_status, non_current_assets_header_id, FALSE, system_user_id, NOW()),
        (intangible_id, test_tenant_id, '1220', 'Intangible Assets', 'غیر مرئی اثاثیات', 'ASSET'::account_type, 'ACTIVE'::account_status, non_current_assets_header_id, FALSE, system_user_id, NOW());

    -- ====================================================================
    -- LIABILITIES (2000 series)
    -- ====================================================================
    INSERT INTO accounts (id, tenant_id, account_code, account_name, account_name_ar, account_type, status, parent_account_id, is_control_account, created_by, created_at)
    VALUES
        -- Liabilities Header
        (liabilities_header_id, test_tenant_id, '2000', 'LIABILITIES', 'ذمہ داریاں', 'LIABILITY'::account_type, 'ACTIVE'::account_status, NULL, TRUE, system_user_id, NOW()),

        -- Current Liabilities Header
        (current_liab_header_id, test_tenant_id, '2100', 'Current Liabilities', 'موجودہ ذمہ داریاں', 'LIABILITY'::account_type, 'ACTIVE'::account_status, liabilities_header_id, TRUE, system_user_id, NOW()),
        (customer_deposits_id, test_tenant_id, '2110', 'Customer Deposits', 'گاہک کی جمع رقم', 'LIABILITY'::account_type, 'ACTIVE'::account_status, current_liab_header_id, FALSE, system_user_id, NOW()),
        (accounts_payable_id, test_tenant_id, '2120', 'Accounts Payable', 'واجب الادا رقم', 'LIABILITY'::account_type, 'ACTIVE'::account_status, current_liab_header_id, FALSE, system_user_id, NOW()),

        -- Non-Current Liabilities Header
        (non_current_liab_header_id, test_tenant_id, '2200', 'Non-Current Liabilities', 'غیر موجودہ ذمہ داریاں', 'LIABILITY'::account_type, 'ACTIVE'::account_status, liabilities_header_id, TRUE, system_user_id, NOW()),
        (long_term_financing_id, test_tenant_id, '2210', 'Long-term Financing', 'طویل مدتی فنڈنگ', 'LIABILITY'::account_type, 'ACTIVE'::account_status, non_current_liab_header_id, FALSE, system_user_id, NOW());

    -- ====================================================================
    -- EQUITY (3000 series)
    -- ====================================================================
    INSERT INTO accounts (id, tenant_id, account_code, account_name, account_name_ar, account_type, status, parent_account_id, is_control_account, created_by, created_at)
    VALUES
        (equity_header_id, test_tenant_id, '3000', 'EQUITY', 'سرمایہ', 'EQUITY'::account_type, 'ACTIVE'::account_status, NULL, TRUE, system_user_id, NOW()),
        (share_capital_id, test_tenant_id, '3100', 'Share Capital', 'حصص کی رقم', 'EQUITY'::account_type, 'ACTIVE'::account_status, equity_header_id, FALSE, system_user_id, NOW()),
        (retained_earnings_id, test_tenant_id, '3200', 'Retained Earnings', 'برقرار کمائی', 'EQUITY'::account_type, 'ACTIVE'::account_status, equity_header_id, FALSE, system_user_id, NOW()),
        (zakat_fund_id, test_tenant_id, '3300', 'Zakat Fund', 'زکوۃ فنڈ', 'EQUITY'::account_type, 'ACTIVE'::account_status, equity_header_id, FALSE, system_user_id, NOW());

    -- ====================================================================
    -- INCOME (4000 series)
    -- ====================================================================
    INSERT INTO accounts (id, tenant_id, account_code, account_name, account_name_ar, account_type, status, parent_account_id, is_control_account, created_by, created_at)
    VALUES
        (income_header_id, test_tenant_id, '4000', 'INCOME', 'آمدنی', 'INCOME'::account_type, 'ACTIVE'::account_status, NULL, TRUE, system_user_id, NOW()),

        (murabaha_income_header_id, test_tenant_id, '4100', 'Murabaha Income', 'مرابحہ کی آمدنی', 'INCOME'::account_type, 'ACTIVE'::account_status, income_header_id, TRUE, system_user_id, NOW()),
        (murabaha_profit_id, test_tenant_id, '4110', 'Murabaha Profit', 'مرابحہ منافع', 'INCOME'::account_type, 'ACTIVE'::account_status, murabaha_income_header_id, FALSE, system_user_id, NOW()),

        (ijara_income_id, test_tenant_id, '4200', 'Ijara Income', 'اجارہ کی آمدنی', 'INCOME'::account_type, 'ACTIVE'::account_status, income_header_id, FALSE, system_user_id, NOW()),
        (service_fees_id, test_tenant_id, '4300', 'Service Fees', 'خدمات کی فیس', 'INCOME'::account_type, 'ACTIVE'::account_status, income_header_id, FALSE, system_user_id, NOW()),
        (riba_income_id, test_tenant_id, '4400', 'Interest/Riba (Non-Compliant)', 'سود (غیر شرعی)', 'INCOME'::account_type, 'ACTIVE'::account_status, income_header_id, FALSE, system_user_id, NOW());

    -- ====================================================================
    -- EXPENSES (5000 series)
    -- ====================================================================
    INSERT INTO accounts (id, tenant_id, account_code, account_name, account_name_ar, account_type, status, parent_account_id, is_control_account, created_by, created_at)
    VALUES
        (expense_header_id, test_tenant_id, '5000', 'EXPENSES', 'اخراجات', 'EXPENSE'::account_type, 'ACTIVE'::account_status, NULL, TRUE, system_user_id, NOW()),
        (salaries_id, test_tenant_id, '5100', 'Salaries & Benefits', 'تنخواہیں اور فوائد', 'EXPENSE'::account_type, 'ACTIVE'::account_status, expense_header_id, FALSE, system_user_id, NOW()),
        (admin_expenses_id, test_tenant_id, '5200', 'Administrative Expenses', 'انتظامی اخراجات', 'EXPENSE'::account_type, 'ACTIVE'::account_status, expense_header_id, FALSE, system_user_id, NOW()),
        (bad_debts_provision_id, test_tenant_id, '5300', 'Provision for Bad Debts', 'مشکوک ڈیبٹ کے لیے ذخیرہ', 'EXPENSE'::account_type, 'ACTIVE'::account_status, expense_header_id, FALSE, system_user_id, NOW()),
        (depreciation_id, test_tenant_id, '5400', 'Depreciation', 'قیمت میں کمی', 'EXPENSE'::account_type, 'ACTIVE'::account_status, expense_header_id, FALSE, system_user_id, NOW());

    RAISE NOTICE '✓ Successfully inserted 30 GL accounts for tenant %', test_tenant_id;
    RAISE NOTICE '✓ Chart of Accounts ready for use!';

END $$;

-- Verify insertion - list all accounts by category
SELECT '====== GL ACCOUNTS SUMMARY ======' AS info;
SELECT
    account_code,
    account_name,
    account_name_ar,
    account_type,
    status,
    CASE WHEN is_control_account THEN 'Header' ELSE 'Leaf' END as level
FROM accounts
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
ORDER BY account_code;
