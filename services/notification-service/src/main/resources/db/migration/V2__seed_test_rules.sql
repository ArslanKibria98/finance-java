-- ============================================================================
-- SEED DATA: TEST NOTIFICATION RULES
-- ============================================================================

-- Insert a default tenant and customer for testing (if not exist)
-- Tenant ID: 550e8400-e29b-41d4-a716-446655440000 (Mock Tenant)
-- Customer ID: d290f1ee-6c54-4b01-90e6-d701748f0851 (Mock Customer)

-- 1. Test Routing Rule: Send SMS on Payment Completed
INSERT INTO template_routing_rules (
    tenant_id, rule_code, rule_name, event_type, channel, novu_template_id, priority
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'RULE_PYMT_SMS', 
    'Payment Success SMS Reminder', 
    'PAYMENT_COMPLETED', 
    'SMS', 
    'payment-completed-sms-template', 
    'NORMAL'
);

-- 2. Test Routing Rule: Send Push Notification on Loan Approval
INSERT INTO template_routing_rules (
    tenant_id, rule_code, rule_name, event_type, channel, novu_template_id, priority
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'RULE_LOAN_PUSH', 
    'Loan Approval App Push', 
    'LOAN_APPROVED', 
    'PUSH', 
    'loan-approval-push-template', 
    'HIGH'
);

-- 3. Test Routing Rule: Send WhatsApp on Overdue (Dunning)
INSERT INTO template_routing_rules (
    tenant_id, rule_code, rule_name, event_type, channel, novu_template_id, priority
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'RULE_OVERDUE_WA', 
    'Overdue Payment WhatsApp Alert', 
    'DUNNING_STEP', 
    'WHATSAPP', 
    'overdue-wa-template', 
    'CRITICAL'
);

-- 5. Test Routing Rule: Send SMS on Loan Disbursement
INSERT INTO template_routing_rules (
    tenant_id, rule_code, rule_name, event_type, channel, novu_template_id, priority
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'RULE_DISB_SMS', 
    'Loan Disbursement Confirmation', 
    'LOAN_DISBURSED', 
    'SMS', 
    'loan-disbursed-sms-template', 
    'HIGH'
);

-- 4. Default Preferences for Mock Customer
INSERT INTO notification_preferences (
    tenant_id, customer_id, preferred_language, sms_enabled, email_enabled, push_enabled
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'd290f1ee-6c54-4b01-90e6-d701748f0851', 
    'ar', 
    TRUE, 
    TRUE, 
    TRUE
);
