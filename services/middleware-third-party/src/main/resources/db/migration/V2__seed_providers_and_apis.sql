-- ============================================================
-- V2: Seed Providers & APIs
-- Default tenant for initial setup
-- ============================================================

-- Use a variable for default tenant (will be replaced per-tenant in production)
DO $$
DECLARE
    v_tenant UUID := '00000000-0000-0000-0000-000000000001';
    v_simah UUID;
    v_unifonic UUID;
    v_tahaquq UUID;
    v_nafath UUID;
    v_dakhli UUID;
    v_tarabut UUID;
    v_eiger UUID;
    v_nafith UUID;
    v_absher UUID;
    v_wathq UUID;
    v_azm UUID;
    v_lynk UUID;
    v_lean UUID;
    v_cls UUID;
    v_emdha UUID;
    v_naba UUID;
    v_anb UUID;
    v_hyperpay UUID;
BEGIN

-- ===================== PROVIDERS (18) =====================

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'SIMAH', 'SIMAH (Saudi Credit Bureau)', 'Saudi credit bureau for consumer and commercial credit reports', 'CREDIT_BUREAU', 'BEARER', 60000, 3)
RETURNING id INTO v_simah;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'UNIFONIC', 'Unifonic', 'SMS, OTP, and IVR voice communication provider', 'COMMUNICATION', 'API_KEY', 30000, 3)
RETURNING id INTO v_unifonic;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'TAHAQUQ', 'Tahaquq (Elm)', 'Identity verification via Elm Tahaquq (ex-Yakeen Lite)', 'IDENTITY', 'API_KEY', 30000, 3)
RETURNING id INTO v_tahaquq;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'NAFATH', 'Nafath (Elm)', 'National digital identity verification', 'IDENTITY', 'API_KEY', 60000, 3)
RETURNING id INTO v_nafath;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'DAKHLI', 'Dakhli (Elm)', 'Income and salary verification via GOSI and government payslips', 'INCOME_VERIFICATION', 'API_KEY', 30000, 3)
RETURNING id INTO v_dakhli;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'TARABUT', 'Tarabut Gateway', 'Open banking gateway for KSA and Bahrain', 'OPEN_BANKING', 'OAUTH2', 60000, 3)
RETURNING id INTO v_tarabut;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'EIGER', 'Eiger Trading', 'Commodity trading platform for Tawarruq operations', 'COMMODITY_TRADING', 'OAUTH2', 60000, 3)
RETURNING id INTO v_eiger;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'NAFITH', 'Nafith (Sanad)', 'Electronic promissory notes and Sanad management', 'PROMISSORY_NOTES', 'OAUTH2', 60000, 3)
RETURNING id INTO v_nafith;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'ABSHER', 'Absher (MOI)', 'Ministry of Interior identity and address services', 'GOVERNMENT', 'API_KEY', 60000, 3)
RETURNING id INTO v_absher;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'WATHQ', 'Wathq (Elm)', 'Commercial registration verification', 'GOVERNMENT', 'API_KEY', 30000, 3)
RETURNING id INTO v_wathq;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'AZM', 'Azm', 'Financial services provider', 'FINANCIAL', 'API_KEY', 30000, 3)
RETURNING id INTO v_azm;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'LYNK', 'Lynk', 'Financial services integration', 'FINANCIAL', 'API_KEY', 30000, 3)
RETURNING id INTO v_lynk;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'LEAN', 'Lean Technologies', 'Open banking data aggregation', 'OPEN_BANKING', 'OAUTH2', 30000, 3)
RETURNING id INTO v_lean;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'CLS', 'CLS', 'Financial settlement services', 'FINANCIAL', 'API_KEY', 30000, 3)
RETURNING id INTO v_cls;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'EMDHA', 'Emdha', 'Digital document signing and verification', 'DIGITAL_SIGNING', 'API_KEY', 30000, 3)
RETURNING id INTO v_emdha;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'NABA', 'Naba', 'Notification and communication services', 'NOTIFICATIONS', 'API_KEY', 30000, 3)
RETURNING id INTO v_naba;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'ANB', 'Arab National Bank', 'Banking integration for transfers and inquiries', 'BANKING', 'OAUTH2', 60000, 3)
RETURNING id INTO v_anb;

INSERT INTO third_party_providers (id, tenant_id, code, name, description, category, auth_type, timeout_ms, retry_count) VALUES
    (gen_random_uuid(), v_tenant, 'HYPERPAY', 'HyperPay', 'Payment gateway for checkout and refunds', 'PAYMENT_GATEWAY', 'BEARER', 30000, 3)
RETURNING id INTO v_hyperpay;

-- ===================== APIS — SIMAH (28) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_simah, 'SIMAH_LOGIN', 'SIMAH Login', 'POST', '/api/v1/Identity/login', 'Authentication to SIMAH API'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_PRODUCTS', 'Get All Product Types', 'GET', '/api/v1/Lookup/lookup/getallproducts', 'Lookup product types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_NATIONALITY', 'Get All Nationality', 'GET', '/api/v1/Lookup/lookup/countrycodes', 'Lookup country codes'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_MARITAL_STATUS', 'Get All Marital Status', 'GET', '/api/v1/Lookup/lookup/maritalstatus', 'Lookup marital statuses'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_APPLICANT_TYPES', 'Get All Applicant Types', 'GET', '/api/v1/Lookup/lookup/applicanttypes', 'Lookup applicant types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_CITIES', 'Get All Cities', 'GET', '/api/v1/Lookup/lookup/cities', 'Lookup cities'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_ID_TYPES', 'Get All ID Types', 'GET', '/api/v1/Lookup/lookup/getidtypes', 'Lookup ID types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_EMPLOYER_TYPES', 'Get Employer Types', 'GET', '/api/v1/Lookup/lookup/getemployertypes', 'Lookup employer types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_CONTACT_TYPES', 'Get All Contact Types', 'GET', '/api/v1/com/CommercialLookup/lookup/contacttypes', 'Lookup contact types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_REASON_CODES', 'Get Reason Codes', 'GET', '/api/v1/Lookup/lookup/reasoncodes', 'Lookup reason codes'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_RESIDENCE_CITIES', 'Get All City Of Residence', 'GET', '/api/v1/Lookup/lookup/residencecities', 'Lookup residence cities'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_PROCESSING_TYPES', 'Get All Processing Types', 'GET', '/api/v1/Lookup/lookup/processingtypes', 'Lookup processing types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_DECLARATION_TYPES', 'Get Declaration Types', 'GET', '/api/v1/Lookup/lookup/declarationtypes', 'Lookup declaration types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_RESIDENTIAL_TYPES', 'Get All Residential Types', 'GET', '/api/v1/Lookup/lookup/residentialtypes', 'Lookup residential types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_ADDRESS_TYPES', 'Get All Address Types', 'GET', '/api/v1/Lookup/lookup/addresstypes', 'Lookup address types'),
    (v_tenant, v_simah, 'SIMAH_LOOKUP_BUSINESS_TYPES', 'Get All Business Types', 'GET', '/api/v1/Lookup/lookup/businesstypes', 'Lookup business types'),
    (v_tenant, v_simah, 'SIMAH_ENQUIRY_NEW_V2', 'Enquiry New Application V2', 'POST', '/api/v2/enquiry/consumer/newv2', 'New consumer credit enquiry v2'),
    (v_tenant, v_simah, 'SIMAH_ENQUIRY_NEW_V1', 'Enquiry New Application V1', 'POST', '/api/v2/enquiry/consumer/new', 'New consumer credit enquiry v1'),
    (v_tenant, v_simah, 'SIMAH_ENQUIRY_REVIEW', 'Enquiry Review V2', 'POST', '/api/v2/enquiry/consumer/review', 'Review consumer enquiry'),
    (v_tenant, v_simah, 'SIMAH_SALARY_CERTIFICATE', 'Salary Certificate', 'POST', '/api/v2/enquiry/consumer/salarycertificate', 'Salary certificate enquiry'),
    (v_tenant, v_simah, 'SIMAH_SALARY_CERTIFICATE_MOF', 'Salary Certificate MOF', 'POST', '/api/v3/enquiry/consumer/mof/salarycertificate', 'MOF salary certificate'),
    (v_tenant, v_simah, 'SIMAH_SCORE_CREDIT', 'Score Credit Report', 'POST', '/api/v2/enquiry/consumer/score', 'Consumer credit score v1'),
    (v_tenant, v_simah, 'SIMAH_SCORE_CREDIT_V2', 'Score Credit Report V2', 'POST', '/api/v2/enquiry/consumer/scoreV2', 'Consumer credit score v2'),
    (v_tenant, v_simah, 'SIMAH_NEGATIVE_CONSUMER', 'Negative Consumer', 'POST', '/api/v2/enquiry/consumer/negative', 'Negative consumer check'),
    (v_tenant, v_simah, 'SIMAH_MISCELLANEOUS', 'Miscellaneous Enquiry', 'POST', '/api/v2/enquiry/consumer/misc', 'Miscellaneous consumer enquiry'),
    (v_tenant, v_simah, 'SIMAH_CONSUMER_AFFORDABILITY', 'Consumer Affordability', 'POST', '/api/v2/enquiry/consumer/affordability/save', 'Consumer affordability assessment'),
    (v_tenant, v_simah, 'SIMAH_CONSUMER_REPORT', 'Consumer Report', 'POST', '/api/v2/enquiry/consumer/new', 'Full consumer credit report'),
    (v_tenant, v_simah, 'SIMAH_CREDIT_COMMITMENTS', 'Credit Commitments', 'POST', '/api/v2/enquiry/consumer/creditcommitments', 'Consumer credit commitments');

-- ===================== APIS — UNIFONIC (3) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_unifonic, 'UNIFONIC_IVR', 'Unifonic IVR Call', 'POST', '/v1/calls', 'Initiate IVR voice call'),
    (v_tenant, v_unifonic, 'UNIFONIC_SEND_OTP', 'Send OTP SMS', 'POST', '/rest/SMS/messages', 'Send OTP via SMS'),
    (v_tenant, v_unifonic, 'UNIFONIC_IVR_STATUS', 'IVR Call Status', 'GET', '/v1/providers/voice-call-log/{phone}', 'Check IVR call status');

-- ===================== APIS — TAHAQUQ (1) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_tahaquq, 'TAHAQUQ_VERIFY_MOBILE', 'Verify Mobile Ownership', 'GET', '/api/v1/person/{nid}/owns-mobile/{mobile}', 'Verify NID owns mobile number');

-- ===================== APIS — NAFATH (4) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_nafath, 'NAFATH_INITIATE', 'Initiate Nafath Request', 'POST', '/initiate', 'Initiate Nafath identity verification'),
    (v_tenant, v_nafath, 'NAFATH_CHECK_STATUS', 'Check Nafath Status', 'POST', '/check-status', 'Check Nafath verification status'),
    (v_tenant, v_nafath, 'NAFATH_CALLBACK', 'Nafath Callback', 'POST', '/callback', 'Receive Nafath callback'),
    (v_tenant, v_nafath, 'NAFATH_GET_JWK', 'Get Nafath JWK', 'GET', '/GetJwk', 'Get Nafath JSON Web Key');

-- ===================== APIS — DAKHLI (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_dakhli, 'DAKHLI_GOSI', 'GOSI Salary Lookup', 'GET', '/gosi', 'GOSI salary and income lookup'),
    (v_tenant, v_dakhli, 'DAKHLI_GOVT', 'Government Payslip', 'GET', '/govt', 'Government payslip lookup');

-- ===================== APIS — TARABUT (20) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_tarabut, 'TARABUT_AUTH', 'Generate Token', 'POST', '/sandbox/token', 'OAuth2 token generation'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_GET_PROVIDERS', 'KSA Get Providers', 'GET', '/v1/providers', 'List KSA bank providers'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_CREATE_INTENT', 'KSA Create Intent', 'POST', '/accountInformation/v1/intent', 'Create account information intent'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_GET_INTENT', 'KSA Get Intent', 'GET', '/accountInformation/v1/intent/{intentId}', 'Get intent details'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_GET_ACCOUNTS', 'KSA Get Accounts', 'GET', '/accountInformation/v2/accounts', 'List accounts'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_GET_ACCOUNT_DETAILS', 'KSA Account Details', 'GET', '/accountInformation/v2/accounts/{accountId}', 'Get account details'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_GET_BALANCES', 'KSA Account Balances', 'GET', '/accountInformation/v2/accounts/{accountId}/balances', 'Get account balances'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_GET_TRANSACTIONS', 'KSA Account Transactions', 'GET', '/accountInformation/v2/accounts/{accountId}/transactions', 'Get account transactions'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_REVOKE_CONSENT', 'KSA Revoke Consent', 'DELETE', '/accountInformation/v1/intent/{intentId}', 'Revoke account consent'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_CATEGORIZATION', 'KSA Categorization', 'POST', '/accountInformation/v1/transactions/categorize', 'Categorize transactions'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_SALARY_CHECK', 'KSA Salary Check', 'GET', '/accountInformation/v1/salary-check/{intentId}', 'Salary verification'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_ACCOUNT_VERIFY', 'KSA Account Verification', 'POST', '/accountInformation/v1/account-verification', 'Verify account ownership'),
    (v_tenant, v_tarabut, 'TARABUT_KSA_IBAN_MATCH', 'KSA IBAN Match', 'POST', '/accountInformation/v1/iban-match', 'IBAN matching verification'),
    (v_tenant, v_tarabut, 'TARABUT_BAH_GET_PROVIDERS', 'BAH Get Providers', 'GET', '/v1/providers', 'List Bahrain bank providers'),
    (v_tenant, v_tarabut, 'TARABUT_BAH_CREATE_INTENT', 'BAH Create Intent', 'POST', '/accountInformation/v1/intent', 'Create intent (Bahrain)'),
    (v_tenant, v_tarabut, 'TARABUT_BAH_GET_ACCOUNTS', 'BAH Get Accounts', 'GET', '/accountInformation/v2/accounts', 'List accounts (Bahrain)'),
    (v_tenant, v_tarabut, 'TARABUT_BAH_GET_BALANCES', 'BAH Account Balances', 'GET', '/accountInformation/v2/accounts/{accountId}/balances', 'Get balances (Bahrain)'),
    (v_tenant, v_tarabut, 'TARABUT_BAH_GET_TRANSACTIONS', 'BAH Account Transactions', 'GET', '/accountInformation/v2/accounts/{accountId}/transactions', 'Get transactions (Bahrain)'),
    (v_tenant, v_tarabut, 'TARABUT_BAH_CATEGORIZATION', 'BAH Categorization', 'POST', '/accountInformation/v1/transactions/categorize', 'Categorize (Bahrain)'),
    (v_tenant, v_tarabut, 'TARABUT_BAH_SALARY_CHECK', 'BAH Salary Check', 'GET', '/accountInformation/v1/salary-check/{intentId}', 'Salary check (Bahrain)');

-- ===================== APIS — EIGER (6) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_eiger, 'EIGER_AUTH', 'Eiger Authentication', 'POST', '/oauth2/token', 'AWS Cognito token generation'),
    (v_tenant, v_eiger, 'EIGER_ORDER_PURCHASE', 'Create Purchase Order', 'POST', '/order/purchase', 'Create commodity purchase order'),
    (v_tenant, v_eiger, 'EIGER_ORDER_PURCHASE_UPDATE', 'Update Purchase Order', 'PATCH', '/order/purchase', 'Update purchase order status'),
    (v_tenant, v_eiger, 'EIGER_TRANSFER_NOTIFICATION', 'Transfer Notification', 'PATCH', '/transfer-notification', 'Notify transfer completion'),
    (v_tenant, v_eiger, 'EIGER_ORDER_SALE', 'Create Sale Order', 'POST', '/order/sale', 'Create commodity sale order'),
    (v_tenant, v_eiger, 'EIGER_ORDER_SALE_UPDATE', 'Update Sale Order', 'PATCH', '/order/sale', 'Update sale order status');

-- ===================== APIS — NAFITH (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_nafith, 'NAFITH_AUTH', 'Nafith Authentication', 'POST', '/oauth/token', 'OAuth token generation'),
    (v_tenant, v_nafith, 'NAFITH_CREATE_SANAD', 'Create Sanad', 'POST', '/sanad', 'Create electronic promissory note');

-- ===================== APIS — ABSHER (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_absher, 'ABSHER_VERIFY_IDENTITY', 'Verify Identity', 'POST', '/api/v1/verify-identity', 'Verify citizen identity via Absher'),
    (v_tenant, v_absher, 'ABSHER_GET_ADDRESS', 'Get National Address', 'GET', '/api/v1/address/{nid}', 'Retrieve national address');

-- ===================== APIS — WATHQ (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_wathq, 'WATHQ_VERIFY_CR', 'Verify Commercial Registration', 'GET', '/api/v1/cr/{crNumber}', 'Verify commercial registration'),
    (v_tenant, v_wathq, 'WATHQ_GET_COMPANY_INFO', 'Get Company Info', 'GET', '/api/v1/company/{crNumber}', 'Get company details');

-- ===================== APIS — AZM (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_azm, 'AZM_VERIFY', 'Azm Verification', 'POST', '/api/v1/verify', 'Azm verification service'),
    (v_tenant, v_azm, 'AZM_QUERY', 'Azm Query', 'GET', '/api/v1/query', 'Azm query service');

-- ===================== APIS — LYNK (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_lynk, 'LYNK_TRANSFER', 'Lynk Transfer', 'POST', '/api/v1/transfer', 'Initiate fund transfer'),
    (v_tenant, v_lynk, 'LYNK_BALANCE', 'Lynk Balance', 'GET', '/api/v1/balance', 'Check account balance');

-- ===================== APIS — LEAN (3) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_lean, 'LEAN_CREATE_ENTITY', 'Create Entity', 'POST', '/api/v1/entity', 'Create Lean entity'),
    (v_tenant, v_lean, 'LEAN_GET_ACCOUNTS', 'Get Accounts', 'GET', '/api/v1/accounts', 'List connected accounts'),
    (v_tenant, v_lean, 'LEAN_GET_TRANSACTIONS', 'Get Transactions', 'GET', '/api/v1/transactions', 'Get account transactions');

-- ===================== APIS — CLS (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_cls, 'CLS_SETTLE', 'CLS Settlement', 'POST', '/api/v1/settle', 'Settlement service'),
    (v_tenant, v_cls, 'CLS_QUERY', 'CLS Query', 'GET', '/api/v1/query', 'Query settlement status');

-- ===================== APIS — EMDHA (3) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_emdha, 'EMDHA_SIGN_DOCUMENT', 'Sign Document', 'POST', '/api/v1/sign', 'Digitally sign a document'),
    (v_tenant, v_emdha, 'EMDHA_VERIFY_SIGNATURE', 'Verify Signature', 'POST', '/api/v1/verify', 'Verify digital signature'),
    (v_tenant, v_emdha, 'EMDHA_GET_STATUS', 'Get Signing Status', 'GET', '/api/v1/status/{requestId}', 'Get signing request status');

-- ===================== APIS — NABA (2) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_naba, 'NABA_SEND_NOTIFICATION', 'Send Notification', 'POST', '/api/v1/send', 'Send notification'),
    (v_tenant, v_naba, 'NABA_GET_STATUS', 'Get Notification Status', 'GET', '/api/v1/status/{notificationId}', 'Get notification delivery status');

-- ===================== APIS — ANB (3) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_anb, 'ANB_AUTH', 'ANB Authentication', 'POST', '/oauth/token', 'ANB OAuth token'),
    (v_tenant, v_anb, 'ANB_TRANSFER', 'ANB Transfer', 'POST', '/api/v1/transfer', 'Initiate bank transfer'),
    (v_tenant, v_anb, 'ANB_ACCOUNT_INQUIRY', 'ANB Account Inquiry', 'GET', '/api/v1/account/{iban}', 'Account inquiry by IBAN');

-- ===================== APIS — HYPERPAY (3) =====================

INSERT INTO provider_apis (tenant_id, provider_id, code, name, http_method, endpoint_path, description) VALUES
    (v_tenant, v_hyperpay, 'HYPERPAY_CHECKOUT', 'Create Checkout', 'POST', '/v1/checkouts', 'Create payment checkout'),
    (v_tenant, v_hyperpay, 'HYPERPAY_STATUS', 'Payment Status', 'GET', '/v1/checkouts/{checkoutId}/payment', 'Get payment status'),
    (v_tenant, v_hyperpay, 'HYPERPAY_REFUND', 'Refund Payment', 'POST', '/v1/payments/{paymentId}/refund', 'Refund a payment');

END $$;
