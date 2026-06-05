-- V23: add the WEEKLY dimension to wallet transaction limits (alongside daily/monthly/yearly
--      from V16-V17 and single from V21).
--   * weekly_transaction_limit — max cumulative spend per ISO week (Monday-Sunday, KSA timezone)
-- Admin platform bounds + the limit-change approval workflow gain matching weekly columns.

-- 1. Per-wallet effective weekly limit --------------------------------------------------------
ALTER TABLE wallets
    ADD COLUMN weekly_transaction_limit NUMERIC(20,6) NOT NULL DEFAULT 50000;

-- 2. Platform bounds: weekly min / max / default ----------------------------------------------
ALTER TABLE wallet_limit_bounds
    ADD COLUMN min_weekly_limit      NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN max_weekly_limit      NUMERIC(20,6) NOT NULL DEFAULT 200000,
    ADD COLUMN default_weekly_limit  NUMERIC(20,6) NOT NULL DEFAULT 50000;

ALTER TABLE wallet_limit_bounds
    ADD CONSTRAINT chk_wlb_weekly_range CHECK (max_weekly_limit >= min_weekly_limit);

-- 3. Limit-change requests: snapshot weekly limits --------------------------------------------
ALTER TABLE wallet_limit_change_requests
    ADD COLUMN requested_weekly_limit NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN current_weekly_limit   NUMERIC(20,6) NOT NULL DEFAULT 0;
