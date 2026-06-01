-- V14: Register ONBOARDING_SERVICE as a real DEV client for onboarding-workflow-service.
-- Grants access to all Facia APIs so service-to-service Facia calls hit the real
-- vendor (TEST_MOCK_CLIENT goes through MockResponseDispatcher because its
-- environment=TEST — we need a DEV-env client to actually proxy to Facia.ai).

DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_facia_id   UUID;
    v_client_id  UUID;
    v_api_rec    RECORD;
BEGIN

-- Find Facia provider (registered in V13)
SELECT id INTO v_facia_id
FROM third_party_providers
WHERE code = 'FACIA' AND tenant_id = v_tenant AND deleted_at IS NULL;

IF v_facia_id IS NULL THEN
    RAISE EXCEPTION 'Facia provider missing — run V13 first';
END IF;

-- Upsert client (api_clients has a partial unique index on tenant_id+code WHERE
-- deleted_at IS NULL, which ON CONFLICT cannot target — do an explicit check).
SELECT id INTO v_client_id
FROM api_clients
WHERE tenant_id = v_tenant AND code = 'ONBOARDING_SERVICE' AND deleted_at IS NULL;

IF v_client_id IS NULL THEN
    INSERT INTO api_clients (
        id, tenant_id, code, name, description, secret_key,
        environment, status, version
    ) VALUES (
        gen_random_uuid(), v_tenant, 'ONBOARDING_SERVICE',
        'Onboarding Workflow Service',
        'Service-to-service client used by onboarding-workflow-service for real '
            || 'third-party calls (Facia.ai, future Tahaquq/Nafath in real DEV mode)',
        'ob-svc-mw-secret-2026-x9k4p',
        'DEV', 'ACTIVE', 1
    )
    RETURNING id INTO v_client_id;
ELSE
    UPDATE api_clients
    SET secret_key = 'ob-svc-mw-secret-2026-x9k4p',
        environment = 'DEV',
        status = 'ACTIVE',
        updated_at = NOW()
    WHERE id = v_client_id;
END IF;

-- Grant access to FACIA provider (BOTH = DEV+PROD)
INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
VALUES (v_tenant, v_client_id, v_facia_id, 'BOTH', TRUE)
ON CONFLICT (tenant_id, client_id, provider_id) DO UPDATE
    SET is_active = TRUE, environment = 'BOTH';

-- Grant access to each Facia API (BOTH)
FOR v_api_rec IN
    SELECT id FROM provider_apis
    WHERE provider_id = v_facia_id AND tenant_id = v_tenant AND deleted_at IS NULL
LOOP
    INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
    VALUES (v_tenant, v_client_id, v_api_rec.id, 'BOTH', TRUE)
    ON CONFLICT (tenant_id, client_id, api_id) DO UPDATE
        SET is_active = TRUE, environment = 'BOTH';
END LOOP;

RAISE NOTICE 'ONBOARDING_SERVICE client created with DEV env + Facia access (9 APIs)';

END $$;
