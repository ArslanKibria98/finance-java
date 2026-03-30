-- BRD V1.8: Add 2-component profit formula columns and first installment due date

-- First installment due date (BRD: 30 days from application date)
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS first_installment_due_date DATE;

-- 2-component profit formula (BRD Page 14, Step 34)
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS cost_of_term NUMERIC(20,6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS total_cost_of_financing NUMERIC(20,6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS cost_of_term_percent NUMERIC(10,8);

-- Emdha digital signature tracking (BRD: contract signing via Emdha)
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS emdha_signature_id VARCHAR(255);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS emdha_signed BOOLEAN DEFAULT FALSE;

-- ANB B2B disbursement tracking (BRD Steps 64-69)
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS anb_transaction_id VARCHAR(255);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS anb_disbursement_status VARCHAR(50);

-- Also add to loans table for post-disbursement reference
ALTER TABLE loans ADD COLUMN IF NOT EXISTS first_installment_due_date DATE;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS anb_transaction_id VARCHAR(255);
