-- V6: Fix unique constraints to work correctly with soft-delete pattern
-- Problem: UNIQUE(tenant_id, code) blocks re-creation of soft-deleted records
-- Solution: Partial unique index that only applies to active (non-deleted) records

-- ===================== api_clients =====================
ALTER TABLE api_clients DROP CONSTRAINT api_clients_tenant_id_code_key;
CREATE UNIQUE INDEX uq_clients_tenant_code_active
    ON api_clients(tenant_id, code) WHERE deleted_at IS NULL;

-- ===================== third_party_providers =====================
ALTER TABLE third_party_providers DROP CONSTRAINT third_party_providers_tenant_id_code_key;
CREATE UNIQUE INDEX uq_providers_tenant_code_active
    ON third_party_providers(tenant_id, code) WHERE deleted_at IS NULL;

-- ===================== provider_apis =====================
ALTER TABLE provider_apis DROP CONSTRAINT provider_apis_tenant_id_code_key;
CREATE UNIQUE INDEX uq_apis_tenant_code_active
    ON provider_apis(tenant_id, code) WHERE deleted_at IS NULL;
