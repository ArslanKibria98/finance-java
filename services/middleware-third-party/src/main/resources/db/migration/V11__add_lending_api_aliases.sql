-- V11: Add PAYMENT_GUARD provider and API code aliases used by lending-service.
--      Lending workflow calls API codes that did not exist in the catalog
--      (PAYMENT_GUARD_CHECK, TARABUT_IBAN_VERIFY, EIGER_COMMODITY_BUY/SELL,
--       UNIFONIC_IVR_CALL, UNIFONIC_OTP_SEND/VERIFY, NAFITH_REGISTER, EMDHA_SIGN).
--      Each missing code caused middleware to return 404 → lending fell back to
--      a local mock without recording the call in client_request_test.
--
--      This migration adds:
--      1. The PAYMENT_GUARD provider (was missing entirely).
--      2. Aliases for the seven existing APIs so lending's call names resolve.
--      3. Test client access grants for all of the above.

DO $$
DECLARE
    v_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_payment_guard_id UUID;
    v_tarabut_id UUID;
    v_eiger_id UUID;
    v_unifonic_id UUID;
    v_nafith_id UUID;
    v_emdha_id UUID;
    v_client_id UUID;
    v_api RECORD;
BEGIN

-- ===================== PAYMENT_GUARD PROVIDER (NEW) =====================
SELECT id INTO v_payment_guard_id FROM third_party_providers
WHERE tenant_id = v_tenant AND code = 'PAYMENT_GUARD' AND deleted_at IS NULL;

IF v_payment_guard_id IS NULL THEN
    INSERT INTO third_party_providers (id, tenant_id, code, name_en, description_en, category, base_url_dev, base_url_prod, auth_type, status, version)
    VALUES (
        gen_random_uuid(), v_tenant, 'PAYMENT_GUARD', 'PaymentGuard',
        'PaymentGuard fraud / risk scoring on disbursement transactions',
        'AML_SCREENING',
        'https://api-sandbox.paymentguard.sa', 'https://api.paymentguard.sa',
        'API_KEY', 'ACTIVE', 1
    )
    RETURNING id INTO v_payment_guard_id;
END IF;

-- PAYMENT_GUARD APIs (codes used by lending-service)
INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, description_en, http_method, endpoint_path, status, version) VALUES
(gen_random_uuid(), v_tenant, v_payment_guard_id, 'PAYMENT_GUARD_CHECK', 'PaymentGuard Check', 'PaymentGuard pre-disbursement fraud check', 'POST', '/api/v1/check', 'ACTIVE', 1),
(gen_random_uuid(), v_tenant, v_payment_guard_id, 'PAYMENT_GUARD_ACCOUNT_REGISTRATION', 'PaymentGuard Account Registration', 'Register account with PaymentGuard', 'POST', '/api/v1/accounts', 'ACTIVE', 1)
ON CONFLICT DO NOTHING;

-- ===================== TARABUT alias: TARABUT_IBAN_VERIFY =====================
SELECT id INTO v_tarabut_id FROM third_party_providers WHERE tenant_id = v_tenant AND code = 'TARABUT';
IF v_tarabut_id IS NOT NULL THEN
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, description_en, http_method, endpoint_path, status, version) VALUES
    (gen_random_uuid(), v_tenant, v_tarabut_id, 'TARABUT_IBAN_VERIFY', 'Tarabut IBAN Verify', 'Verify IBAN ownership (alias of TARABUT_KSA_IBAN_MATCH)', 'POST', '/api/v1/iban/verify', 'ACTIVE', 1)
    ON CONFLICT DO NOTHING;
END IF;

-- ===================== EIGER aliases: EIGER_COMMODITY_BUY/SELL =====================
SELECT id INTO v_eiger_id FROM third_party_providers WHERE tenant_id = v_tenant AND code = 'EIGER';
IF v_eiger_id IS NOT NULL THEN
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, description_en, http_method, endpoint_path, status, version) VALUES
    (gen_random_uuid(), v_tenant, v_eiger_id, 'EIGER_COMMODITY_BUY', 'Eiger Commodity Buy', 'Buy commodity for Tawarruq (alias of EIGER_ORDER_PURCHASE)', 'POST', '/api/v1/commodity/buy', 'ACTIVE', 1),
    (gen_random_uuid(), v_tenant, v_eiger_id, 'EIGER_COMMODITY_SELL', 'Eiger Commodity Sell', 'Sell commodity for cash (alias of EIGER_ORDER_SALE)', 'POST', '/api/v1/commodity/sell', 'ACTIVE', 1)
    ON CONFLICT DO NOTHING;
END IF;

-- ===================== UNIFONIC aliases =====================
SELECT id INTO v_unifonic_id FROM third_party_providers WHERE tenant_id = v_tenant AND code = 'UNIFONIC';
IF v_unifonic_id IS NOT NULL THEN
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, description_en, http_method, endpoint_path, status, version) VALUES
    (gen_random_uuid(), v_tenant, v_unifonic_id, 'UNIFONIC_IVR_CALL', 'Unifonic IVR Call', 'Initiate IVR verification call (alias of UNIFONIC_IVR)', 'POST', '/api/v1/ivr/call', 'ACTIVE', 1),
    (gen_random_uuid(), v_tenant, v_unifonic_id, 'UNIFONIC_OTP_SEND', 'Unifonic OTP Send', 'Send OTP (alias of UNIFONIC_SEND_OTP)', 'POST', '/api/v1/otp/send', 'ACTIVE', 1),
    (gen_random_uuid(), v_tenant, v_unifonic_id, 'UNIFONIC_OTP_VERIFY', 'Unifonic OTP Verify', 'Verify OTP', 'POST', '/api/v1/otp/verify', 'ACTIVE', 1)
    ON CONFLICT DO NOTHING;
END IF;

-- ===================== NAFITH alias =====================
SELECT id INTO v_nafith_id FROM third_party_providers WHERE tenant_id = v_tenant AND code = 'NAFITH';
IF v_nafith_id IS NOT NULL THEN
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, description_en, http_method, endpoint_path, status, version) VALUES
    (gen_random_uuid(), v_tenant, v_nafith_id, 'NAFITH_REGISTER', 'Nafith Register', 'Register e-promissory note (alias of NAFITH_CREATE_SANAD)', 'POST', '/api/v1/promissory/register', 'ACTIVE', 1)
    ON CONFLICT DO NOTHING;
END IF;

-- ===================== EMDHA alias =====================
SELECT id INTO v_emdha_id FROM third_party_providers WHERE tenant_id = v_tenant AND code = 'EMDHA';
IF v_emdha_id IS NOT NULL THEN
    INSERT INTO provider_apis (id, tenant_id, provider_id, code, name_en, description_en, http_method, endpoint_path, status, version) VALUES
    (gen_random_uuid(), v_tenant, v_emdha_id, 'EMDHA_SIGN', 'Emdha Sign', 'Sign contract via Emdha (alias of EMDHA_SIGN_DOCUMENT)', 'POST', '/api/v1/contracts/sign', 'ACTIVE', 1),
    (gen_random_uuid(), v_tenant, v_emdha_id, 'EMDHA_GENERATE_CONTRACT', 'Emdha Generate Contract', 'Generate contract document for signing', 'POST', '/api/v1/contracts/generate', 'ACTIVE', 1)
    ON CONFLICT DO NOTHING;
END IF;

-- ===================== GRANT TEST CLIENT ACCESS TO ALL NEW/ALIAS APIs =====================
SELECT id INTO v_client_id FROM api_clients WHERE code = 'TEST_MOCK_CLIENT' AND tenant_id = v_tenant;

IF v_client_id IS NOT NULL THEN
    FOR v_api IN
        SELECT pa.id, pa.provider_id FROM provider_apis pa
        WHERE pa.tenant_id = v_tenant
          AND pa.code IN (
              'PAYMENT_GUARD_CHECK',
              'PAYMENT_GUARD_ACCOUNT_REGISTRATION',
              'TARABUT_IBAN_VERIFY',
              'EIGER_COMMODITY_BUY',
              'EIGER_COMMODITY_SELL',
              'UNIFONIC_IVR_CALL',
              'UNIFONIC_OTP_SEND',
              'UNIFONIC_OTP_VERIFY',
              'NAFITH_REGISTER',
              'EMDHA_SIGN',
              'EMDHA_GENERATE_CONTRACT'
          )
    LOOP
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_client_id, v_api.provider_id, 'TEST', true)
        ON CONFLICT DO NOTHING;

        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        VALUES (v_tenant, v_client_id, v_api.id, 'TEST', true)
        ON CONFLICT DO NOTHING;
    END LOOP;

    RAISE NOTICE 'V11: Test client granted access to lending-service API aliases (PAYMENT_GUARD + 9 aliases)';
END IF;

END $$;
