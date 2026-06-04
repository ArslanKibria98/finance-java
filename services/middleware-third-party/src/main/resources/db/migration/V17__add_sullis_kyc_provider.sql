-- =====================================================================
-- V17: Sullis KYC provider (replaces Facia for onboarding KYC)
-- ---------------------------------------------------------------------
-- Registers SULLIS as an IDENTITY provider with the 5-step commercial
-- KYC session flow, plus TEST / DEV / PROD environment configs, and
-- grants TEST_MOCK_CLIENT access so onboarding-workflow-service can route
-- Sullis calls through the middleware (same pattern as FACIA / SCOTIABANK).
--
-- TEST environment -> served by SullisMockProvider (canned responses).
-- DEV              -> sandbox server (http://192.168.6.51:8084) + sandbox key.
-- PROD             -> placeholder credentials; fill the real Sullis-Api-Key
--                     from the Third Party Management panel at deploy time.
--
-- Sullis specifics:
--   * Auth        : API_KEY  ->  header `Sullis-Api-Key: sk_...`
--   * Flow        : create-session -> start-attempt -> upload-document
--                   -> upload-selfie -> submit
--   * Path params : {sessionId}, {attemptId} (substituted by ExecuteApiService)
--   * Query params: document  -> ?type=&side=
--                   biometric -> ?kind=
--   * Body        : create-session is JSON; the other 4 are empty/multipart
--   * Uploads     : document + biometric are multipart/form-data (file=...)
-- =====================================================================
DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    v_sullis_id  UUID;
    v_client_id  UUID;
    api_rec      RECORD;
    -- Sandbox values supplied by Sullis onboarding (DEV/TEST only).
    v_dev_base   TEXT := 'http://192.168.6.51:8084';
    v_dev_key    TEXT := 'sk_test_1B1i0f_HvCwlkXm5E0MZmiUVWnMTIQEKhPhZwrg';
BEGIN
    -- Idempotent re-runs
    IF EXISTS (SELECT 1 FROM third_party_providers
               WHERE code = 'SULLIS' AND tenant_id = v_tenant) THEN
        RAISE NOTICE 'SULLIS provider already exists — skipping';
        RETURN;
    END IF;

    -- ===================== PROVIDER =====================
    INSERT INTO third_party_providers (
        id, tenant_id, code, name_en, name_ar, description_en, description_ar, category,
        base_url_dev, base_url_prod, auth_type, status, timeout_ms, retry_count, version
    ) VALUES (
        gen_random_uuid(), v_tenant, 'SULLIS',
        'Sullis KYC', 'سوليس للتحقق من الهوية',
        'Sullis commercial KYC platform — session-based document verification, '
            || 'OCR/MRZ extraction, face liveness, face match, and risk scoring',
        'منصة سوليس للتحقق من الهوية — التحقق من المستندات ومطابقة الوجه',
        'IDENTITY',
        v_dev_base, 'https://api.sullis.local',
        'API_KEY', 'ACTIVE', 60000, 2, 1
    )
    RETURNING id INTO v_sullis_id;

    -- ===================== APIs (5 endpoints) =====================
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, name_ar, description_en,
                                http_method, endpoint_path, status, version)
    VALUES
        (gen_random_uuid(), v_tenant, v_sullis_id, 'SULLIS_CREATE_SESSION',
            'Create KYC Session', 'إنشاء جلسة تحقق',
            'Create a KYC session (JSON body: customerReference, webhookUrl). Returns sessionId + sdkToken.',
            'POST', '/api/v1/commercial/kyc/sessions', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_sullis_id, 'SULLIS_START_ATTEMPT',
            'Start KYC Attempt', 'بدء محاولة التحقق',
            'Start a verification attempt for a session. Returns attemptId.',
            'POST', '/api/v1/commercial/kyc/sessions/{sessionId}/attempts', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_sullis_id, 'SULLIS_UPLOAD_DOCUMENT',
            'Upload Document', 'رفع المستند',
            'Upload an ID/passport image (multipart file). Query: type, side. Returns OCR-extracted fields.',
            'POST', '/api/v1/commercial/kyc/sessions/{sessionId}/attempts/{attemptId}/document', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_sullis_id, 'SULLIS_UPLOAD_SELFIE',
            'Upload Selfie', 'رفع الصورة الذاتية',
            'Upload a selfie biometric (multipart file). Query: kind=SELFIE_IMAGE.',
            'POST', '/api/v1/commercial/kyc/sessions/{sessionId}/attempts/{attemptId}/biometric', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_sullis_id, 'SULLIS_SUBMIT',
            'Submit Attempt', 'إرسال المحاولة',
            'Run the verification pipeline and return scores + outcome (APPROVED/DECLINED).',
            'POST', '/api/v1/commercial/kyc/sessions/{sessionId}/attempts/{attemptId}/submit', 'ACTIVE', 1);

    -- ===================== ENVIRONMENT CONFIGS (TEST / DEV / PROD) =====================
    FOR api_rec IN
        SELECT id AS api_id, endpoint_path
        FROM provider_apis
        WHERE provider_id = v_sullis_id AND tenant_id = v_tenant
    LOOP
        -- TEST env (mock — credentials irrelevant, served by SullisMockProvider)
        INSERT INTO api_environment_configs (
            tenant_id, api_id, environment, base_url, endpoint_path,
            credentials, headers, query_params, auth_type, is_active, created_by, version
        ) VALUES (
            v_tenant, api_rec.api_id, 'TEST',
            v_dev_base, api_rec.endpoint_path,
            jsonb_build_object('headerName', 'Sullis-Api-Key', 'apiKey', 'mock-sullis-key'),
            jsonb_build_object('Accept', 'application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1
        );

        -- DEV env (sandbox server + sandbox key)
        INSERT INTO api_environment_configs (
            tenant_id, api_id, environment, base_url, endpoint_path,
            credentials, headers, query_params, auth_type, is_active, created_by, version
        ) VALUES (
            v_tenant, api_rec.api_id, 'DEV',
            v_dev_base, api_rec.endpoint_path,
            jsonb_build_object('headerName', 'Sullis-Api-Key', 'apiKey', v_dev_key),
            jsonb_build_object('Accept', 'application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1
        );

        -- PROD env (placeholders — real key from Vault/panel at deploy time)
        INSERT INTO api_environment_configs (
            tenant_id, api_id, environment, base_url, endpoint_path,
            credentials, headers, query_params, auth_type, is_active, created_by, version
        ) VALUES (
            v_tenant, api_rec.api_id, 'PROD',
            'https://api.sullis.local', api_rec.endpoint_path,
            jsonb_build_object('headerName', 'Sullis-Api-Key', 'apiKey', 'TBD'),
            jsonb_build_object('Accept', 'application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1
        );
    END LOOP;

    -- ===================== GRANT TEST_MOCK_CLIENT ACCESS =====================
    SELECT id INTO v_client_id
    FROM api_clients
    WHERE code = 'TEST_MOCK_CLIENT' AND tenant_id = v_tenant;

    IF v_client_id IS NOT NULL THEN
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_client_id, v_sullis_id, 'TEST', TRUE)
        ON CONFLICT DO NOTHING;

        FOR api_rec IN
            SELECT id FROM provider_apis
            WHERE provider_id = v_sullis_id AND tenant_id = v_tenant
        LOOP
            INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
            VALUES (v_tenant, v_client_id, api_rec.id, 'TEST', TRUE)
            ON CONFLICT DO NOTHING;
        END LOOP;

        RAISE NOTICE 'SULLIS provider + 5 APIs registered, TEST_MOCK_CLIENT granted TEST access';
    ELSE
        RAISE NOTICE 'TEST_MOCK_CLIENT not found — SULLIS access not auto-granted';
    END IF;

    -- ===================== GRANT ONBOARDING_SERVICE ACCESS (DEV/PROD live) =====================
    -- onboarding-workflow-service calls Sullis via this DEV client so the request hits
    -- the real Sullis sandbox (env=DEV -> executeLive). TEST_MOCK_CLIENT (env=TEST) would
    -- be answered by SullisMockProvider instead.
    SELECT id INTO v_client_id
    FROM api_clients
    WHERE code = 'ONBOARDING_SERVICE' AND tenant_id = v_tenant AND deleted_at IS NULL;

    IF v_client_id IS NOT NULL THEN
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_client_id, v_sullis_id, 'BOTH', TRUE)
        ON CONFLICT (tenant_id, client_id, provider_id) DO UPDATE
            SET is_active = TRUE, environment = 'BOTH';

        FOR api_rec IN
            SELECT id FROM provider_apis
            WHERE provider_id = v_sullis_id AND tenant_id = v_tenant
        LOOP
            INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
            VALUES (v_tenant, v_client_id, api_rec.id, 'BOTH', TRUE)
            ON CONFLICT (tenant_id, client_id, api_id) DO UPDATE
                SET is_active = TRUE, environment = 'BOTH';
        END LOOP;

        RAISE NOTICE 'ONBOARDING_SERVICE granted SULLIS access (5 APIs, BOTH env)';
    ELSE
        RAISE NOTICE 'ONBOARDING_SERVICE client not found — run V14 first';
    END IF;

END $$;
