-- V2__add_account_balance_movements.sql
-- Add columns required by AccountBalanceJpaEntity that were missing from V1 schema

ALTER TABLE account_balances
    ADD COLUMN IF NOT EXISTS debit_movement   NUMERIC(20, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS credit_movement  NUMERIC(20, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS transaction_count INT NOT NULL DEFAULT 0;
