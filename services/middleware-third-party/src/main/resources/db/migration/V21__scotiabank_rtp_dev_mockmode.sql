-- =====================================================================
-- V21: Scotiabank RTP — DEV served by local mock (portal proxy is gated)
-- ---------------------------------------------------------------------
-- V20 pointed DEV at the Scotia developer-portal mock proxy
-- (developer.api.scotiabank.com/mock/28). Verified server-to-server: that
-- URL returns HTTP 302 -> /session-error because the portal "Try it" proxy
-- is gated behind a logged-in browser session — it is NOT a server-to-server
-- sandbox. (A raw call with only x-api-key is redirected to login.)
--
-- Until Scotia issues real server-to-server credentials against a reachable
-- host (api.scotiabank.com / a true sandbox), DEV is served by the local
-- ScotiaBankMockProvider via credentials.mockMode=true. Paths, headers and
-- creds stay correct so going live later = remove mockMode + set the real
-- base_url/x-api-key/customer-profile-id (+ jwsPrivateKey for commit) via panel.
--
-- TEST: local mock (unchanged). PROD: api.scotiabank.com, creds TBD.
-- =====================================================================
DO $$
DECLARE
    v_tenant    UUID := '00000000-0000-0000-0000-000000000001';
    v_scotia_id UUID;
BEGIN
    SELECT id INTO v_scotia_id FROM third_party_providers
        WHERE code = 'SCOTIABANK' AND tenant_id = v_tenant;
    IF v_scotia_id IS NULL THEN
        RAISE NOTICE 'SCOTIABANK provider missing — skipping';
        RETURN;
    END IF;

    -- Add mockMode=true to the DEV credentials of all 5 RTP APIs (idempotent: jsonb || overwrites).
    UPDATE api_environment_configs ec
    SET credentials = COALESCE(ec.credentials, '{}'::jsonb) || jsonb_build_object('mockMode','true')
    FROM provider_apis pa
    WHERE ec.api_id = pa.id
      AND ec.tenant_id = v_tenant
      AND ec.environment = 'DEV'
      AND pa.provider_id = v_scotia_id
      AND pa.code IN ('SCOTIABANK_PAYMENT_OPTIONS_INQUIRY','SCOTIABANK_PAYMENT_COMMIT',
                      'SCOTIABANK_PAYMENT_SUMMARY','SCOTIABANK_PAYMENT_DETAILS','SCOTIABANK_PAYMENT_CANCEL');

    RAISE NOTICE 'Scotiabank RTP DEV set to mockMode=true (portal proxy is session-gated); flip off + set real host/creds when issued';
END $$;
