-- V8: Add is_deleted soft delete flag to all LOV reference data tables
-- DELETE endpoint now does soft delete (permanent, not reversible)
-- Active/Inactive is a separate toggle via activate/deactivate endpoints

ALTER TABLE source_of_wealth_options  ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE source_of_funds_options   ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE source_of_income_options  ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE purpose_of_finance_options ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE net_worth_range_options   ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_sow_not_deleted  ON source_of_wealth_options(tenant_id)  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_sof_not_deleted  ON source_of_funds_options(tenant_id)   WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_soi_not_deleted  ON source_of_income_options(tenant_id)  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_pof_not_deleted  ON purpose_of_finance_options(tenant_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_nwr_not_deleted  ON net_worth_range_options(tenant_id)   WHERE is_deleted = false;
