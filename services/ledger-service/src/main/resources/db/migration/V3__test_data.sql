-- ============================================================================
-- TEST DATA FOR GL REPORTS - Sample accounts and journal entries
-- ============================================================================

-- Use a fixed tenant ID for testing (matching superadmin's tenant)
DO $$
DECLARE
    test_tenant_id UUID := '00000000-0000-0000-0000-000000000001';
    bank_account_id UUID := '550e8400-e29b-41d4-a716-446655440001';
    loans_receivable_id UUID := '550e8400-e29b-41d4-a716-446655440002';
    profit_income_id UUID := '550e8400-e29b-41d4-a716-446655440003';
    equity_id UUID := '550e8400-e29b-41d4-a716-446655440004';
    je_id UUID := '550e8400-e29b-41d4-a716-446655440010';
BEGIN
    -- Insert GL Accounts
    INSERT INTO accounts (id, tenant_id, account_code, account_name, account_type, status, created_by, created_at)
    VALUES
        (bank_account_id, test_tenant_id, '1010', 'Bank Account', 'ASSET'::account_type, 'ACTIVE'::account_status, '00000000-0000-0000-0000-000000000000'::UUID, NOW()),
        (loans_receivable_id, test_tenant_id, '1200', 'Loans Receivable', 'ASSET'::account_type, 'ACTIVE'::account_status, '00000000-0000-0000-0000-000000000000'::UUID, NOW()),
        (profit_income_id, test_tenant_id, '4010', 'Murabaha Profit Income', 'INCOME'::account_type, 'ACTIVE'::account_status, '00000000-0000-0000-0000-000000000000'::UUID, NOW()),
        (equity_id, test_tenant_id, '3000', 'Share Capital', 'EQUITY'::account_type, 'ACTIVE'::account_status, '00000000-0000-0000-0000-000000000000'::UUID, NOW());

    -- Insert opening balances (as of 2026-01-01)
    INSERT INTO account_balances (id, tenant_id, account_id, balance_date, opening_balance, closing_balance, created_at)
    VALUES
        (gen_random_uuid(), test_tenant_id, bank_account_id, '2026-01-01'::DATE, '1000000.00'::NUMERIC(19,2), '1000000.00'::NUMERIC(19,2), NOW()),
        (gen_random_uuid(), test_tenant_id, loans_receivable_id, '2026-01-01'::DATE, '0.00'::NUMERIC(19,2), '0.00'::NUMERIC(19,2), NOW()),
        (gen_random_uuid(), test_tenant_id, profit_income_id, '2026-01-01'::DATE, '0.00'::NUMERIC(19,2), '0.00'::NUMERIC(19,2), NOW()),
        (gen_random_uuid(), test_tenant_id, equity_id, '2026-01-01'::DATE, '1000000.00'::NUMERIC(19,2), '1000000.00'::NUMERIC(19,2), NOW());

    -- Sample Journal Entry 1: Loan Disbursement on 2026-01-15
    -- Debit Loans Receivable, Credit Bank Account
    INSERT INTO journal_entries (id, tenant_id, entry_number, entry_date, value_date, status, description, reference_type, total_debit, total_credit, created_by, created_at)
    VALUES (je_id, test_tenant_id, 'JE001', '2026-01-15'::DATE, '2026-01-15'::DATE, 'POSTED'::entry_status, 'Loan Disbursement - Customer ABC', 'LOAN', '500000.000000'::NUMERIC(20,6), '500000.000000'::NUMERIC(20,6), '00000000-0000-0000-0000-000000000000'::UUID, NOW());

    INSERT INTO journal_lines (id, journal_entry_id, tenant_id, account_id, line_number, debit_amount, credit_amount, description, created_at)
    VALUES
        (gen_random_uuid(), je_id, test_tenant_id, loans_receivable_id, 1, '500000.000000'::NUMERIC(20,6), '0.000000'::NUMERIC(20,6), 'Debit Loans Receivable', NOW()),
        (gen_random_uuid(), je_id, test_tenant_id, bank_account_id, 2, '0.000000'::NUMERIC(20,6), '500000.000000'::NUMERIC(20,6), 'Credit Bank Account', NOW());

    -- Sample Journal Entry 2: Profit Accrual on 2026-02-01
    -- Debit Bank Account, Credit Profit Income
    INSERT INTO journal_entries (id, tenant_id, entry_number, entry_date, value_date, status, description, reference_type, total_debit, total_credit, created_by, created_at)
    VALUES (gen_random_uuid(), test_tenant_id, 'JE002', '2026-02-01'::DATE, '2026-02-01'::DATE, 'POSTED'::entry_status, 'Monthly Profit Accrual', 'ACCRUAL', '5000.000000'::NUMERIC(20,6), '5000.000000'::NUMERIC(20,6), '00000000-0000-0000-0000-000000000000'::UUID, NOW());

    INSERT INTO journal_lines (id, journal_entry_id, tenant_id, account_id, line_number, debit_amount, credit_amount, description, created_at)
    VALUES
        (gen_random_uuid(), (SELECT id FROM journal_entries WHERE entry_number = 'JE002' AND tenant_id = test_tenant_id), test_tenant_id, bank_account_id, 1, '5000.000000'::NUMERIC(20,6), '0.000000'::NUMERIC(20,6), 'Debit Bank Account', NOW()),
        (gen_random_uuid(), (SELECT id FROM journal_entries WHERE entry_number = 'JE002' AND tenant_id = test_tenant_id), test_tenant_id, profit_income_id, 2, '0.000000'::NUMERIC(20,6), '5000.000000'::NUMERIC(20,6), 'Credit Profit Income', NOW());

    -- Sample Journal Entry 3: Loan Repayment on 2026-03-10
    -- Debit Bank Account, Credit Loans Receivable
    INSERT INTO journal_entries (id, tenant_id, entry_number, entry_date, value_date, status, description, reference_type, total_debit, total_credit, created_by, created_at)
    VALUES (gen_random_uuid(), test_tenant_id, 'JE003', '2026-03-10'::DATE, '2026-03-10'::DATE, 'POSTED'::entry_status, 'Loan Repayment - Customer ABC', 'REPAY', '50000.000000'::NUMERIC(20,6), '50000.000000'::NUMERIC(20,6), '00000000-0000-0000-0000-000000000000'::UUID, NOW());

    INSERT INTO journal_lines (id, journal_entry_id, tenant_id, account_id, line_number, debit_amount, credit_amount, description, created_at)
    VALUES
        (gen_random_uuid(), (SELECT id FROM journal_entries WHERE entry_number = 'JE003' AND tenant_id = test_tenant_id), test_tenant_id, bank_account_id, 1, '50000.000000'::NUMERIC(20,6), '0.000000'::NUMERIC(20,6), 'Debit Bank Account', NOW()),
        (gen_random_uuid(), (SELECT id FROM journal_entries WHERE entry_number = 'JE003' AND tenant_id = test_tenant_id), test_tenant_id, loans_receivable_id, 2, '0.000000'::NUMERIC(20,6), '50000.000000'::NUMERIC(20,6), 'Credit Loans Receivable', NOW());

    -- Update account balances for 2026-03-30
    INSERT INTO account_balances (id, tenant_id, account_id, balance_date, opening_balance, closing_balance, created_at)
    VALUES
        (gen_random_uuid(), test_tenant_id, bank_account_id, '2026-03-30'::DATE, '1000000.000000'::NUMERIC(20,6), '1455000.000000'::NUMERIC(20,6), NOW()),
        (gen_random_uuid(), test_tenant_id, loans_receivable_id, '2026-03-30'::DATE, '0.000000'::NUMERIC(20,6), '450000.000000'::NUMERIC(20,6), NOW()),
        (gen_random_uuid(), test_tenant_id, profit_income_id, '2026-03-30'::DATE, '0.000000'::NUMERIC(20,6), '5000.000000'::NUMERIC(20,6), NOW()),
        (gen_random_uuid(), test_tenant_id, equity_id, '2026-03-30'::DATE, '1000000.000000'::NUMERIC(20,6), '1000000.000000'::NUMERIC(20,6), NOW());

END $$;
