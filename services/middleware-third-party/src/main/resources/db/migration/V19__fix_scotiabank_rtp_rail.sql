-- =====================================================================
-- V19: Scotiabank Real-Time Payments (RTP) rail — correct the V16 setup
-- ---------------------------------------------------------------------
-- V16 registered SCOTIABANK with 7 RTP APIs, but the real product
-- (developer.api.scotiabank.com — real-time-payments/v1-0-17, INTERAC
-- e-Transfer* for Business) exposes only 5 endpoints. Two of the V16
-- entries (PAYMENT_VALIDATE /v1/payments/validate and PAYMENT_CREATE
-- POST /v1/payments) do not exist in this product → deactivated here.
--
-- The 5 real endpoints + their role in the flow:
--   1. SCOTIABANK_PAYMENT_OPTIONS_INQUIRY  POST /v1/payment-options/inquiry
--        Eligibility: is the creditor (alias e-mail/mobile or account no.)
--        reachable via Autodeposit / Real-time Account Deposit.   (x-api-key)
--   2. SCOTIABANK_PAYMENT_COMMIT           POST /v1/payments/secure/commit-transaction
--        Execute (send) the payment. THE money-movement call.  (detached x-jws-signature)
--   3. SCOTIABANK_PAYMENT_SUMMARY          GET  /v1/payments/{id}/summary
--        Status of a payment; {id} = Scotia payment_id OR caller's
--        message_identification, selected by the PaymentIdSource header. (x-api-key)
--   4. SCOTIABANK_PAYMENT_DETAILS          GET  /v1/payments/{payment-id}
--        Full FI-to-FI payment details.                          (x-api-key)
--   5. SCOTIABANK_PAYMENT_CANCEL           POST /v1/payments/{payment-id}/cancel
--        Cancel an unclaimed INTERAC e-Transfer.                 (x-api-key)
--
-- MANDATORY flow (send money):   1 (options) -> 2 (commit) -> 3 (summary, poll)
-- OPTIONAL:                      4 (details, audit)   5 (cancel, if unclaimed)
--
-- Environment routing (ExecuteApiService, by calling client's env):
--   TEST -> local ScotiaBankMockProvider     -> client_request_test  (works now)
--   DEV  -> credentials.mockMode=true        -> client_request_dev   (local mock;
--           flip mockMode off + set base_url + customer-profile-id + jwsPrivateKey
--           via the Third Party Management panel once Scotia RTP sandbox creds exist)
--   PROD -> https://api.scotiabank.com        -> client_request_prod (creds TBD via panel)
--
-- Dynamic header tokens resolved per-request by ExecuteApiService:
--   {{TRACE_ID}} / {{SPAN_ID}} -> 16-hex B3 ids
--   {{JWS}}                    -> detached RS256 JWS over the request body,
--                                 signed with credentials.jwsPrivateKey (PKCS#8 PEM)
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
    v_test_cli   UUID;
    v_pay_cli    UUID;
    -- Scotia documented sample detached JWS (DEV/sandbox only; mock does not verify).
    v_dev_jws    TEXT := 'eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..nM45URPbMU1Knxnc9g1msv7Gl3KLrdbDhthVI6dxtlhFHmARu8pgshu1CCP1lEIExminwlNfZ-n-myGHe0BEHWZf-B2kFjsEOmqCAeUsx_iJ68JRB3cJ1QfrIh4mOhVN3i83L76eD9P8c1HF1FSfm1sE5iOPJOdfZJuK7LxST3e5pIpqW';
    v_dev_profile TEXT := 'd9ff0bb9-5d6a-4b76-a2ac-29ac4378a11a';  -- sandbox customer-profile-id
    v_dev_apikey  TEXT := 'mfZL3qmlPgjWr1234iy3uBG44FQmGabc';        -- sandbox x-api-key
BEGIN
    SELECT id INTO v_scotia_id FROM third_party_providers
        WHERE code = 'SCOTIABANK' AND tenant_id = v_tenant;
    IF v_scotia_id IS NULL THEN
        RAISE NOTICE 'SCOTIABANK provider missing (V16 not applied) — skipping RTP fix';
        RETURN;
    END IF;

    SELECT id INTO v_options_id FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_OPTIONS_INQUIRY' AND tenant_id=v_tenant;
    SELECT id INTO v_commit_id  FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_COMMIT'          AND tenant_id=v_tenant;
    SELECT id INTO v_summary_id FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_SUMMARY'         AND tenant_id=v_tenant;
    SELECT id INTO v_details_id FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_DETAILS'         AND tenant_id=v_tenant;
    SELECT id INTO v_cancel_id  FROM provider_apis WHERE code='SCOTIABANK_PAYMENT_CANCEL'          AND tenant_id=v_tenant;

    -- Idempotent re-run guard: skip if commit config is already JWS-aware.
    IF EXISTS (SELECT 1 FROM api_environment_configs
               WHERE api_id = v_commit_id AND tenant_id = v_tenant
                 AND headers ? 'x-jws-signature') THEN
        RAISE NOTICE 'Scotiabank RTP rail already corrected — skipping';
        RETURN;
    END IF;

    -- ===================== 1. Deactivate phantom endpoints =====================
    UPDATE provider_apis SET status = 'INACTIVE'
        WHERE tenant_id = v_tenant
          AND code IN ('SCOTIABANK_PAYMENT_VALIDATE', 'SCOTIABANK_PAYMENT_CREATE');

    -- ===================== 2. Rebuild env configs for the 5 real RTP APIs ======
    DELETE FROM api_environment_configs
        WHERE tenant_id = v_tenant
          AND api_id IN (v_options_id, v_commit_id, v_summary_id, v_details_id, v_cancel_id);

    -- ---- PAYMENT OPTIONS INQUIRY (x-api-key) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_options_id, 'TEST', 'https://api.scotiabank.com', '/v1/payment-options/inquiry',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_options_id, 'DEV', 'https://developer.api.scotiabank.com/mock/RTP', '/v1/payment-options/inquiry',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey, 'mockMode','true'),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_options_id, 'PROD', 'https://api.scotiabank.com', '/v1/payment-options/inquiry',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ---- COMMIT TRANSACTION (detached JWS, no x-api-key) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_commit_id, 'TEST', 'https://api.scotiabank.com', '/v1/payments/secure/commit-transaction', NULL,
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_commit_id, 'DEV', 'https://developer.api.scotiabank.com/mock/RTP', '/v1/payments/secure/commit-transaction',
            jsonb_build_object('mockMode','true'),
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id', v_dev_profile, 'x-jws-signature', v_dev_jws,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1),
        (v_tenant, v_commit_id, 'PROD', 'https://api.scotiabank.com', '/v1/payments/secure/commit-transaction',
            jsonb_build_object('jwsPrivateKey','TBD'),
            jsonb_build_object('x-channel-id','OtherFI','x-originating-appl-code','BFCP','x-country-code','CA',
                'customer-profile-id','TBD','x-jws-signature','{{JWS}}',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'NONE', TRUE, v_created_by, 1);

    -- ---- PAYMENT SUMMARY (x-api-key + PaymentIdSource) ----
    -- {id} path param is the payment_id by default; pass PaymentIdSource=MESSAGE_IDENTIFICATION
    -- if you query by your own message_identification instead.
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_summary_id, 'TEST', 'https://api.scotiabank.com', '/v1/payments/{id}/summary',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','PaymentIdSource','PAYMENT_ID',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_summary_id, 'DEV', 'https://developer.api.scotiabank.com/mock/RTP', '/v1/payments/{id}/summary',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey, 'mockMode','true'),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,'PaymentIdSource','PAYMENT_ID',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_summary_id, 'PROD', 'https://api.scotiabank.com', '/v1/payments/{id}/summary',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD','PaymentIdSource','PAYMENT_ID',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ---- PAYMENT DETAILS (x-api-key) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_details_id, 'TEST', 'https://api.scotiabank.com', '/v1/payments/{payment-id}',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_details_id, 'DEV', 'https://developer.api.scotiabank.com/mock/RTP', '/v1/payments/{payment-id}',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey, 'mockMode','true'),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_details_id, 'PROD', 'https://api.scotiabank.com', '/v1/payments/{payment-id}',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}','Accept','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ---- CANCEL PAYMENT (x-api-key) ----
    INSERT INTO api_environment_configs
        (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by, version)
    VALUES
        (v_tenant, v_cancel_id, 'TEST', 'https://api.scotiabank.com', '/v1/payments/{payment-id}/cancel',
            jsonb_build_object('headerName','x-api-key','apiKey','mock-scotia-api-key'),
            jsonb_build_object('x-country-code','CA','x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_cancel_id, 'DEV', 'https://developer.api.scotiabank.com/mock/RTP', '/v1/payments/{payment-id}/cancel',
            jsonb_build_object('headerName','x-api-key','apiKey', v_dev_apikey, 'mockMode','true'),
            jsonb_build_object('x-country-code','CA','customer-profile-id', v_dev_profile,
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1),
        (v_tenant, v_cancel_id, 'PROD', 'https://api.scotiabank.com', '/v1/payments/{payment-id}/cancel',
            jsonb_build_object('headerName','x-api-key','apiKey','TBD'),
            jsonb_build_object('x-country-code','CA','customer-profile-id','TBD',
                'x-b3-traceid','{{TRACE_ID}}','x-b3-spanid','{{SPAN_ID}}',
                'Accept','application/json','Content-Type','application/json'),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1);

    -- ===================== 3. Grants ==========================================
    -- TEST_MOCK_CLIENT already has all RTP api access from V16. Ensure the dedicated
    -- DEV PAYMENT_SERVICE client (created in V18) can also call the 5 RTP endpoints.
    SELECT id INTO v_test_cli FROM api_clients WHERE code = 'TEST_MOCK_CLIENT' AND tenant_id = v_tenant;
    IF v_test_cli IS NOT NULL THEN
        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        SELECT v_tenant, v_test_cli, id, 'TEST', TRUE FROM provider_apis
            WHERE provider_id = v_scotia_id AND tenant_id = v_tenant
              AND code IN ('SCOTIABANK_PAYMENT_OPTIONS_INQUIRY','SCOTIABANK_PAYMENT_COMMIT',
                           'SCOTIABANK_PAYMENT_SUMMARY','SCOTIABANK_PAYMENT_DETAILS','SCOTIABANK_PAYMENT_CANCEL')
        ON CONFLICT DO NOTHING;
    END IF;

    SELECT id INTO v_pay_cli FROM api_clients WHERE code = 'PAYMENT_SERVICE' AND tenant_id = v_tenant;
    IF v_pay_cli IS NOT NULL THEN
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_pay_cli, v_scotia_id, 'DEV', TRUE) ON CONFLICT DO NOTHING;
        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        SELECT v_tenant, v_pay_cli, id, 'DEV', TRUE FROM provider_apis
            WHERE provider_id = v_scotia_id AND tenant_id = v_tenant
              AND code IN ('SCOTIABANK_PAYMENT_OPTIONS_INQUIRY','SCOTIABANK_PAYMENT_COMMIT',
                           'SCOTIABANK_PAYMENT_SUMMARY','SCOTIABANK_PAYMENT_DETAILS','SCOTIABANK_PAYMENT_CANCEL')
        ON CONFLICT DO NOTHING;
    END IF;

    RAISE NOTICE 'Scotiabank RTP rail corrected: 5 active APIs reconfigured (commit=JWS, summary=PaymentIdSource), 2 phantom APIs deactivated, grants refreshed';
END $$;
