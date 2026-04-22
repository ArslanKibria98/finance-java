-- V7__es_config_single_row_per_range.sql
-- Collapse RANGE rows: one row per range span instead of one-row-per-invoice.
-- Model after migration:
--   SINGLE  → invoice_order NOT NULL, is_range=false, range_no=0, min/max NULL
--   RANGE   → invoice_order NULL,     is_range=true,  range_no>0, min+max populated
--
-- Dedup pre-existing RANGE rows (keep the earliest per (delinquency_id, range_no))
-- before relaxing the column to NULL, so we don't leave duplicates around.

DELETE FROM early_settlement_configs a
USING  early_settlement_configs b
WHERE  a.delinquency_id = b.delinquency_id
  AND  a.is_range       = true
  AND  b.is_range       = true
  AND  a.range_no       = b.range_no
  AND  a.range_no       > 0
  AND  a.created_at     > b.created_at;

ALTER TABLE early_settlement_configs
    ALTER COLUMN invoice_order DROP NOT NULL;

UPDATE early_settlement_configs
SET    invoice_order = NULL
WHERE  is_range = true;
