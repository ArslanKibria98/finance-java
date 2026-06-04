-- =====================================================================
-- V15: Twilio SMS provider (managed via Third Party Management panel)
-- ---------------------------------------------------------------------
-- Adds TWILIO as a COMMUNICATION provider with one API (TWILIO_SEND_SMS)
-- and DEV + PROD environment configs. Credentials are seeded as PLACEHOLDERS
-- on purpose — fill the real Account SID + Auth Token from the panel
-- (Environment Settings -> Providers -> TWILIO -> Select -> Env Config).
--
-- Twilio specifics:
--   * Auth        : BASIC  (username = Account SID, password = Auth Token)
--   * Content-Type: application/x-www-form-urlencoded (To/From/Body)
--   * Endpoint    : /2010-04-01/Accounts/{AccountSid}/Messages.json
--                   -> the Account SID is baked into the env-config endpoint
--                      path (panel-editable) so no path templating is needed.
-- =====================================================================
DO $$
DECLARE
    v_tenant     UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    v_twilio_id  UUID;
    v_api_id     UUID;
BEGIN
    -- Skip if already present (idempotent re-runs)
    IF EXISTS (SELECT 1 FROM third_party_providers
               WHERE code = 'TWILIO' AND tenant_id = v_tenant) THEN
        RAISE NOTICE 'TWILIO provider already exists — skipping';
        RETURN;
    END IF;

    -- 1) Provider
    INSERT INTO third_party_providers (
        id, tenant_id, code, name_en, name_ar, description_en, description_ar, category,
        base_url_dev, base_url_prod, auth_type, status, timeout_ms, retry_count, version
    ) VALUES (
        gen_random_uuid(), v_tenant, 'TWILIO', 'Twilio SMS Gateway', 'تويليو للرسائل',
        'Twilio SMS gateway — send OTP and notification text messages',
        'بوابة تويليو لإرسال الرسائل النصية ورموز التحقق',
        'COMMUNICATION',
        'https://api.twilio.com', 'https://api.twilio.com',
        'BASIC', 'ACTIVE', 30000, 3, 1
    )
    RETURNING id INTO v_twilio_id;

    -- 2) API: send an SMS message
    INSERT INTO provider_apis (
        id, tenant_id, provider_id, code, name_en, name_ar, description_en,
        http_method, endpoint_path, status, version
    ) VALUES (
        gen_random_uuid(), v_tenant, v_twilio_id, 'TWILIO_SEND_SMS',
        'Send SMS', 'إرسال رسالة نصية',
        'Send an SMS via Twilio Messages API (form-urlencoded To/From/Body)',
        'POST', '/2010-04-01/Accounts/{AccountSid}/Messages.json', 'ACTIVE', 1
    )
    RETURNING id INTO v_api_id;

    -- 3) Environment configs — DEV + PROD (placeholders; update via panel)
    INSERT INTO api_environment_configs (
        tenant_id, api_id, environment, base_url, endpoint_path,
        credentials, headers, query_params, auth_type, is_active, created_by, version
    ) VALUES
    (
        v_tenant, v_api_id, 'DEV',
        'https://api.twilio.com',
        '/2010-04-01/Accounts/AC_REPLACE_WITH_DEV_ACCOUNT_SID/Messages.json',
        jsonb_build_object(
            'username', 'AC_REPLACE_WITH_DEV_ACCOUNT_SID',
            'password', 'REPLACE_WITH_DEV_AUTH_TOKEN'
        ),
        jsonb_build_object('Content-Type', 'application/x-www-form-urlencoded'),
        '{}'::jsonb, 'BASIC', TRUE, v_created_by, 1
    ),
    (
        v_tenant, v_api_id, 'PROD',
        'https://api.twilio.com',
        '/2010-04-01/Accounts/AC_REPLACE_WITH_PROD_ACCOUNT_SID/Messages.json',
        jsonb_build_object(
            'username', 'AC_REPLACE_WITH_PROD_ACCOUNT_SID',
            'password', 'REPLACE_WITH_PROD_AUTH_TOKEN'
        ),
        jsonb_build_object('Content-Type', 'application/x-www-form-urlencoded'),
        '{}'::jsonb, 'BASIC', TRUE, v_created_by, 1
    );

    RAISE NOTICE 'TWILIO provider seeded (provider=% api=%)', v_twilio_id, v_api_id;
END $$;
