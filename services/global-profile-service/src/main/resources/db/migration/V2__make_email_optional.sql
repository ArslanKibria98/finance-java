-- V2: Make email optional for onboarding (only mobile + NID available during KYC)
ALTER TABLE global_customers ALTER COLUMN global_email_hash DROP NOT NULL;

-- Replace absolute UNIQUE with partial unique index (only non-null emails must be unique)
ALTER TABLE global_customers DROP CONSTRAINT uq_global_email;
CREATE UNIQUE INDEX uq_global_email_non_null ON global_customers (global_email_hash) WHERE global_email_hash IS NOT NULL;
