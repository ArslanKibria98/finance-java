-- V16: Wallet transaction (spend) limits + admin platform bounds + limit-change approval workflow
--
-- Distinct from the existing top-up limits (daily/monthly/single_top_up_limit) which cap money
-- coming INTO the wallet. These cap money going OUT (transfers + withdrawals).
--   * daily_transaction_limit   — max cumulative spend per calendar day
--   * monthly_transaction_limit — max cumulative spend per calendar month
-- A customer may request a higher limit; an admin must approve before it takes effect.
-- Admin-configured platform bounds (per tenant) constrain the range a customer may request.

-- 1. Per-wallet effective transaction limits ---------------------------------------------------
ALTER TABLE wallets
    ADD COLUMN daily_transaction_limit   NUMERIC(20,6) NOT NULL DEFAULT 20000,
    ADD COLUMN monthly_transaction_limit NUMERIC(20,6) NOT NULL DEFAULT 100000;

-- 2. Platform bounds (admin config) — one row per tenant --------------------------------------
CREATE TABLE wallet_limit_bounds (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL,
    min_daily_limit        NUMERIC(20,6) NOT NULL DEFAULT 0,
    max_daily_limit        NUMERIC(20,6) NOT NULL DEFAULT 50000,
    min_monthly_limit      NUMERIC(20,6) NOT NULL DEFAULT 0,
    max_monthly_limit      NUMERIC(20,6) NOT NULL DEFAULT 500000,
    default_daily_limit    NUMERIC(20,6) NOT NULL DEFAULT 20000,
    default_monthly_limit  NUMERIC(20,6) NOT NULL DEFAULT 100000,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by             UUID,
    version                INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_wallet_limit_bounds_tenant UNIQUE (tenant_id),
    CONSTRAINT chk_wlb_daily_range   CHECK (max_daily_limit   >= min_daily_limit),
    CONSTRAINT chk_wlb_monthly_range CHECK (max_monthly_limit >= min_monthly_limit)
);

CREATE INDEX idx_wallet_limit_bounds_tenant ON wallet_limit_bounds(tenant_id);

-- 3. Limit-change requests (approval workflow) ------------------------------------------------
CREATE TABLE wallet_limit_change_requests (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                 UUID NOT NULL,
    wallet_id                 UUID NOT NULL,
    customer_id               UUID NOT NULL,
    requested_daily_limit     NUMERIC(20,6) NOT NULL,
    requested_monthly_limit   NUMERIC(20,6) NOT NULL,
    current_daily_limit       NUMERIC(20,6) NOT NULL,
    current_monthly_limit     NUMERIC(20,6) NOT NULL,
    reason                    VARCHAR(500),
    status                    VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by              UUID,
    requested_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decision_by               UUID,
    decision_at               TIMESTAMPTZ,
    decision_notes            VARCHAR(500),
    rejection_reason          VARCHAR(500),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                   INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_wlcr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_wlcr_tenant            ON wallet_limit_change_requests(tenant_id);
CREATE INDEX idx_wlcr_tenant_status     ON wallet_limit_change_requests(tenant_id, status);
CREATE INDEX idx_wlcr_wallet            ON wallet_limit_change_requests(wallet_id);
CREATE INDEX idx_wlcr_wallet_status     ON wallet_limit_change_requests(wallet_id, status);

-- 4. Seed default bounds for the single tenant in DEV (idempotent) ----------------------------
INSERT INTO wallet_limit_bounds (tenant_id, min_daily_limit, max_daily_limit,
                                 min_monthly_limit, max_monthly_limit,
                                 default_daily_limit, default_monthly_limit)
VALUES ('00000000-0000-0000-0000-000000000001', 0, 50000, 0, 500000, 20000, 100000)
ON CONFLICT (tenant_id) DO NOTHING;
