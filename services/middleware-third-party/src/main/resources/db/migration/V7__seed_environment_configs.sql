-- ============================================================
-- V7: Seed Environment Configs for all Provider APIs
-- Creates DEV and PROD environment configs for every API
-- ============================================================

DO $$
DECLARE
    v_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_created_by UUID := '00000000-0000-0000-0000-000000000001';
    api_rec RECORD;
    v_provider_code TEXT;
    v_dev_base_url TEXT;
    v_prod_base_url TEXT;
    v_auth_type TEXT;
    v_dev_credentials JSONB;
    v_prod_credentials JSONB;
BEGIN

    FOR api_rec IN
        SELECT pa.id AS api_id, pa.code AS api_code, pa.endpoint_path,
               tp.code AS provider_code, tp.auth_type
        FROM provider_apis pa
        JOIN third_party_providers tp ON pa.provider_id = tp.id
        WHERE pa.deleted_at IS NULL AND tp.deleted_at IS NULL AND pa.tenant_id = v_tenant
        ORDER BY tp.code, pa.code
    LOOP
        v_provider_code := api_rec.provider_code;
        v_auth_type := api_rec.auth_type;

        -- Set base URLs per provider
        CASE v_provider_code
            WHEN 'SIMAH' THEN
                v_dev_base_url := 'https://sandbox.simah.com';
                v_prod_base_url := 'https://api.simah.com';
            WHEN 'UNIFONIC' THEN
                v_dev_base_url := 'https://sandbox.unifonic.com/rest';
                v_prod_base_url := 'https://api.unifonic.com/rest';
            WHEN 'TAHAQUQ' THEN
                v_dev_base_url := 'https://sandbox.tahaquq.sa';
                v_prod_base_url := 'https://api.tahaquq.sa';
            WHEN 'NAFATH' THEN
                v_dev_base_url := 'https://sandbox.nafath.sa';
                v_prod_base_url := 'https://api.nafath.sa';
            WHEN 'DAKHLI' THEN
                v_dev_base_url := 'https://sandbox.dakhli.sa';
                v_prod_base_url := 'https://api.dakhli.sa';
            WHEN 'TARABUT' THEN
                v_dev_base_url := 'https://sandbox.tarabut.com';
                v_prod_base_url := 'https://api.tarabut.com';
            WHEN 'EIGER' THEN
                v_dev_base_url := 'https://sandbox.eigertrading.com';
                v_prod_base_url := 'https://api.eigertrading.com';
            WHEN 'NAFITH' THEN
                v_dev_base_url := 'https://sandbox.nafith.sa';
                v_prod_base_url := 'https://api.nafith.sa';
            WHEN 'ABSHER' THEN
                v_dev_base_url := 'https://sandbox.absher.sa';
                v_prod_base_url := 'https://api.absher.sa';
            WHEN 'WATHQ' THEN
                v_dev_base_url := 'https://sandbox.wathq.sa';
                v_prod_base_url := 'https://api.wathq.sa';
            WHEN 'AZM' THEN
                v_dev_base_url := 'https://sandbox.azm.sa';
                v_prod_base_url := 'https://api.azm.sa';
            WHEN 'LYNK' THEN
                v_dev_base_url := 'https://sandbox.lynk.sa';
                v_prod_base_url := 'https://api.lynk.sa';
            WHEN 'LEAN' THEN
                v_dev_base_url := 'https://sandbox.leantech.me';
                v_prod_base_url := 'https://api.leantech.me';
            WHEN 'CLS' THEN
                v_dev_base_url := 'https://sandbox.cls.sa';
                v_prod_base_url := 'https://api.cls.sa';
            WHEN 'EMDHA' THEN
                v_dev_base_url := 'https://sandbox.emdha.sa';
                v_prod_base_url := 'https://api.emdha.sa';
            WHEN 'NABA' THEN
                v_dev_base_url := 'https://sandbox.naba.sa';
                v_prod_base_url := 'https://api.naba.sa';
            WHEN 'ANB' THEN
                v_dev_base_url := 'https://sandbox.anb.com.sa';
                v_prod_base_url := 'https://api.anb.com.sa';
            WHEN 'HYPERPAY' THEN
                v_dev_base_url := 'https://test.oppwa.com';
                v_prod_base_url := 'https://oppwa.com';
            WHEN 'MASDAR' THEN
                v_dev_base_url := 'https://sandbox.masdar.sa';
                v_prod_base_url := 'https://api.masdar.sa';
            WHEN 'SAFEWATCH' THEN
                v_dev_base_url := 'https://sandbox.safewatch.sa';
                v_prod_base_url := 'https://api.safewatch.sa';
            WHEN 'PAYMENT_GUARD' THEN
                v_dev_base_url := 'https://sandbox.paymentguard.sa';
                v_prod_base_url := 'https://api.paymentguard.sa';
            ELSE
                v_dev_base_url := 'https://sandbox.' || LOWER(v_provider_code) || '.sa';
                v_prod_base_url := 'https://api.' || LOWER(v_provider_code) || '.sa';
        END CASE;

        -- Set credentials based on auth type
        CASE v_auth_type
            WHEN 'BEARER' THEN
                v_dev_credentials := '{"username": "dev_user", "password": "dev_pass"}'::jsonb;
                v_prod_credentials := '{"username": "prod_user", "password": "prod_pass"}'::jsonb;
            WHEN 'API_KEY' THEN
                v_dev_credentials := ('{"apiKey": "dev_key_' || LOWER(v_provider_code) || '"}')::jsonb;
                v_prod_credentials := ('{"apiKey": "prod_key_' || LOWER(v_provider_code) || '"}')::jsonb;
            WHEN 'OAUTH2' THEN
                v_dev_credentials := ('{"clientId": "dev_client_' || LOWER(v_provider_code) || '", "clientSecret": "dev_secret_' || LOWER(v_provider_code) || '"}')::jsonb;
                v_prod_credentials := ('{"clientId": "prod_client_' || LOWER(v_provider_code) || '", "clientSecret": "prod_secret_' || LOWER(v_provider_code) || '"}')::jsonb;
            ELSE
                v_dev_credentials := '{}'::jsonb;
                v_prod_credentials := '{}'::jsonb;
        END CASE;

        -- Insert DEV environment config
        INSERT INTO api_environment_configs (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by)
        VALUES (
            v_tenant,
            api_rec.api_id,
            'DEV',
            v_dev_base_url,
            api_rec.endpoint_path,
            v_dev_credentials,
            '{"Content-Type": "application/json", "Accept": "application/json"}'::jsonb,
            '{}'::jsonb,
            v_auth_type::auth_type,
            true,
            v_created_by
        )
        ON CONFLICT (tenant_id, api_id, environment) DO NOTHING;

        -- Insert PROD environment config
        INSERT INTO api_environment_configs (tenant_id, api_id, environment, base_url, endpoint_path, credentials, headers, query_params, auth_type, is_active, created_by)
        VALUES (
            v_tenant,
            api_rec.api_id,
            'PROD',
            v_prod_base_url,
            api_rec.endpoint_path,
            v_prod_credentials,
            '{"Content-Type": "application/json", "Accept": "application/json"}'::jsonb,
            '{}'::jsonb,
            v_auth_type::auth_type,
            true,
            v_created_by
        )
        ON CONFLICT (tenant_id, api_id, environment) DO NOTHING;

    END LOOP;

    RAISE NOTICE 'Seeded environment configs for all provider APIs';
END;
$$;
