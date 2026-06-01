-- ============================================================================
-- USER_LOGIN ROUTING RULE
-- Triggers a push notification (security + welcome) when a customer logs in
-- via mobile + PIN. Requires Novu FCM provider + customer FCM token registered.
-- ============================================================================

INSERT INTO template_routing_rules (
    tenant_id, rule_code, rule_name, event_type, channel, novu_template_id, priority
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'RULE_USER_LOGIN_PUSH',
    'Mobile PIN Login - Security Alert Push',
    'USER_LOGIN',
    'PUSH',
    'user-login-push-template',
    'HIGH'
);
