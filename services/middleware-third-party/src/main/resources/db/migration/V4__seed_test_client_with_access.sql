-- V4: Seed a TEST client with access to ALL APIs for mock testing
DO $$
DECLARE
    v_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_client_id UUID;
    v_api RECORD;
BEGIN

-- ===================== TEST CLIENT =====================
INSERT INTO api_clients (id, tenant_id, name, code, secret_key, status, environment, version)
VALUES (
    gen_random_uuid(),
    v_tenant,
    'Test Mock Client',
    'TEST_MOCK_CLIENT',
    'test-mock-secret-key-2026',
    'ACTIVE',
    'TEST',
    1
)
RETURNING id INTO v_client_id;

-- ===================== GRANT ACCESS TO ALL APIs =====================
FOR v_api IN
    SELECT id, provider_id FROM provider_apis WHERE tenant_id = v_tenant
LOOP
    -- Grant provider-level access (if not already granted)
    INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
    VALUES (v_tenant, v_client_id, v_api.provider_id, 'TEST', true)
    ON CONFLICT DO NOTHING;

    -- Grant API-level access
    INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
    VALUES (v_tenant, v_client_id, v_api.id, 'TEST', true);
END LOOP;

RAISE NOTICE 'Test client created with ID: %, secret_key: test-mock-secret-key-2026', v_client_id;

END $$;
