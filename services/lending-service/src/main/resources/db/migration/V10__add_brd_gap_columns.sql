-- V10__add_brd_gap_columns.sql
-- Adds columns identified in BRD V1.8 gap analysis:
-- Individual expense categories, SafeWatch AML, Masdar employment,
-- AML declaration, OTP/IVR retry counters, APR, NABA, PaymentGuard

-- ══════════ Individual Expense Categories (BRD Section 3.3) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS food_groceries NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS utilities NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS healthcare NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS communication NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS housing_rent NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS clothing_essentials NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS education NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS transportation NUMERIC(20, 6);

-- ══════════ Purpose of Finance Other (BRD UC_LA_02) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS purpose_of_finance_other VARCHAR(255);

-- ══════════ APR (Annual Percentage Rate) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS apr NUMERIC(10, 8);

-- ══════════ SafeWatch AML Screening (BRD Phase 1) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS safe_watch_session_id VARCHAR(100);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS safe_watch_status VARCHAR(50);

-- ══════════ Masdar Employment Verification (BRD Phase 5) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS employer_name VARCHAR(255);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS employment_sector VARCHAR(100);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS employment_status VARCHAR(50);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS basic_salary NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS total_salary NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS employment_start_date VARCHAR(20);

-- ══════════ AML Declaration (BRD Phase 6) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS aml_declaration_completed BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS aml_declaration_at TIMESTAMPTZ;

-- ══════════ OTP/IVR Retry Counters (BRD: max 3 attempts each) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS otp_attempts INT DEFAULT 0;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS ivr_attempts INT DEFAULT 0;

-- ══════════ NABA Notification (BRD Phase 8 - Absher messaging) ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS naba_notification_sent BOOLEAN DEFAULT FALSE;

-- ══════════ PaymentGuard Fraud Check ══════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS payment_guard_session_id VARCHAR(100);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS payment_guard_status VARCHAR(50);
