-- ============================================================================
-- WALLET TRANSFER ROUTING RULES
-- Push notifications:
--   FUNDS_SENT     → sender when transfer completes
--   FUNDS_RECEIVED → receiver when funds land in their wallet
-- Requires Novu FCM provider + customer FCM token registered (set on first login).
-- ============================================================================

INSERT INTO template_routing_rules (
    tenant_id, rule_code, rule_name, event_type, channel, novu_template_id, priority
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'RULE_FUNDS_SENT_PUSH',
    'Wallet Transfer Sent - Push',
    'FUNDS_SENT',
    'PUSH',
    'funds-sent-push-template',
    'HIGH'
);

INSERT INTO template_routing_rules (
    tenant_id, rule_code, rule_name, event_type, channel, novu_template_id, priority
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'RULE_FUNDS_RECEIVED_PUSH',
    'Wallet Transfer Received - Push',
    'FUNDS_RECEIVED',
    'PUSH',
    'funds-received-push-template',
    'HIGH'
);
