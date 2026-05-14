-- ============================================================================
-- V8: AML / sanctions screening columns + HELD_AML index.
-- Split from V7 because Postgres requires the ALTER TYPE ... ADD VALUE
-- (added in V7) to commit before the new value can appear in a partial index.
-- ============================================================================

ALTER TABLE wallet_withdrawals
    ADD COLUMN screening_ref       VARCHAR(50),
    ADD COLUMN screening_decision  VARCHAR(20),
    ADD COLUMN screening_score     INT,
    ADD COLUMN screening_matches   JSONB,
    ADD COLUMN screened_at         TIMESTAMPTZ,
    ADD COLUMN edd_required        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN released_by         UUID,
    ADD COLUMN released_at         TIMESTAMPTZ,
    ADD COLUMN release_reason      TEXT,
    ADD CONSTRAINT chk_wd_screening_decision CHECK (
        screening_decision IS NULL OR screening_decision IN ('PASS','HOLD_FOR_REVIEW','REJECT')
    );

-- Index for compliance dashboard (held items pending review)
CREATE INDEX idx_wd_held_aml
    ON wallet_withdrawals(tenant_id, status)
    WHERE status = 'HELD_AML';

CREATE INDEX idx_wd_screening_ref
    ON wallet_withdrawals(screening_ref)
    WHERE screening_ref IS NOT NULL;

COMMENT ON COLUMN wallet_withdrawals.screening_ref      IS 'Reference returned by screening adapter for audit trail';
COMMENT ON COLUMN wallet_withdrawals.screening_decision IS 'PASS | HOLD_FOR_REVIEW | REJECT';
COMMENT ON COLUMN wallet_withdrawals.screening_score    IS '0-100 risk score';
COMMENT ON COLUMN wallet_withdrawals.screening_matches  IS 'JSONB array of {listType, matchedName, matchScore, reason}';
COMMENT ON COLUMN wallet_withdrawals.edd_required       IS 'Enhanced Due Diligence required flag';
COMMENT ON COLUMN wallet_withdrawals.released_by        IS 'Compliance officer userId who released a HELD_AML row';
