-- ============================================================================
-- V14: Banks reference data table
-- Simple lookup table for Saudi banks used in loan disbursement
-- ============================================================================

CREATE TABLE banks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(10) NOT NULL,
    name_en         VARCHAR(100) NOT NULL,
    name_ar         VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_banks_tenant ON banks(tenant_id);
CREATE INDEX idx_banks_active ON banks(tenant_id, is_active) WHERE is_active = TRUE;

-- Seed KSA banks (global tenant = shared across all tenants)
INSERT INTO banks (tenant_id, code, name_en, name_ar, sort_order) VALUES
    ('00000000-0000-0000-0000-000000000001', '15', 'Al Bilad Bank', 'بنك البلاد', 1),
    ('00000000-0000-0000-0000-000000000001', '05', 'Al Inma Bank', 'بنك الإنماء', 2),
    ('00000000-0000-0000-0000-000000000001', '80', 'Al Rajhi Bank', 'بنك الراجحي', 3),
    ('00000000-0000-0000-0000-000000000001', '30', 'Arab National Bank', 'البنك العربي', 4),
    ('00000000-0000-0000-0000-000000000001', '60', 'Bank Al Jazira', 'بنك الجزيرة', 5),
    ('00000000-0000-0000-0000-000000000001', '76', 'Bank Muscat', 'بنك مسقط', 6),
    ('00000000-0000-0000-0000-000000000001', '55', 'Banque Saudi Fransi', 'البنك السعودي الفرنسي', 7),
    ('00000000-0000-0000-0000-000000000001', '95', 'Emirates Bank', 'البنك الإماراتي', 8),
    ('00000000-0000-0000-0000-000000000001', '90', 'Gulf International Bank', 'بنك الخليج', 9),
    ('00000000-0000-0000-0000-000000000001', '10', 'National Commercial Bank', 'البنك الأهلي السعودي', 10),
    ('00000000-0000-0000-0000-000000000001', '20', 'Riyad Bank', 'بنك الرياض', 11),
    ('00000000-0000-0000-0000-000000000001', '45', 'Saudi British Bank (SABB)', 'بنك ساب', 12),
    ('00000000-0000-0000-0000-000000000001', '65', 'Saudi Investment Bank', 'بنك الاستثمار', 13);

COMMENT ON TABLE banks IS 'KSA banks reference data for loan disbursement';
