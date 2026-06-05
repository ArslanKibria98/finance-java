-- V21: add the PER-TRANSACTION (single) dimension to wallet transaction limits.
--   * single_transaction_limit — max amount allowed in ONE transfer / withdrawal (not cumulative)
-- Admin platform bounds + the limit-change approval workflow gain matching single-limit columns.

-- 1. Per-wallet effective single (per-transaction) limit ---------------------------------------
ALTER TABLE wallets
    ADD COLUMN single_transaction_limit NUMERIC(20,6) NOT NULL DEFAULT 10000;

-- 2. Platform bounds: single min / max / default ----------------------------------------------
ALTER TABLE wallet_limit_bounds
    ADD COLUMN min_single_limit      NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN max_single_limit      NUMERIC(20,6) NOT NULL DEFAULT 50000,
    ADD COLUMN default_single_limit  NUMERIC(20,6) NOT NULL DEFAULT 10000;

ALTER TABLE wallet_limit_bounds
    ADD CONSTRAINT chk_wlb_single_range CHECK (max_single_limit >= min_single_limit);

-- 3. Limit-change requests: snapshot single limit ---------------------------------------------
ALTER TABLE wallet_limit_change_requests
    ADD COLUMN requested_single_limit NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN current_single_limit   NUMERIC(20,6) NOT NULL DEFAULT 0;
