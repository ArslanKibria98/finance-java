-- =====================================================================
-- V22: Grant SCOTIA_EFT_CLIENT access to the 5 RTP APIs (DEV)
-- ---------------------------------------------------------------------
-- The Postman collection's {{scotiaDevSecret}} maps to SCOTIA_EFT_CLIENT,
-- which previously had only the EFT APIs. Grant it the RTP endpoints too
-- so the "SCOTIABANK (RTP)" Postman folder works out of the box.
-- =====================================================================
DO $$
DECLARE
    v_tenant    UUID := '00000000-0000-0000-0000-000000000001';
    v_scotia_id UUID;
    v_client_id UUID;
BEGIN
    SELECT id INTO v_scotia_id FROM third_party_providers
        WHERE code = 'SCOTIABANK' AND tenant_id = v_tenant;
    SELECT id INTO v_client_id FROM api_clients
        WHERE code = 'SCOTIA_EFT_CLIENT' AND tenant_id = v_tenant;
    IF v_scotia_id IS NULL OR v_client_id IS NULL THEN
        RAISE NOTICE 'SCOTIABANK provider or SCOTIA_EFT_CLIENT missing — skipping RTP grant';
        RETURN;
    END IF;

    INSERT INTO client_provider_access (tenant_id, client_id, provider_id, environment, is_active)
    VALUES (v_tenant, v_client_id, v_scotia_id, 'DEV', TRUE) ON CONFLICT DO NOTHING;

    INSERT INTO client_api_access (tenant_id, client_id, api_id, environment, is_active)
    SELECT v_tenant, v_client_id, id, 'DEV', TRUE FROM provider_apis
        WHERE provider_id = v_scotia_id AND tenant_id = v_tenant
          AND code IN ('SCOTIABANK_PAYMENT_OPTIONS_INQUIRY','SCOTIABANK_PAYMENT_COMMIT',
                       'SCOTIABANK_PAYMENT_SUMMARY','SCOTIABANK_PAYMENT_DETAILS','SCOTIABANK_PAYMENT_CANCEL')
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'SCOTIA_EFT_CLIENT granted DEV access to 5 RTP APIs';
END $$;
