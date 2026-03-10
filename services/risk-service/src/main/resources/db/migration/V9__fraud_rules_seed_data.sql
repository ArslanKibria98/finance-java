-- ============================================================================
-- FRAUD RULES SEED DATA — All 30 Rules from Finova Fraud Monitoring HLD v1
-- Seeds for default tenant: 00000000-0000-0000-0000-000000000001
-- ============================================================================

-- LOCATION RULES (HLD Section 5.1)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'LOC_001', 'Unusual Location Change',
 'تغيير موقع غير معتاد', 'LOCATION',
 'User logs in from a new device or unfamiliar location not previously seen. New location + New device + Distance > 200 km + Within 3 hours.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"distance_km","value":"200","dataType":"INT","description":"Minimum distance in km to trigger"},{"key":"time_window_hours","value":"3","dataType":"INT","description":"Time window in hours"},{"key":"block_duration_hours","value":"72","dataType":"INT","description":"Temporary block duration"}]',
 10),

('00000000-0000-0000-0000-000000000001', 'LOC_002', 'Sudden Location Change (Same Device)',
 'تغيير مفاجئ للموقع (نفس الجهاز)', 'LOCATION',
 'User logs in from a location significantly distant from their last login within a short timeframe, using the same device. Indicates potential GPS spoofing. Same device + Distance > 200 km + Within 1 hour.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"distance_km","value":"200","dataType":"INT","description":"Minimum distance in km"},{"key":"time_window_hours","value":"1","dataType":"INT","description":"Time window in hours"},{"key":"block_duration_hours","value":"72","dataType":"INT","description":"Temporary block duration"}]',
 11),

('00000000-0000-0000-0000-000000000001', 'LOC_003', 'National Address Mismatch on Application',
 'عدم تطابق العنوان الوطني عند التقديم', 'LOCATION',
 'Loan application submitted from device location exceeding configured distance from registered national address. Mandatory callback + ALERT.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"distance_km","value":"200","dataType":"INT","description":"Minimum distance from national address"}]',
 12);

-- DEVICE RULES (HLD Section 5.2)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'DEV_001', 'Multiple Accounts on Same Device',
 'حسابات متعددة على نفس الجهاز', 'DEVICE',
 'Multiple different customers register new accounts using identical Device ID within 48 hours. 3 accounts + Same Device ID.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_accounts","value":"3","dataType":"INT","description":"Max accounts per device"},{"key":"time_window_hours","value":"48","dataType":"INT","description":"Time window"},{"key":"escalate_to_permanent_on_repeat","value":"true","dataType":"BOOLEAN","description":"Escalate to PERMANENT on second occurrence"}]',
 20),

('00000000-0000-0000-0000-000000000001', 'DEV_002', 'Multiple Loan Applications on Same Device',
 'طلبات تمويل متعددة من نفس الجهاز', 'DEVICE',
 'Multiple different customers submit loan applications from same Device ID within 48 hours.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_customers","value":"2","dataType":"INT","description":"Max distinct customers"},{"key":"max_applications","value":"2","dataType":"INT","description":"Max applications"},{"key":"time_window_hours","value":"48","dataType":"INT","description":"Time window"}]',
 21),

('00000000-0000-0000-0000-000000000001', 'DEV_003', 'Blacklisted Device Registration or Login',
 'تسجيل أو دخول من جهاز محظور', 'DEVICE',
 'Device previously associated with confirmed fraud attempts to register or login.',
 'BLOCK', 'PERMANENT', 'ACTIVE',
 '[]',
 22);

-- GEOGRAPHIC & ACCESS CONTROL RULES (HLD Section 5.3)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'GEO_001', 'Blacklisted Country Access',
 'وصول من دولة محظورة', 'GEOGRAPHIC_ACCESS',
 'Customer attempts loan application while IP/GPS resolves to blacklisted country.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[]',
 30),

('00000000-0000-0000-0000-000000000001', 'ACC_001', 'VPN or Proxy Usage Detected',
 'اكتشاف استخدام VPN أو بروكسي', 'GEOGRAPHIC_ACCESS',
 'User accesses platform while routing traffic through VPN or proxy.',
 'BLOCK', 'SESSION', 'ACTIVE',
 '[]',
 31),

('00000000-0000-0000-0000-000000000001', 'ACC_002', 'Jailbroken or Rooted Device',
 'جهاز مكسور الحماية', 'GEOGRAPHIC_ACCESS',
 'Device shows evidence of OS-level security tampering (jailbreak on iOS or root on Android).',
 'BLOCK', 'SESSION', 'ACTIVE',
 '[]',
 32);

-- IBAN & FINANCIAL RULES (HLD Section 5.4)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'FIN_001', 'Multiple Accounts Disbursing to Same IBAN',
 'حسابات متعددة تصرف لنفس الآيبان', 'FINANCIAL',
 'Loan disbursements from 2+ different customer accounts directed to the same beneficiary IBAN.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"min_distinct_accounts","value":"2","dataType":"INT","description":"Minimum distinct accounts sharing IBAN"}]',
 40),

('00000000-0000-0000-0000-000000000001', 'FIN_002', 'Loan Amount vs Transfer Amount Mismatch',
 'عدم تطابق مبلغ القرض مع مبلغ التحويل', 'FINANCIAL',
 'Actual transfer amount does not match approved loan amount.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[]',
 41),

('00000000-0000-0000-0000-000000000001', 'FIN_003', 'Excessive Loan Applications',
 'طلبات تمويل مفرطة', 'FINANCIAL',
 'Customer submits more applications than allowed threshold within monthly or annual period.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_per_month","value":"2","dataType":"INT","description":"Max applications per month"},{"key":"max_per_year","value":"4","dataType":"INT","description":"Max applications per year"}]',
 42);

-- PAYMENT CARD RULES (HLD Section 5.5)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'CARD_001', 'International Card Used for Installment',
 'بطاقة دولية مستخدمة للقسط', 'PAYMENT_CARD',
 'Customer pays installment using card issued by foreign (non-local) bank.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"local_country","value":"SA","dataType":"STRING","description":"Local country code"}]',
 50),

('00000000-0000-0000-0000-000000000001', 'CARD_002', 'Frequent Card Changes per Account',
 'تغيير متكرر للبطاقات', 'PAYMENT_CARD',
 'Customer uses 3+ distinct cards for installment payments.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"max_distinct_cards","value":"3","dataType":"INT","description":"Max distinct cards before alert"}]',
 51),

('00000000-0000-0000-0000-000000000001', 'CARD_003', 'Cardholder Name Mismatch',
 'عدم تطابق اسم حامل البطاقة', 'PAYMENT_CARD',
 'Card name does not match registered customer name.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"use_fuzzy_match","value":"true","dataType":"BOOLEAN","description":"Use fuzzy name matching"}]',
 52);

-- TRANSACTION MONITORING RULES (HLD Section 5.6) — TMO-001 to TMO-017
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'TMO_001', 'High-Frequency Loan Applications (Velocity)',
 'طلبات تمويل عالية التكرار', 'TRANSACTION_MONITORING',
 'Same customer submits >3 applications within 24-hour rolling window.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_applications","value":"3","dataType":"INT","description":"Max applications in window"},{"key":"rolling_window_hours","value":"24","dataType":"INT","description":"Rolling window"}]',
 60),

('00000000-0000-0000-0000-000000000001', 'TMO_002', 'Application Rate Limit Breach (Monthly/Annual)',
 'تجاوز حد الطلبات الشهري/السنوي', 'TRANSACTION_MONITORING',
 'Customer exceeded max applications per month or year.',
 'BLOCK', 'APPLICATION_LEVEL', 'ACTIVE',
 '[{"key":"max_per_month","value":"2","dataType":"INT","description":"Max per month"},{"key":"max_per_year","value":"4","dataType":"INT","description":"Max per year"}]',
 61),

('00000000-0000-0000-0000-000000000001', 'TMO_003', 'Duplicate Loan Application Submission',
 'تقديم طلب تمويل مكرر', 'TRANSACTION_MONITORING',
 'Same customer + same product + same amount within 1 hour.',
 'BLOCK', 'APPLICATION_LEVEL', 'ACTIVE',
 '[{"key":"time_window_hours","value":"1","dataType":"INT","description":"Dedup window"}]',
 62),

('00000000-0000-0000-0000-000000000001', 'TMO_004', 'Late IBAN Substitution Before Disbursement',
 'تغيير الآيبان المتأخر قبل الصرف', 'TRANSACTION_MONITORING',
 'IBAN changed within 24 hours of scheduled disbursement.',
 'HOLD', 'DISBURSEMENT_LEVEL', 'ACTIVE',
 '[{"key":"min_hours_before_disbursement","value":"24","dataType":"INT","description":"Min hours before disbursement"}]',
 63),

('00000000-0000-0000-0000-000000000001', 'TMO_005', 'Multiple Disbursements to Same Beneficiary IBAN (Cross-Account)',
 'صرف متعدد لنفس الآيبان (عبر حسابات)', 'TRANSACTION_MONITORING',
 '2+ customer accounts have same beneficiary IBAN.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"min_distinct_accounts","value":"2","dataType":"INT","description":"Min accounts sharing IBAN"}]',
 64),

('00000000-0000-0000-0000-000000000001', 'TMO_006', 'Disbursement Amount Mismatch',
 'عدم تطابق مبلغ الصرف', 'TRANSACTION_MONITORING',
 'Disbursed amount differs from approved loan amount.',
 'HOLD', 'PERMANENT', 'ACTIVE',
 '[]',
 65),

('00000000-0000-0000-0000-000000000001', 'TMO_007', 'Disbursement to Unverified or Mismatched IBAN',
 'صرف لآيبان غير موثق', 'TRANSACTION_MONITORING',
 'Disbursement to IBAN that is UNVERIFIED or name mismatch.',
 'BLOCK', 'DISBURSEMENT_LEVEL', 'ACTIVE',
 '[]',
 66),

('00000000-0000-0000-0000-000000000001', 'TMO_008', 'Same-Day or Near-Immediate Full Repayment',
 'سداد كامل فوري (مؤشر غسيل أموال)', 'TRANSACTION_MONITORING',
 'Full repayment within 48 hours of disbursement — money laundering indicator.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"repayment_window_hours","value":"48","dataType":"INT","description":"Hours after disbursement"}]',
 67),

('00000000-0000-0000-0000-000000000001', 'TMO_009', 'Installment Payment from Third-Party Source',
 'دفع قسط من مصدر طرف ثالث', 'TRANSACTION_MONITORING',
 'Payment from IBAN or card not registered to borrower.',
 'HOLD', 'PAYMENT_LEVEL', 'ACTIVE',
 '[]',
 68),

('00000000-0000-0000-0000-000000000001', 'TMO_010', 'Repeated Payment Reversal or Refund Pattern',
 'نمط عكس/استرداد دفعات متكرر', 'TRANSACTION_MONITORING',
 '2+ reversals in single loan lifecycle or 3+ across multiple loans.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_reversals_per_loan","value":"2","dataType":"INT","description":"Max reversals per loan"},{"key":"max_reversals_total","value":"3","dataType":"INT","description":"Max reversals across all loans"}]',
 69),

('00000000-0000-0000-0000-000000000001', 'TMO_011', 'Installment Payment Amount Discrepancy',
 'عدم تطابق مبلغ القسط', 'TRANSACTION_MONITORING',
 'Payment amount does not match expected installment.',
 'HOLD', 'PAYMENT_LEVEL', 'ACTIVE',
 '[{"key":"tolerance_percent","value":"5","dataType":"DECIMAL","description":"Tolerance percentage (+/-)"}]',
 70),

('00000000-0000-0000-0000-000000000001', 'TMO_012', 'Dormant Account Sudden Transaction Activity',
 'نشاط مفاجئ لحساب خامل', 'TRANSACTION_MONITORING',
 'Account inactive >90 days suddenly submits loan application or login.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"dormant_days","value":"90","dataType":"INT","description":"Days of inactivity to consider dormant"}]',
 71),

('00000000-0000-0000-0000-000000000001', 'TMO_013', 'Unusual Transaction Time-of-Day',
 'وقت معاملة غير معتاد', 'TRANSACTION_MONITORING',
 'Transaction submitted between 01:00-04:00 local time — first occurrence for this account.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"unusual_hour_start","value":"1","dataType":"INT","description":"Start hour (24h)"},{"key":"unusual_hour_end","value":"4","dataType":"INT","description":"End hour (24h)"}]',
 72),

('00000000-0000-0000-0000-000000000001', 'TMO_014', 'Round-Number Transaction Pattern',
 'نمط معاملات بأرقام مدورة', 'TRANSACTION_MONITORING',
 '3+ consecutive round-number transactions from same account.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"round_denomination","value":"500","dataType":"INT","description":"Round denomination"},{"key":"min_consecutive","value":"3","dataType":"INT","description":"Min consecutive transactions"}]',
 73),

('00000000-0000-0000-0000-000000000001', 'TMO_015', 'High-Volume Proof of Indebtedness Requests',
 'طلبات شهادات مديونية مفرطة', 'TRANSACTION_MONITORING',
 '>3 proof-of-indebtedness requests within 7 days.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_requests","value":"3","dataType":"INT","description":"Max requests"},{"key":"time_window_days","value":"7","dataType":"INT","description":"Time window in days"}]',
 74),

('00000000-0000-0000-0000-000000000001', 'TMO_016', 'Early Repayment Followed by Re-Application',
 'سداد مبكر يتبعه إعادة تقديم', 'TRANSACTION_MONITORING',
 'Customer makes early full repayment then immediately re-applies for new loan — potential layering.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"re_application_window_days","value":"7","dataType":"INT","description":"Days after early repayment"}]',
 75),

('00000000-0000-0000-0000-000000000001', 'TMO_017', 'Coordinated Application Spike',
 'ارتفاع مفاجئ منسق في الطلبات', 'TRANSACTION_MONITORING',
 'Multiple applications from different customers sharing device/IBAN/location within short period — organized fraud ring.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"min_shared_applications","value":"3","dataType":"INT","description":"Min applications with shared attributes"},{"key":"time_window_hours","value":"24","dataType":"INT","description":"Time window"},{"key":"shared_attributes","value":"device_id,disbursement_iban,ip_address","dataType":"STRING","description":"Attributes to check for sharing"}]',
 76);
