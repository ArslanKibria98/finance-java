-- =====================================================================
-- V20: Scotiabank RTP — correct endpoint paths + DEV live sandbox calls
-- ---------------------------------------------------------------------
-- Verified against the Scotia sandbox curls (mock proxy 28). Three fixes
-- over V16/V19:
--   1. Real paths are prefixed /treasury/payments/rtp  (V16/V19 used /v1/* only).
--        POST /treasury/payments/rtp/v1/payment-options/inquiry
--        POST /treasury/payments/rtp/v1/payments/secure/commit-transaction
--        POST /treasury/payments/rtp/v1/payments/{payment-id}/cancel
--        GET  /treasury/payments/rtp/v1/payments/{id}/summary
--        GET  /treasury/payments/rtp/v1/payments/{payment-id}
--   2. Every RTP call carries x-api-key (commit too — plus a blank/sample
--        x-jws-signature the sandbox does not verify). Summary's selector
--        header is `payment-id-source` (lowercase), NOT `PaymentIdSource`.
--   3. DEV is now a LIVE call to the Scotia sandbox (mock/28) with the real
--        sandbox x-api-key + customer-profile-id — mockMode removed. TEST stays
--        local mock (ScotiaBankMockProvider). PROD creds still TBD via panel.
--
-- Env routing: TEST -> local mock | DEV -> developer.api.scotiabank.com/mock/28
--              PROD -> api.scotiabank.com (creds TBD)
-- =====================================================================
DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    v_scotia_id  UUID;
    v_options_id UUID;
    v_commit_id  UUID;
    v_summary_id UUID;
    v_details_id UUID;
    v_cancel_id  UUID;
    v_dev_base   TEXT := 'https://developer.api.scotiabank.com/mock/28';
    v_prod_base  TEXT := 'https://api.scotiabank.com';
    v_dev_apikey TEXT := 'mfZL3qmlPgjWr1234iy3uBG44FQmGabc';            -- sandbox x-api-key
    v_dev_profile TEXT := 'f29b4777-e885-11ea-adc1-0242ac122222';       -- sandbox customer-profile-id
    -- Scotia documented sample detached JWS (sandbox does not verify).
    v_dev_jws    TEXT := 'eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..nM45URPbMU1Knxnc9g1msv7Gl3KLrdbDhthVI6dxtlhFHmARu8pgshu1CCP1lEIExminwlNfZ-n-myGHe0BEHWZf-B2kFjsEOmqCAeUsx_iJ68JRB3cJ1QfrIh4mOhVN3i83L76eD9P8c1HF1FSfm1sE5iOPJOdfZJuK7LxST3e5pIpqW';
BEGIN
    SELECT id INTO v_scotia_id FROM third_party_providers
        WHERE code = 'SCOTIABANK' AND tenant_id = v_tenant;
    IF v_scotia_id IS NULL THEN
        RAISE NOTICE 'SCOTIABANK provider missing — skipping RTP path fix';
        RETURN;
    END IF;

    SELECT id INTO v_options_id FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_OPTIONS_INQUIRY' AND tenant_id=v_tenant;
    SELECT id INTO v_commit_id  FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_COMMIT'          AND tenant_id=v_tenant;
    SELECT id INTO v_summary_id FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_SUMMARY'         AND tenant_id=v_tenant;
    SELECT id INTO v_details_id FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_DETAILS'         AND tenant_id=v_tenant;
    SELECT id INTO v_cancel_id  FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_CANCEL'          AND tenant_id=v_tenant;

    -- Idempotent re-run guard.
    IF EXISTS (SELECT 1 FROM provider_apis
               WHERE id = v_options_id AND endpoint_path = '/treasury/payments/rtp/v1/payment-options/inquiry') THEN
        RAISE NOTICE 'Scotiabank RTP paths already corrected — skipping';
        RETURN;
    END IF;

    -- ===================== 1. Correct provider_apis paths =====================
    UPDATE provider_apis SET endpoint_path = '/treasury/payments/rtp/v1/payment-options/inquiry'        WHERE id = v_options_id;
    UPDATE provider_apis SET endpoint_path = '/treasury/payments/rtp/v1/payments/secure/commit-transaction' WHERE id = v_commit_id;
    UPDATE provider_apis SET endpoint_path = '/treasury/payments/rtp/v1/payments/{payment-id}/cancel'    WHERE id = v_cancel_id;
    UPDATE provider_apis SET endpoint_path = '/treasury/payments/rtp/v1/payments/{id}/summary'           WHERE id = v_summary_id;
    UPDATE provider_apis SET endpoint_path = '/treasury/payments/rtp/v1/payments/{payment-id}'           WHERE id = v_details_id;

    -- ===================== 2. Rebuild env configs (TEST mock / DEV live / PROD) ==========
    DELETE FROM api_environment_configs
        WHERE tenant_id = v_tenant
          AND api_id IN (v_options_id, v_commit_id, v_summary_id, v_details_id, v_cancel_id);

    -- ---- OPTIONS INQUIRY ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_options_id, 'TEST', v_dev_base, '/treasury/payments/rtp/v1/payment-options/inquiry',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_options_id, 'DEV', v_dev_base, '/treasury/payments/rtp/v1/payment-options/inquiry',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_options_id, 'PROD', v_prod_base, '/treasury/payments/rtp/v1/payment-options/inquiry',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ---- COMMIT TRANSACTION (x-api-key + x-jws-signature) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_commit_id, 'TEST', v_dev_base, '/treasury/payments/rtp/v1/payments/secure/commit-transaction',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_commit_id, 'DEV', v_dev_base, '/treasury/payments/rtp/v1/payments/secure/commit-transaction',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_commit_id, 'PROD', v_prod_base, '/treasury/payments/rtp/v1/payments/secure/commit-transaction',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD','jwsPrivateKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-jws-signature','{{JWS}}',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ---- SUMMARY (x-api-key + payment-id-source) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_summary_id, 'TEST', v_dev_base, '/treasury/payments/rtp/v1/payments/{id}/summary',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','payment-id-source','PAYMENT_ID',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_summary_id, 'DEV', v_dev_base, '/treasury/payments/rtp/v1/payments/{id}/summary',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,'payment-id-source','PAYMENT_ID',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_summary_id, 'PROD', v_prod_base, '/treasury/payments/rtp/v1/payments/{id}/summary',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD','payment-id-source','PAYMENT_ID',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ---- DETAILS (x-api-key) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_details_id, 'TEST', v_dev_base, '/treasury/payments/rtp/v1/payments/{payment-id}',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_details_id, 'DEV', v_dev_base, '/treasury/payments/rtp/v1/payments/{payment-id}',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_details_id, 'PROD', v_prod_base, '/treasury/payments/rtp/v1/payments/{payment-id}',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ---- CANCEL (x-api-key) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_cancel_id, 'TEST', v_dev_base, '/treasury/payments/rtp/v1/payments/{payment-id}/cancel',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_cancel_id, 'DEV', v_dev_base, '/treasury/payments/rtp/v1/payments/{payment-id}/cancel',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_cancel_id, 'PROD', v_prod_base, '/treasury/payments/rtp/v1/payments/{payment-id}/cancel',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    RAISE NOTICE 'Scotiabank RTP corrected: paths -> /treasury/payments/rtp/v1/*, DEV live on mock/28, payment-id-source header';
END $$;
