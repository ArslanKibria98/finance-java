-- V15: Add fineract_product_id directly on products table
-- This stores the Fineract CBS product ID on the product itself,
-- allowing direct lookup without joining the mapping table.

ALTER TABLE products
    ADD COLUMN fineract_product_id VARCHAR(50);

-- Index for lookups by Fineract ID (e.g., webhooks, reconciliation)
CREATE INDEX idx_products_fineract_id ON products(tenant_id, fineract_product_id)
    WHERE fineract_product_id IS NOT NULL;

-- Backfill from existing mapping table
UPDATE products p
SET fineract_product_id = m.fineract_product_id
FROM product_fineract_mappings m
WHERE p.id = m.product_id
  AND p.tenant_id = m.tenant_id;
