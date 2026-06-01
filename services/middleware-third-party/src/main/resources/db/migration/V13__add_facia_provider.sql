-- V13: Register Facia.ai as a third-party KYC/biometrics provider.
-- Adds the provider, 9 cloud API endpoints (auth + 8 services), DEV/PROD/TEST
-- environment configs with credentials, and re-grants TEST_MOCK_CLIENT access
-- so onboarding-workflow-service can route Facia calls through the middleware.

DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    v_facia_id   UUID;
    v_client_id  UUID;
    api_rec      RECORD;
BEGIN

-- ===================== FACIA PROVIDER =====================
-- auth_type=CUSTOM because Facia uses a `client-secret` header (not OAuth2,
-- not standard Bearer). The middleware ExecuteApi flow reads headers from
-- api_environment_configs.headers JSONB and forwards them verbatim.
INSERT INTO third_party_providers (
    id, tenant_id, code, name_en, description_en, category,
    base_url_dev, base_url_prod, auth_type, status, timeout_ms, retry_count, version
) VALUES (
    gen_random_uuid(), v_tenant, 'FACIA', 'Facia.ai',
    'Facia.ai biometric KYC platform — document verification, face matching, '
        || '3D liveness, age estimation, deepfake detection, and one-shot KYC flows',
    'IDENTITY',
    'https://api.facia.ai', 'https://api.facia.ai',
    'CUSTOM', 'ACTIVE', 60000, 2, 1
)
RETURNING id INTO v_facia_id;

-- ===================== FACIA APIs (9 endpoints) =====================
INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, description_en,
                            http_method, endpoint_path, status, version)
VALUES
    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_AUTH',
        'Facia Access Token', 'Generate access token via client credentials',
        'POST', '/request-access-token', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_DOC_VERIFY',
        'Facia Document Verification',
        'OCR + authenticity check for ID, passport, or driver licence',
        'POST', '/document-verification', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_FACE_MATCH',
        'Facia Face Match',
        '1:1 face match between a selfie and a document photo',
        'POST', '/face-match', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_LIVENESS_3D',
        'Facia 3D Liveness',
        'Generate a hosted 3D liveness session URL',
        'POST', '/generate-liveness-url', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_FACE_SEARCH',
        'Facia Face Search',
        '1:N face search against an enrolled gallery',
        'POST', '/face-search', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_AGE_ESTIMATION',
        'Facia Age Estimation',
        'Estimate age from a face image',
        'POST', '/age-estimation', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_DEEPFAKE',
        'Facia Deepfake Liveness',
        'Detect deepfake / synthetic-media spoofing',
        'POST', '/deepfake-liveness', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_KYC_INTERACTIVE',
        'Facia KYC (Interactive)',
        'End-to-end KYC flow with user interaction',
        'POST', '/kyc/interactive', 'ACTIVE', 1),

    (gen_random_uuid(), v_tenant, v_facia_id, 'FACIA_KYC_NON_INTERACTIVE',
        'Facia KYC (Non-Interactive)',
        'Server-side one-shot KYC with submitted images',
        'POST', '/kyc/non-interactive', 'ACTIVE', 1);

-- ===================== ENVIRONMENT CONFIGS =====================
-- TEST + DEV use the sandbox credentials provided by Facia onboarding email.
-- PROD intentionally left with placeholders — real creds come from Vault later.
FOR api_rec IN
    SELECT id AS api_id, endpoint_path
    FROM provider_apis
    WHERE provider_id = v_facia_id AND tenant_id = v_tenant
LOOP
    -- TEST env
    INSERT INTO api_environment_configs (
        tenant_id, api_id, environment, base_url, endpoint_path,
        credentials, headers, query_params, auth_type, is_active, created_by, version
    ) VALUES (
        v_tenant, api_rec.api_id, 'TEST',
        'https://api.facia.ai',
        api_rec.endpoint_path,
        jsonb_build_object(
            'clientId', '415585bd38',
            'clientSecret', 'PBi1AeyudzSnDgNQWhnAkWVJrtLj7nohWNturfEC'
        ),
        jsonb_build_object(
            'client-secret', 'PBi1AeyudzSnDgNQWhnAkWVJrtLj7nohWNturfEC',
            'Content-Type', 'application/json',
            'Accept', 'application/json'
        ),
        '{}'::jsonb, 'CUSTOM', TRUE, v_created_by, 1
    );

    -- DEV env (same credentials as TEST for sandbox)
    INSERT INTO api_environment_configs (
        tenant_id, api_id, environment, base_url, endpoint_path,
        credentials, headers, query_params, auth_type, is_active, created_by, version
    ) VALUES (
        v_tenant, api_rec.api_id, 'DEV',
        'https://api.facia.ai',
        api_rec.endpoint_path,
        jsonb_build_object(
            'clientId', '415585bd38',
            'clientSecret', 'PBi1AeyudzSnDgNQWhnAkWVJrtLj7nohWNturfEC'
        ),
        jsonb_build_object(
            'client-secret', 'PBi1AeyudzSnDgNQWhnAkWVJrtLj7nohWNturfEC',
            'Content-Type', 'application/json',
            'Accept', 'application/json'
        ),
        '{}'::jsonb, 'CUSTOM', TRUE, v_created_by, 1
    );

    -- PROD env — placeholders, real creds injected at deploy time
    INSERT INTO api_environment_configs (
        tenant_id, api_id, environment, base_url, endpoint_path,
        credentials, headers, query_params, auth_type, is_active, created_by, version
    ) VALUES (
        v_tenant, api_rec.api_id, 'PROD',
        'https://api.facia.ai',
        api_rec.endpoint_path,
        jsonb_build_object('clientId', 'TBD', 'clientSecret', 'TBD'),
        jsonb_build_object(
            'client-secret', 'TBD',
            'Content-Type', 'application/json',
            'Accept', 'application/json'
        ),
        '{}'::jsonb, 'CUSTOM', TRUE, v_created_by, 1
    );
END LOOP;

-- ===================== GRANT TEST_MOCK_CLIENT ACCESS =====================
SELECT id INTO v_client_id
FROM api_clients
WHERE code = 'TEST_MOCK_CLIENT' AND tenant_id = v_tenant;

IF v_client_id IS NOT NULL THEN
    INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
    VALUES (v_tenant, v_client_id, v_facia_id, 'TEST', TRUE)
    ON CONFLICT DO NOTHING;

    FOR api_rec IN
        SELECT id FROM provider_apis
        WHERE provider_id = v_facia_id AND tenant_id = v_tenant
    LOOP
        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        VALUES (v_tenant, v_client_id, api_rec.id, 'TEST', TRUE)
        ON CONFLICT DO NOTHING;
    END LOOP;

    RAISE NOTICE 'Facia provider + 9 APIs registered, TEST_MOCK_CLIENT granted access';
ELSE
    RAISE NOTICE 'TEST_MOCK_CLIENT not found — Facia access not auto-granted';
END IF;

END $$;
