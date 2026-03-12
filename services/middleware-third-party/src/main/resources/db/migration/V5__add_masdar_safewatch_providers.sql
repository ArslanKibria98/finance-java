-- V5: Add MASDAR and SAFEWATCH providers + their APIs, then re-grant test client access
DO $$
DECLARE
    v_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_masdar_id UUID;
    v_safewatch_id UUID;
    v_client_id UUID;
    v_api RECORD;
BEGIN

-- ===================== MASDAR PROVIDER =====================
INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, base_url_dev, base_url_prod, auth_type, status, version)
VALUES (
    gen_random_uuid(), v_tenant, 'MASDAR', 'Masdar',
    'Masdar data provider for individual and employment information',
    'INCOME_VERIFICATION',
    'https://api-sandbox.masdr.sa', 'https://api.masdr.sa',
    'OAUTH2', 'ACTIVE', 1
)
RETURNING id INTO v_masdar_id;

-- MASDAR APIs
INSERT INTO provider_apis (id, tenant_id, provider_id, code, name, description, http_method, endpoint_path, status, version) VALUES
(gen_random_uuid(), v_tenant, v_masdar_id, 'MASDAR_AUTH', 'Masdar Auth', 'Masdar OAuth2 authentication', 'POST', '/oauth/token', 'ACTIVE', 1),
(gen_random_uuid(), v_tenant, v_masdar_id, 'MASDAR_INDIVIDUAL_PLUS', 'Masdar Individual Plus', 'Masdar individual plus data lookup', 'POST', '/api/v1/individual-plus', 'ACTIVE', 1);

-- ===================== SAFEWATCH PROVIDER =====================
INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, base_url_dev, base_url_prod, auth_type, status, version)
VALUES (
    gen_random_uuid(), v_tenant, 'SAFEWATCH', 'SafeWatch',
    'SafeWatch sanctions, PEP, and adverse media screening',
    'AML_SCREENING',
    'https://api-sandbox.safewatch.com', 'https://api.safewatch.com',
    'API_KEY', 'ACTIVE', 1
)
RETURNING id INTO v_safewatch_id;

-- SAFEWATCH APIs
INSERT INTO provider_apis (id, tenant_id, provider_id, code, name, description, http_method, endpoint_path, status, version) VALUES
(gen_random_uuid(), v_tenant, v_safewatch_id, 'SAFEWATCH_SCAN_SESSION', 'SafeWatch Scan Session', 'Create a new screening scan session', 'POST', '/api/v1/scan', 'ACTIVE', 1),
(gen_random_uuid(), v_tenant, v_safewatch_id, 'SAFEWATCH_SCAN_DETAILS', 'SafeWatch Scan Details', 'Get scan session results and details', 'GET', '/api/v1/scan/{sessionId}', 'ACTIVE', 1);

-- ===================== RE-GRANT TEST CLIENT ACCESS TO NEW APIs =====================
SELECT id INTO v_client_id FROM api_clients WHERE code = 'TEST_MOCK_CLIENT' AND tenant_id = v_tenant;

IF v_client_id IS NOT NULL THEN
    -- Grant access to all NEW APIs (MASDAR + SAFEWATCH)
    FOR v_api IN
        SELECT pa.id, pa.provider_id FROM provider_apis pa
        WHERE pa.tenant_id = v_tenant
        AND pa.provider_id IN (v_masdar_id, v_safewatch_id)
    LOOP
        INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
        VALUES (v_tenant, v_client_id, v_api.provider_id, 'TEST', true)
        ON CONFLICT DO NOTHING;

        INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
        VALUES (v_tenant, v_client_id, v_api.id, 'TEST', true)
        ON CONFLICT DO NOTHING;
    END LOOP;

    RAISE NOTICE 'Test client granted access to MASDAR and SAFEWATCH APIs';
END IF;

END $$;
