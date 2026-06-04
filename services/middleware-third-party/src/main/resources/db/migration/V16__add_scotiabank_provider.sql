-- =====================================================================
-- V16: Scotiabank Real-Time Payments (RTP) provider
-- ---------------------------------------------------------------------
-- Registers SCOTIABANK as a BANKING provider with 7 payment APIs, plus
-- TEST / DEV / PROD environment configs, and grants TEST_MOCK_CLIENT
-- access so callers can route Scotia RTP calls through the middleware.
--
-- TEST environment  -> served by ScotiaBankMockProvider (canned responses).
-- DEV / PROD        -> placeholder credentials; fill the real x-api-key +
--                      customer-profile-id from the Third Party Management
--                      panel (Providers -> SCOTIABANK -> Env Config).
--
-- Scotia specifics:
--   * Auth        : API_KEY  (x-api-key header issued at onboarding)
--   * Tracing     : x-b3-traceid / x-b3-spanid headers
--   * Country     : x-country-code = CA (ISO 3166 Alpha-2, enforced)
--   * Content-Type: application/json
--   * commit-transaction additionally requires x-jws-signature (detached)
-- =====================================================================
DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    v_scotia_id  UUID;
    v_client_id  UUID;
    api_rec      RECORD;
BEGIN
    -- Idempotent re-runs
    IF EXISTS (SELECT 1 FROM third_party_providers
               WHERE code = 'SCOTIABANK' AND tenant_id = v_tenant) THEN
        RAISE NOTICE 'SCOTIABANK provider already exists — skipping';
        RETURN;
    END IF;

    -- ===================== PROVIDER =====================
    INSERT INTO third_party_providers (
        id, tenant_id, code, name_en, name_ar, description_en, description_ar, category,
        base_url_dev, base_url_prod, auth_type, status, timeout_ms, retry_count, version
    ) VALUES (
        gen_random_uuid(), v_tenant, 'SCOTIABANK',
        'Scotiabank Real-Time Payments', 'سكوشيا بنك للمدفوعات الفورية',
        'Scotiabank RTP API — payment options inquiry, payment validate/create/commit, '
            || 'cancel, summary, and FI-to-FI payment status report',
        'واجهة سكوشيا بنك للمدفوعات الفورية',
        'BANKING',
        'https://api.scotiabank.com', 'https://api.scotiabank.com',
        'API_KEY', 'ACTIVE', 30000, 2, 1
    )
    RETURNING id INTO v_scotia_id;

    -- ===================== APIs (7 endpoints) =====================
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, name_ar, description_en,
                                http_method, endpoint_path, status, version)
    VALUES
        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_PAYMENT_OPTIONS_INQUIRY',
            'Payment Options Inquiry', 'استعلام خيارات الدفع',
            'Inquire available payment options for a deposit handle',
            'POST', '/v1/payment-options/inquiry', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_PAYMENT_VALIDATE',
            'Validate Payment', 'التحقق من الدفع',
            'Validate a payment prior to creation',
            'POST', '/v1/payments/validate', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_PAYMENT_CREATE',
            'Create Payment', 'إنشاء دفعة',
            'Create a real-time payment',
            'POST', '/v1/payments', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_PAYMENT_COMMIT',
            'Commit Transaction', 'تأكيد المعاملة',
            'Securely commit a payment transaction (requires detached x-jws-signature)',
            'POST', '/v1/payments/secure/commit-transaction', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_PAYMENT_CANCEL',
            'Cancel Payment', 'إلغاء الدفع',
            'Cancel a real-time payment by reference number',
            'POST', '/v1/payments/{payment-id}/cancel', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_PAYMENT_SUMMARY',
            'Payment Summary', 'ملخص الدفع',
            'Get the real-time payment summary for a payment',
            'GET', '/v1/payments/{id}/summary', 'ACTIVE', 1),

        (gen_random_uuid(), v_tenant, v_scotia_id, 'SCOTIABANK_PAYMENT_DETAILS',
            'Payment Details', 'تفاصيل الدفع',
            'Get full FI-to-FI payment status report for a payment',
            'GET', '/v1/payments/{payment-id}', 'ACTIVE', 1);

    -- ===================== ENVIRONMENT CONFIGS (TEST / DEV / PROD) =====================
    FOR api_rec IN
        SELECT id AS api_id, endpoint_path
        FROM provider_apis
        WHERE provider_id = v_scotia_id AND tenant_id = v_tenant
    LOOP
        -- TEST env (mock — credentials irrelevant, served by ScotiaBankMockProvider)
        INSERT INTO api_environment_configs (
            tenant_id, api_id, environment, base_url, endpoint_path,
            credentials, headers, query_params, auth_type, is_active, created_by, version
        ) VALUES (
            v_tenant, api_rec.api_id, 'TEST',
            'https://api.scotiabank.com', api_rec.endpoint_path,
            jsonb_build_object('apiKey', 'mock-scotia-api-key'),
            jsonb_build_object(
                'x-api-key', 'mock-scotia-api-key',
                'x-country-code', 'CA',
                'Content-Type', 'application/json',
                'Accept', 'application/json'
            ),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1
        );

        -- DEV env (placeholders — fill via panel)
        INSERT INTO api_environment_configs (
            tenant_id, api_id, environment, base_url, endpoint_path,
            credentials, headers, query_params, auth_type, is_active, created_by, version
        ) VALUES (
            v_tenant, api_rec.api_id, 'DEV',
            'https://api.scotiabank.com', api_rec.endpoint_path,
            jsonb_build_object('apiKey', 'REPLACE_WITH_DEV_API_KEY'),
            jsonb_build_object(
                'x-api-key', 'REPLACE_WITH_DEV_API_KEY',
                'x-country-code', 'CA',
                'customer-profile-id', 'REPLACE_WITH_DEV_PROFILE_ID',
                'Content-Type', 'application/json',
                'Accept', 'application/json'
            ),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1
        );

        -- PROD env (placeholders — real creds from Vault at deploy time)
        INSERT INTO api_environment_configs (
            tenant_id, api_id, environment, base_url, endpoint_path,
            credentials, headers, query_params, auth_type, is_active, created_by, version
        ) VALUES (
            v_tenant, api_rec.api_id, 'PROD',
            'https://api.scotiabank.com', api_rec.endpoint_path,
            jsonb_build_object('apiKey', 'TBD'),
            jsonb_build_object(
                'x-api-key', 'TBD',
                'x-country-code', 'CA',
                'customer-profile-id', 'TBD',
                'Content-Type', 'application/json',
                'Accept', 'application/json'
            ),
            '{}'::jsonb, 'API_KEY', TRUE, v_created_by, 1
        );
    END LOOP;

    -- ===================== GRANT TEST_MOCK_CLIENT ACCESS =====================
    SELECT id INTO v_client_id
    FROM api_clients
    WHERE code = 'TEST_MOCK_CLIENT' AND tenant_id = v_tenant;

    IF v_client_id IS NOT NULL THEN
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_client_id, v_scotia_id, 'TEST', TRUE)
        ON CONFLICT DO NOTHING;

        FOR api_rec IN
            SELECT id FROM provider_apis
            WHERE provider_id = v_scotia_id AND tenant_id = v_tenant
        LOOP
            INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
            VALUES (v_tenant, v_client_id, api_rec.id, 'TEST', TRUE)
            ON CONFLICT DO NOTHING;
        END LOOP;

        RAISE NOTICE 'SCOTIABANK provider + 7 APIs registered, TEST_MOCK_CLIENT granted TEST access';
    ELSE
        RAISE NOTICE 'TEST_MOCK_CLIENT not found — SCOTIABANK access not auto-granted';
    END IF;

END $$;
