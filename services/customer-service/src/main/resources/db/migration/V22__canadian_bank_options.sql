-- ============================================================================
-- V22: Canadian financial institution options (LOV)
-- Admin-managed lookup; same shape as relationship_options / occupation_options
-- `code` holds the 3-digit Canadian financial institution number (FI number).
-- Used for bank-account capture during onboarding (source of wealth / income flows).
-- ============================================================================

CREATE TABLE canadian_bank_options (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(50) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    name_ar         VARCHAR(255),
    description_en  TEXT,
    description_ar  TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_canadian_bank_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_canadian_bank_tenant ON canadian_bank_options(tenant_id);
CREATE INDEX idx_canadian_bank_active ON canadian_bank_options(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_canadian_bank_not_deleted ON canadian_bank_options(tenant_id) WHERE is_deleted = FALSE;

CREATE TRIGGER trg_canadian_bank_updated
    BEFORE UPDATE ON canadian_bank_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE canadian_bank_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY canadian_bank_tenant_isolation ON canadian_bank_options
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- name_ar omitted (nullable) — proper-noun institution names, Arabic not maintained.
INSERT INTO canadian_bank_options (tenant_id, code, name_en, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', '001', 'Bank of Montreal',                  1),
    ('00000000-0000-0000-0000-000000000001', '002', 'Bank of Nova Scotia',               2),
    ('00000000-0000-0000-0000-000000000001', '003', 'Royal Bank of Canada',              3),
    ('00000000-0000-0000-0000-000000000001', '004', 'Toronto Dominion',                  4),
    ('00000000-0000-0000-0000-000000000001', '006', 'Banque National Du Canada',         5),
    ('00000000-0000-0000-0000-000000000001', '010', 'CIBC',                              6),
    ('00000000-0000-0000-0000-000000000001', '016', 'Hong Kong Bank of Canada',          7),
    ('00000000-0000-0000-0000-000000000001', '039', 'Laurentian Bank of Canada',         8),
    ('00000000-0000-0000-0000-000000000001', '072', 'Credit Union Centre Ontario',       9),
    ('00000000-0000-0000-0000-000000000001', '239', 'Province of Ontario Savings',       10),
    ('00000000-0000-0000-0000-000000000001', '240', 'ABN Ampro',                         11),
    ('00000000-0000-0000-0000-000000000001', '241', 'Bank of America NA, Canada',        12),
    ('00000000-0000-0000-0000-000000000001', '260', 'Citibank Canada',                   13),
    ('00000000-0000-0000-0000-000000000001', '270', 'Bank One',                          14),
    ('00000000-0000-0000-0000-000000000001', '307', 'Bank of East Asia Canada',          15),
    ('00000000-0000-0000-0000-000000000001', '309', 'Citizens Bank of Canada',           16),
    ('00000000-0000-0000-0000-000000000001', '326', 'Amicus Bank',                       17),
    ('00000000-0000-0000-0000-000000000001', '328', 'Citibank NA Canadian Branch',       18),
    ('00000000-0000-0000-0000-000000000001', '338', 'Canadian Tire Bank',                19),
    ('00000000-0000-0000-0000-000000000001', '509', 'Canada Trust',                      20),
    ('00000000-0000-0000-0000-000000000001', '519', 'Central Guaranty Trust Co.',        21),
    ('00000000-0000-0000-0000-000000000001', '540', 'Manulife Bank of Canada',           22),
    ('00000000-0000-0000-0000-000000000001', '580', 'Royal Trust Corp. of Canada',       23),
    ('00000000-0000-0000-0000-000000000001', '590', 'National Trust',                    24),
    ('00000000-0000-0000-0000-000000000001', '614', 'ING Direct',                        25),
    ('00000000-0000-0000-0000-000000000001', '806', 'Duca Community Credit Union',       26),
    ('00000000-0000-0000-0000-000000000001', '809', 'BC Central Credit Union',           27),
    ('00000000-0000-0000-0000-000000000001', '815', 'Caisse Populaire DesJardins',       28),
    ('00000000-0000-0000-0000-000000000001', '819', 'Caisse Populaire',                  29),
    ('00000000-0000-0000-0000-000000000001', '828', 'Credit Union',                      30),
    ('00000000-0000-0000-0000-000000000001', '829', 'Caisse Populaire',                  31),
    ('00000000-0000-0000-0000-000000000001', '836', 'Caisse Populaire',                  32),
    ('00000000-0000-0000-0000-000000000001', '837', 'Hepcoe Credit Union Limited',       33),
    ('00000000-0000-0000-0000-000000000001', '846', 'Queen''s Park Civil Service CU',     34),
    ('00000000-0000-0000-0000-000000000001', '850', 'Polysar Lambton CU Limited',        35),
    ('00000000-0000-0000-0000-000000000001', '879', 'Cambrian Credit Union',             36),
    ('00000000-0000-0000-0000-000000000001', '889', 'Saskatchewan Financial Inst',       37),
    ('00000000-0000-0000-0000-000000000001', '890', 'Caisse Populaire',                  38),
    ('00000000-0000-0000-0000-000000000001', '899', 'Alberta Credit Unions',             39);

COMMENT ON TABLE canadian_bank_options IS 'Configurable Canadian financial institution reference data (per tenant); code = 3-digit FI number';
