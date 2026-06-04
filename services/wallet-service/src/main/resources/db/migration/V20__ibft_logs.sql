-- ============================================================================
-- IBFT audit + reconciliation logs
-- ============================================================================
CREATE TABLE ibft_status_history (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    ibft_id      UUID NOT NULL REFERENCES ibft_transactions(id),
    from_status  ibft_status,
    to_status    ibft_status NOT NULL,
    reason       VARCHAR(500),
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ibft_history ON ibft_status_history(ibft_id, changed_at DESC);

CREATE OR REPLACE FUNCTION track_ibft_status()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') OR (OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO ibft_status_history (tenant_id, ibft_id, from_status, to_status, reason)
        VALUES (NEW.tenant_id, NEW.id,
                CASE WHEN TG_OP = 'INSERT' THEN NULL ELSE OLD.status END,
                NEW.status, NEW.error_message);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_ibft_status
    AFTER INSERT OR UPDATE ON ibft_transactions FOR EACH ROW EXECUTE FUNCTION track_ibft_status();

-- Per-call Scotia EFT log
CREATE TABLE scotia_eft_logs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    ibft_id              UUID REFERENCES ibft_transactions(id),
    api_code             VARCHAR(60) NOT NULL,
    middleware_request_id VARCHAR(60),
    success              BOOLEAN,
    status               VARCHAR(60),
    detail               TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_scotia_eft_logs_ibft ON scotia_eft_logs(ibft_id, created_at DESC);

-- Reconciliation run summary
CREATE TABLE ibft_reconciliation_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scanned        INT NOT NULL DEFAULT 0,
    settled        INT NOT NULL DEFAULT 0,
    failed         INT NOT NULL DEFAULT 0,
    still_pending  INT NOT NULL DEFAULT 0,
    errors         INT NOT NULL DEFAULT 0,
    duration_ms    BIGINT NOT NULL DEFAULT 0
);
