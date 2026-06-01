-- ============================================================================
-- CLEANUP DUPLICATE / UNWANTED ROUTING RULES
-- ----------------------------------------------------------------------------
-- Reasons:
--   1. Customer ko 2-2 notifications ja rahi thin kyunki SAME Novu workflow
--      ko 2 DB rules trigger karte the (PUSH + IN_APP rules dono same workflow
--      ko trigger kar rahe the, jiske andar already push step hai).
--   2. LOAN_APPROVED + LOAN_DISBURSED back-to-back fire hote hain workflow se;
--      customer ko sirf DISBURSED notification chahiye (APPROVED redundant).
--   3. SUSPICIOUS_ACTIVITY mein test-only duplicate rule pada hua tha.
-- ============================================================================

-- 1) Customer LOAN_APPROVED notifications fully remove (Issue 2 fix)
--    Customer ko sirf LOAN_DISBURSED notification mile, APPROVED admin-only hai.
DELETE FROM template_routing_rules
WHERE tenant_id   = '00000000-0000-0000-0000-000000000001'
  AND event_type  = 'LOAN_APPROVED'
  AND rule_code IN ('CUSTOMER_LOAN_APPROVED_PUSH', 'CUSTOMER_LOAN_APPROVED_INAPP');

-- 2) LOAN_DISBURSED customer duplicate remove (Issue 1 fix)
--    Workflow `customer-loan-disbursed` ke andar already push+sms steps hain,
--    isliye sirf ek DB rule trigger karna chahiye. PUSH rule keep,
--    SMS-channel rule delete (Novu khud SMS fan-out karega workflow ke andar).
DELETE FROM template_routing_rules
WHERE tenant_id   = '00000000-0000-0000-0000-000000000001'
  AND event_type  = 'LOAN_DISBURSED'
  AND rule_code   = 'CUSTOMER_LOAN_DISBURSED_SMS';

-- 3) Cleanup leftover test rule for SUSPICIOUS_ACTIVITY
DELETE FROM template_routing_rules
WHERE tenant_id   = '00000000-0000-0000-0000-000000000001'
  AND event_type  = 'SUSPICIOUS_ACTIVITY'
  AND rule_code   = 'ADMIN_SUSPICIOUS_TEST';
