-- V2__add_iban_to_wallets.sql
-- Add IBAN column to wallets table for bank account linking during onboarding

ALTER TABLE wallets ADD COLUMN iban VARCHAR(34);
