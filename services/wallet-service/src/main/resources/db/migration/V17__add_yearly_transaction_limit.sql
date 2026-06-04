-- V17: add the YEARLY dimension to wallet transaction limits (alongside daily + monthly from V16).
--   * yearly_transaction_limit — max cumulative spend per calendar year
-- Admin platform bounds + the limit-change approval workflow gain matching yearly columns.

-- 1. Per-wallet effective yearly limit ---------------------------------------------------------
ALTER TABLE wallets
    ADD COLUMN yearly_transaction_limit NUMERIC(20,6) NOT NULL DEFAULT 1000000;

-- 2. Platform bounds: yearly min / max / default ----------------------------------------------
ALTER TABLE wallet_limit_bounds
    ADD COLUMN min_yearly_limit      NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN max_yearly_limit      NUMERIC(20,6) NOT NULL DEFAULT 5000000,
    ADD COLUMN default_yearly_limit  NUMERIC(20,6) NOT NULL DEFAULT 1000000;

ALTER TABLE wallet_limit_bounds
    ADD CONSTRAINT chk_wlb_yearly_range CHECK (max_yearly_limit >= min_yearly_limit);

-- 3. Limit-change requests: snapshot yearly limits --------------------------------------------
ALTER TABLE wallet_limit_change_requests
    ADD COLUMN requested_yearly_limit NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN current_yearly_limit   NUMERIC(20,6) NOT NULL DEFAULT 0;
