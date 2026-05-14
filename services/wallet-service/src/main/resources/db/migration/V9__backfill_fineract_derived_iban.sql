-- =====================================================================
-- V9__backfill_fineract_derived_iban.sql
-- ---------------------------------------------------------------------
-- One-time backfill: replace pre-existing random IBANs with deterministic
-- SA IBANs derived from the Fineract savings account id, matching the new
-- com.ksa.financing.wallet.domain.iban.IbanGenerator algorithm.
--
-- Format (ISO 13616, length 24):
--   SA | check-digits(2) | bank-code(2) | account-number(18)
--
-- Only wallets that already have a Fineract savings account id are touched
-- (i.e. wallets that were successfully synced to Fineract). Other wallets
-- keep their current IBAN value.
-- =====================================================================

-- MOD-97 SA IBAN computation (ISO 13616). bank_code defaults to '80', which
-- matches the WalletFineractFeatureFlag.ibanBankCode default in application code.
CREATE OR REPLACE FUNCTION compute_sa_iban_from_savings_id(
    p_savings_id BIGINT,
    p_bank_code  TEXT DEFAULT '80'
) RETURNS TEXT AS $$
DECLARE
    v_account_no TEXT;
    v_numeric    TEXT;
    v_check_int  INT;
BEGIN
    IF p_savings_id IS NULL OR p_savings_id <= 0 THEN
        RETURN NULL;
    END IF;
    IF p_bank_code IS NULL OR p_bank_code !~ '^\d{2}$' THEN
        RAISE EXCEPTION 'bank_code must be exactly 2 digits, got: %', p_bank_code;
    END IF;

    -- 18-digit zero-padded account number
    v_account_no := LPAD(p_savings_id::TEXT, 18, '0');
    IF LENGTH(v_account_no) <> 18 THEN
        RAISE EXCEPTION 'savings id exceeds 18 digits: %', p_savings_id;
    END IF;

    -- Numeric string per ISO 13616: bankCode + account + "2810" (S=28,A=10) + "00"
    v_numeric := p_bank_code || v_account_no || '2810' || '00';

    -- check digits = 98 - (numeric mod 97). NUMERIC handles 26-digit input.
    v_check_int := 98 - (v_numeric::NUMERIC % 97)::INT;

    RETURN 'SA' || LPAD(v_check_int::TEXT, 2, '0') || p_bank_code || v_account_no;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Backfill: only wallets that have been linked to a Fineract savings account.
UPDATE wallets
SET    iban       = compute_sa_iban_from_savings_id(fineract_savings_account_id, '80'),
       updated_at = NOW()
WHERE  fineract_savings_account_id IS NOT NULL;
