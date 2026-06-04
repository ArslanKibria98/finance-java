-- =====================================================================
-- V23: Scotiabank Wire Payments rail (one-time wire transfer)
-- ---------------------------------------------------------------------
-- Third Scotia rail (after EFT + RTP). Product: wire-payments/v1-0-4
-- (developer.api.scotiabank.com, mock proxy 40). Verified against sandbox curls.
-- Paths prefixed /treasury/payments/wire/v1. Flow: validate -> create -> inquire.
--   1. SCOTIABANK_WIRE_VALIDATE  POST /treasury/payments/wire/v1/payments/validate  (x-jws-signature)
--   2. SCOTIABANK_WIRE_CREATE    POST /treasury/payments/wire/v1/payments           (x-jws-signature)
--   3. SCOTIABANK_WIRE_INQUIRE   GET  /treasury/payments/wire/v1/payments/{payment-id}
--
-- Auth: NO x-api-key. Headers: x-channel-id=Online, x-country-code=CA,
--       customer-profile-id, x-b3-traceid/spanid; validate/create add x-jws-signature.
-- (NOTE: the V16 "phantom" SCOTIABANK_PAYMENT_VALIDATE/_CREATE were actually Wire
--  endpoints mis-grouped under RTP; left INACTIVE as legacy — Wire uses these new codes.)
--
-- Env routing: TEST -> local mock | DEV -> mockMode (portal proxy 40 is session-gated
--              -> 302, same as RTP/EFT) | PROD -> api.scotiabank.com (creds TBD via panel).
-- =====================================================================
DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    v_scotia_id  UUID;
    v_val_id     UUID;
    v_crt_id     UUID;
    v_inq_id     UUID;
    v_test_cli   UUID;
    v_eft_cli    UUID;
    v_pay_cli    UUID;
    v_dev_base   TEXT := 'https://developer.api.scotiabank.com/mock/40';
    v_prod_base  TEXT := 'https://api.scotiabank.com';
    v_dev_profile TEXT := 'f29b4777-e885-11ea-adc1-0242ac122222';
    v_dev_jws    TEXT := 'eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..nM45URPbMU1Knxnc9g1msv7Gl3KLrdbDhthVI6dxtlhFHmARu8pgshu1CCP1lEIExminwlNfZ-n-myGHe0BEHWZf-B2kFjsEOmqCAeUsx_iJ68JRB3cJ1QfrIh4mOhVN3i83L76eD9P8c1HF1FSfm1sE5iOPJOdfZJuK7LxST3e5pIpqW';
BEGIN
    SELECT id INTO v_scotia_id FROM third_party_providers
        WHERE code = 'SCOTIABANK' AND tenant_id = v_tenant;
    IF v_scotia_id IS NULL THEN
        RAISE NOTICE 'SCOTIABANK provider missing — skipping Wire migration';
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM provider_apis WHERE code='SCOTIABANK_WIRE_CREATE' AND tenant_id=v_tenant) THEN
        RAISE NOTICE 'Scotiabank Wire APIs already registered — skipping';
        RETURN;
    END IF;

    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, name_ar, description_en,
                               http_method, endpoint_path, status, version)
    VALUES
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_WIRE_VALIDATE',
            'Validate Wire Payment', 'التحقق من الحوالة',
            'Validate a wire payment prior to creation (requires detached x-jws-signature)',
            'POST', '/treasury/payments/wire/v1/payments/validate', 'ACTIVE', 1),
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_WIRE_CREATE',
            'Create Wire Payment', 'إنشاء حوالة',
            'Create a one-time wire transfer (Canada / US / international, requires x-jws-signature)',
            'POST', '/treasury/payments/wire/v1/payments', 'ACTIVE', 1),
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_WIRE_INQUIRE',
            'Inquire Wire Payment', 'استعلام الحوالة',
            'Retrieve full wire payment details by payment id',
            'GET', '/treasury/payments/wire/v1/payments/{payment-id}', 'ACTIVE', 1);

    SELECT id INTO v_val_id FROM provider_apis WHERE code='SCOTIABANK_WIRE_VALIDATE' AND tenant_id=v_tenant;
    SELECT id INTO v_crt_id FROM provider_apis WHERE code='SCOTIABANK_WIRE_CREATE'   AND tenant_id=v_tenant;
    SELECT id INTO v_inq_id FROM provider_apis WHERE code='SCOTIABANK_WIRE_INQUIRE'  AND tenant_id=v_tenant;

    -- ---- VALIDATE (x-jws-signature) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_val_id, 'TEST', v_dev_base, '/treasury/payments/wire/v1/payments/validate', NULL,
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_val_id, 'DEV', v_dev_base, '/treasury/payments/wire/v1/payments/validate',
            jsonb_build_object('mockMode','true'),
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_val_id, 'PROD', v_prod_base, '/treasury/payments/wire/v1/payments/validate',
            jsonb_build_object('jwsPrivateKey','TBD'),
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','customer-profile-id','TBD',
                'x-jws-signature','{{JWS}}',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1);

    -- ---- CREATE (x-jws-signature) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_crt_id, 'TEST', v_dev_base, '/treasury/payments/wire/v1/payments', NULL,
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_crt_id, 'DEV', v_dev_base, '/treasury/payments/wire/v1/payments',
            jsonb_build_object('mockMode','true'),
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_crt_id, 'PROD', v_prod_base, '/treasury/payments/wire/v1/payments',
            jsonb_build_object('jwsPrivateKey','TBD'),
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','customer-profile-id','TBD',
                'x-jws-signature','{{JWS}}',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1);

    -- ---- INQUIRE (no jws, no api-key) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_inq_id, 'TEST', v_dev_base, '/treasury/payments/wire/v1/payments/{payment-id}', NULL,
            jsonb_build_object('x-channel-id','Online','x-country-code','CA',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_inq_id, 'DEV', v_dev_base, '/treasury/payments/wire/v1/payments/{payment-id}',
            jsonb_build_object('mockMode','true'),
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_inq_id, 'PROD', v_prod_base, '/treasury/payments/wire/v1/payments/{payment-id}', NULL,
            jsonb_build_object('x-channel-id','Online','x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1);

    -- ===================== GRANTS =====================
    SELECT id INTO v_test_cli FROM api_clients WHERE code='TEST_MOCK_CLIENT'  AND tenant_id=v_tenant;
    SELECT id INTO v_eft_cli  FROM api_clients WHERE code='SCOTIA_EFT_CLIENT' AND tenant_id=v_tenant;
    SELECT id INTO v_pay_cli  FROM api_clients WHERE code='PAYMENT_SERVICE'   AND tenant_id=v_tenant;

    IF v_test_cli IS NOT NULL THEN
        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        SELECT v_tenant, v_test_cli, id, 'TEST', TRUE FROM provider_apis
            WHERE provider_id=v_scotia_id AND tenant_id=v_tenant
              AND code IN ('SCOTIABANK_WIRE_VALIDATE','SCOTIABANK_WIRE_CREATE','SCOTIABANK_WIRE_INQUIRE')
        ON CONFLICT DO NOTHING;
    END IF;

    IF v_eft_cli IS NOT NULL THEN
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_eft_cli, v_scotia_id, 'DEV', TRUE) ON CONFLICT DO NOTHING;
        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        SELECT v_tenant, v_eft_cli, id, 'DEV', TRUE FROM provider_apis
            WHERE provider_id=v_scotia_id AND tenant_id=v_tenant
              AND code IN ('SCOTIABANK_WIRE_VALIDATE','SCOTIABANK_WIRE_CREATE','SCOTIABANK_WIRE_INQUIRE')
        ON CONFLICT DO NOTHING;
    END IF;

    IF v_pay_cli IS NOT NULL THEN
        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        SELECT v_tenant, v_pay_cli, id, 'DEV', TRUE FROM provider_apis
            WHERE provider_id=v_scotia_id AND tenant_id=v_tenant
              AND code IN ('SCOTIABANK_WIRE_VALIDATE','SCOTIABANK_WIRE_CREATE','SCOTIABANK_WIRE_INQUIRE')
        ON CONFLICT DO NOTHING;
    END IF;

    RAISE NOTICE 'Scotiabank Wire rail registered: 3 APIs + env configs (DEV mockMode); TEST_MOCK_CLIENT/SCOTIA_EFT_CLIENT/PAYMENT_SERVICE granted';
END $$;
