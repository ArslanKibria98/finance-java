-- V18: Add is_catalog_visible flag to modules and permissions
-- Controls which permissions/modules show in the admin panel catalog UI
-- Hidden items still exist in DB for enforcement but are not displayed

ALTER TABLE modules ADD COLUMN IF NOT EXISTS is_catalog_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS is_catalog_visible BOOLEAN NOT NULL DEFAULT TRUE;

-- Mark LENDING module as catalog-hidden (mobile/customer-facing, not admin panel)
UPDATE modules SET is_catalog_visible = false
WHERE tenant_id = '00000000-0000-0000-0000-000000000001' AND module_code = 'LENDING';

-- Mark fine-grained CUSTOMER permissions as catalog-hidden (kept for enforcement, not displayed)
UPDATE permissions SET is_catalog_visible = false
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
  AND permission_code IN (
      'CUSTOMER_CREATE', 'CUSTOMER_UPDATE',
      'CUSTOMER_BANK_ACCOUNT_READ', 'CUSTOMER_BANK_ACCOUNT_CREATE',
      'CUSTOMER_EMPLOYMENT_CREATE', 'CUSTOMER_KYC_STATUS_UPDATE',
      'REFERENCE_DATA_READ', 'REFERENCE_DATA_CREATE',
      'REFERENCE_DATA_UPDATE', 'REFERENCE_DATA_DELETE'
  );

-- Mark all LENDING permissions as catalog-hidden
UPDATE permissions SET is_catalog_visible = false
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
  AND resource_type = 'LENDING';
