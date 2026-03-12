-- V14: Add Fineract product mapping table and new product status values
-- Supports product activation workflow with Temporal + Fineract integration

-- ══════════ Fineract Product Mapping Table ══════════
CREATE TABLE IF NOT EXISTS product_fineract_mappings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    product_id      UUID NOT NULL,
    product_code    VARCHAR(50) NOT NULL,
    fineract_product_id VARCHAR(50) NOT NULL,
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pfm_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_pfm_tenant_product UNIQUE (tenant_id, product_id),
    CONSTRAINT uq_pfm_tenant_fineract UNIQUE (tenant_id, fineract_product_id)
);

CREATE INDEX idx_pfm_tenant ON product_fineract_mappings(tenant_id);
CREATE INDEX idx_pfm_product_code ON product_fineract_mappings(tenant_id, product_code);
