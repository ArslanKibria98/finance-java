-- =====================================================================
-- V18: Scotiabank EFT (Electronic Funds Transfer) rail — local Canada FT
-- ---------------------------------------------------------------------
-- V16 registered the SCOTIABANK provider with the RTP payment endpoints.
-- This migration adds the EFT rail used for account-number based fund
-- transfers within Canada (the standard CPA batch rail), plus account
-- validation and a dedicated DEV payment client.
--
-- Flow (validate -> create -> submit -> inquire):
--   1. SCOTIABANK_ACCOUNT_VALIDATION  POST /treasury/validation/v2/account-validation
--   2. SCOTIABANK_EFT_CREATE          POST /treasury/payments/eft/v1/payments         (x-jws-signature required)
--   3. SCOTIABANK_EFT_SUBMIT          POST /treasury/payments/eft/v1/submissions/{submissionId}
--   4. SCOTIABANK_EFT_INQUIRE         GET  /treasury/payments/eft/v1/submissions/{submissionId}
--
-- Environment routing (handled by ExecuteApiService via the calling client's env):
--   TEST -> local ScotiaBankMockProvider          -> client_request_test
--   DEV  -> Scotia hosted sandbox (developer.api…/mock) -> client_request_dev
--   PROD -> https://api.scotiabank.com (creds TBD via panel/Vault) -> client_request_prod
--
-- Dynamic header tokens resolved per-request by ExecuteApiService:
--   {{TRACE_ID}} / {{SPAN_ID}} -> 16-hex B3 trace/span ids
--   {{JWS}}                    -> detached RS256 JWS over the request body
--                                 (signed with credentials.jwsPrivateKey, PKCS#8 PEM)
-- DEV uses Scotia's documented sample x-jws-signature (mock does not verify);
-- PROD uses {{JWS}} + the real Scotia-registered signing key from the panel.
-- =====================================================================
DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    v_scotia_id  UUID;
    v_av_id      UUID;
    v_create_id  UUID;
    v_submit_id  UUID;
    v_inquire_id UUID;
    v_test_cli   UUID;
    v_pay_cli    UUID;
    -- Scotia documented sample detached JWS (DEV/sandbox only; mock does not verify).
    v_dev_jws    TEXT := 'eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..nM45URPbMU1Knxnc9g1msv7Gl3KLrdbDhthVI6dxtlhFHmARu8pgshu1CCP1lEIExminwlNfZ-n-myGHe0BEHWZf-B2kFjsEOmqCAeUsx_iJ68JRB3cJ1QfrIh4mOhVN3i83L76eD9P8c1HF1FSfm1sE5iOPJOdfZJuK7LxST3e5pIpqW';
    v_dev_profile TEXT := 'd9ff0bb9-5d6a-4b76-a2ac-29ac4378a11a';  -- EFT sandbox customer-profile-id
    v_dev_av_profile TEXT := 'f29b4777-e885-11ea-adc1-0242ac122222'; -- account-validation sandbox profile
    v_dev_apikey  TEXT := 'mfZL3qmlPgjWr1234iy3uBG44FQmGabc';        -- sandbox x-api-key
BEGIN
    SELECT id INTO v_scotia_id FROM third_party_providers
        WHERE code = 'SCOTIABANK' AND tenant_id = v_tenant;
    IF v_scotia_id IS NULL THEN
        RAISE NOTICE 'SCOTIABANK provider missing (V16 not applied) — skipping EFT migration';
        RETURN;
    END IF;

    -- Idempotent re-run guard.
    IF EXISTS (SELECT 1 FROM provider_apis
               WHERE code = 'SCOTIABANK_EFT_CREATE' AND tenant_id = v_tenant) THEN
        RAISE NOTICE 'Scotiabank EFT APIs already registered — skipping';
        RETURN;
    END IF;

    -- ===================== APIs =====================
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, name_ar, description_en,
                               http_method, endpoint_path, status, version)
    VALUES
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_ACCOUNT_VALIDATION',
            'Account Validation', 'التحقق من الحساب',
            'Validate institution code + transit + account number; returns account status, name-match and activity',
            'POST', '/treasury/validation/v2/account-validation', 'ACTIVE', 1),
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_EFT_CREATE',
            'Create EFT Payment', 'إنشاء دفعة تحويل إلكتروني',
            'Create an EFT submission with one or more payments (requires detached x-jws-signature)',
            'POST', '/treasury/payments/eft/v1/payments', 'ACTIVE', 1),
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_EFT_SUBMIT',
            'Submit EFT Payment', 'تقديم دفعة تحويل إلكتروني',
            'Submit a previously created EFT submission for processing by submission id',
            'POST', '/treasury/payments/eft/v1/submissions/{submissionId}', 'ACTIVE', 1),
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_EFT_INQUIRE',
            'Inquire EFT Payment', 'استعلام دفعة تحويل إلكتروني',
            'Inquire the status of an EFT submission by submission id',
            'GET', '/treasury/payments/eft/v1/submissions/{submissionId}', 'ACTIVE', 1);

    SELECT id INTO v_av_id      FROM provider_apis WHERE code='SCOTIABANK_ACCOUNT_VALIDATION' AND tenant_id=v_tenant;
    SELECT id INTO v_create_id  FROM provider_apis WHERE code='SCOTIABANK_EFT_CREATE'         AND tenant_id=v_tenant;
    SELECT id INTO v_submit_id  FROM provider_apis WHERE code='SCOTIABANK_EFT_SUBMIT'         AND tenant_id=v_tenant;
    SELECT id INTO v_inquire_id FROM provider_apis WHERE code='SCOTIABANK_EFT_INQUIRE'        AND tenant_id=v_tenant;

    -- ===================== ENV CONFIGS: ACCOUNT VALIDATION =====================
    -- TEST + DEV -> Scotia hosted account-validation sandbox (mock/33). PROD -> real prod base.
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_av_id, 'TEST', 'https://developer.api.scotiabank.com/mock/33',
            '/treasury/validation/v2/account-validation',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_av_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_av_id, 'DEV', 'https://developer.api.scotiabank.com/mock/33',
            '/treasury/validation/v2/account-validation',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_av_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_av_id, 'PROD', 'https://api.scotiabank.com',
            '/treasury/validation/v2/account-validation',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ===================== ENV CONFIGS: EFT CREATE (needs JWS) =====================
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_create_id, 'TEST', 'https://developer.api.scotiabank.com/mock/38',
            '/treasury/payments/eft/v1/payments', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id', v_dev_profile, 'x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_create_id, 'DEV', 'https://developer.api.scotiabank.com/mock/38',
            '/treasury/payments/eft/v1/payments', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id', v_dev_profile, 'x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_create_id, 'PROD', 'https://api.scotiabank.com',
            '/treasury/payments/eft/v1/payments',
            jsonb_build_object('jwsPrivateKey','TBD'),
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id','TBD','x-jws-signature','{{JWS}}',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1);

    -- ===================== ENV CONFIGS: EFT SUBMIT =====================
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_submit_id, 'TEST', 'https://developer.api.scotiabank.com/mock/38',
            '/treasury/payments/eft/v1/submissions/{submissionId}', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_submit_id, 'DEV', 'https://developer.api.scotiabank.com/mock/38',
            '/treasury/payments/eft/v1/submissions/{submissionId}', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_submit_id, 'PROD', 'https://api.scotiabank.com',
            '/treasury/payments/eft/v1/submissions/{submissionId}', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1);

    -- ===================== ENV CONFIGS: EFT INQUIRE =====================
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_inquire_id, 'TEST', 'https://developer.api.scotiabank.com/mock/38',
            '/treasury/payments/eft/v1/submissions/{submissionId}', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_inquire_id, 'DEV', 'https://developer.api.scotiabank.com/mock/38',
            '/treasury/payments/eft/v1/submissions/{submissionId}', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_inquire_id, 'PROD', 'https://api.scotiabank.com',
            '/treasury/payments/eft/v1/submissions/{submissionId}', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1);

    -- ===================== GRANT TEST_MOCK_CLIENT (TEST) =====================
    SELECT id INTO v_test_cli FROM api_clients
        WHERE code = 'TEST_MOCK_CLIENT' AND tenant_id = v_tenant;
    IF v_test_cli IS NOT NULL THEN
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_test_cli, v_scotia_id, 'TEST', TRUE) ON CONFLICT DO NOTHING;
        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        SELECT v_tenant, v_test_cli, id, 'TEST', TRUE FROM provider_apis
            WHERE provider_id = v_scotia_id AND tenant_id = v_tenant
              AND code IN ('SCOTIABANK_ACCOUNT_VALIDATION','SCOTIABANK_EFT_CREATE',
                           'SCOTIABANK_EFT_SUBMIT','SCOTIABANK_EFT_INQUIRE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- ===================== DEDICATED DEV PAYMENT CLIENT =====================
    -- A caller service uses this client's secret key to route DEV Scotia EFT calls.
    SELECT id INTO v_pay_cli FROM api_clients
        WHERE code = 'PAYMENT_SERVICE' AND tenant_id = v_tenant;
    IF v_pay_cli IS NULL THEN
        INSERT INTO api_clients (id, tenant_id, name, code, secret_key, status, environment, version)
        VALUES (gen_random_uuid(), v_tenant, 'Payment Service', 'PAYMENT_SERVICE',
                'payment-service-dev-secret-2026', 'ACTIVE', 'DEV', 1)
        RETURNING id INTO v_pay_cli;
    END IF;

    INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
    VALUES (v_tenant, v_pay_cli, v_scotia_id, 'DEV', TRUE) ON CONFLICT DO NOTHING;
    INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
    SELECT v_tenant, v_pay_cli, id, 'DEV', TRUE FROM provider_apis
        WHERE provider_id = v_scotia_id AND tenant_id = v_tenant
          AND code IN ('SCOTIABANK_ACCOUNT_VALIDATION','SCOTIABANK_EFT_CREATE',
                       'SCOTIABANK_EFT_SUBMIT','SCOTIABANK_EFT_INQUIRE')
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'Scotiabank EFT rail registered: 4 APIs + env configs; TEST_MOCK_CLIENT (TEST) + PAYMENT_SERVICE (DEV) granted';
END $$;
