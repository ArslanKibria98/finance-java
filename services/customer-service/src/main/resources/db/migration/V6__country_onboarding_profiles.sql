-- V6__country_onboarding_profiles.sql
-- Country-specific onboarding step configuration
-- Phase 1: Table creation + Saudi Arabia (SAU) seed data

CREATE TABLE country_onboarding_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    country_code    VARCHAR(3) NOT NULL,
    step_order      INT NOT NULL,
    step_type       VARCHAR(50) NOT NULL,
    step_label      VARCHAR(100) NOT NULL,
    step_label_ar   VARCHAR(100),
    description     VARCHAR(255),
    provider_code   VARCHAR(50) NOT NULL,
    is_signal_wait  BOOLEAN NOT NULL DEFAULT false,
    timeout_minutes INT NOT NULL DEFAULT 30,
    is_required     BOOLEAN NOT NULL DEFAULT true,
    is_enabled      BOOLEAN NOT NULL DEFAULT true,
    config_json     JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    UNIQUE(tenant_id, country_code, step_order)
);

CREATE INDEX idx_cop_tenant_country ON country_onboarding_profiles(tenant_id, country_code);
CREATE INDEX idx_cop_country_enabled ON country_onboarding_profiles(country_code, is_enabled);

-- Supported countries registry
CREATE TABLE supported_countries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code    VARCHAR(3) NOT NULL UNIQUE,
    country_name    VARCHAR(100) NOT NULL,
    country_name_ar VARCHAR(100),
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'SAR',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    id_types        VARCHAR(255) NOT NULL,
    default_id_type VARCHAR(20) NOT NULL,
    kyc_providers   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==================== Seed: Supported Countries ====================

INSERT INTO supported_countries (country_code, country_name, country_name_ar, currency_code, is_active, id_types, default_id_type, kyc_providers) VALUES
('SAU', 'Saudi Arabia', 'المملكة العربية السعودية', 'SAR', true, 'NID,IQAMA', 'NID', 'TAHAKUK,UNIFONIC,NAFATH,YAKEEN,GOSI,SAMA_SANCTIONS'),
('ARE', 'United Arab Emirates', 'الإمارات العربية المتحدة', 'AED', false, 'EMIRATES_ID,PASSPORT', 'EMIRATES_ID', 'UAE_PASS,ICA,UAE_SANCTIONS'),
('PAK', 'Pakistan', 'پاکستان', 'PKR', false, 'CNIC,PASSPORT', 'CNIC', 'NADRA,NIC,JAZZCASH,PAK_SANCTIONS');

-- ==================== Seed: Saudi Arabia (SAU) Onboarding Steps ====================
-- Default tenant (used when no tenant-specific config exists)

INSERT INTO country_onboarding_profiles (tenant_id, country_code, step_order, step_type, step_label, step_label_ar, description, provider_code, is_signal_wait, timeout_minutes, is_required, config_json) VALUES
('00000000-0000-0000-0000-000000000000', 'SAU', 1, 'MOBILE_VERIFY',    'Mobile Verification',        'التحقق من الجوال',           'Verify mobile number ownership via Tahakuk',           'TAHAKUK',        false, 5,    true,  '{"api": "tahakuk/verify"}'),
('00000000-0000-0000-0000-000000000000', 'SAU', 2, 'OTP',              'OTP Verification',            'التحقق من رمز OTP',          'Verify OTP code sent to mobile',                       'UNIFONIC',       true,  10,   true,  '{"maxAttempts": 3, "otpLength": 6, "expiryMinutes": 5}'),
('00000000-0000-0000-0000-000000000000', 'SAU', 3, 'TERMS',            'Terms & Conditions',          'الشروط والأحكام',            'Review and accept terms and conditions',                'INTERNAL',       true,  1440, true,  null),
('00000000-0000-0000-0000-000000000000', 'SAU', 4, 'IDENTITY_VERIFY',  'Nafath Identity Verification','التحقق من الهوية عبر نفاذ',  'Verify identity using Nafath national digital ID',     'NAFATH',         true,  30,   true,  '{"transactionType": "BIOMETRIC"}'),
('00000000-0000-0000-0000-000000000000', 'SAU', 5, 'DATA_ENRICHMENT',  'Identity Data Enrichment',    'إثراء بيانات الهوية',        'Fetch citizen data from Yakeen (NIC)',                  'YAKEEN',         false, 5,    true,  '{"fields": "fullName,dob,address,nationality"}'),
('00000000-0000-0000-0000-000000000000', 'SAU', 6, 'SCREENING',        'Security Screening',          'الفحص الأمني',               'PEP, sanctions, and AML screening via SAMA',           'SAMA_SANCTIONS',  false, 5,    true,  '{"checkTypes": "PEP,SANCTIONS,AML"}'),
('00000000-0000-0000-0000-000000000000', 'SAU', 7, 'ADDITIONAL_INFO',  'Additional Information',      'معلومات إضافية',             'Submit employment and banking details (GOSI salary)',  'GOSI',           true,  1440, true,  '{"fetchSalary": true, "requireBank": true}'),
('00000000-0000-0000-0000-000000000000', 'SAU', 8, 'PIN_SETUP',        'Set App PIN',                 'تعيين رمز PIN',              'Create a 6-digit PIN to secure the account',           'INTERNAL',       true,  1440, true,  '{"pinLength": 6}');

-- ==================== Seed: UAE (ARE) Onboarding Steps (disabled) ====================

INSERT INTO country_onboarding_profiles (tenant_id, country_code, step_order, step_type, step_label, step_label_ar, description, provider_code, is_signal_wait, timeout_minutes, is_required, is_enabled, config_json) VALUES
('00000000-0000-0000-0000-000000000000', 'ARE', 1, 'MOBILE_VERIFY',    'Mobile Verification',         'التحقق من الجوال',           'Verify mobile via UAE Pass',                            'UAE_PASS',       false, 5,    true,  false, '{"api": "uaepass/verify"}'),
('00000000-0000-0000-0000-000000000000', 'ARE', 2, 'OTP',              'OTP Verification',            'التحقق من رمز OTP',          'UAE Pass built-in OTP',                                 'UAE_PASS_OTP',   true,  10,   true,  false, '{"maxAttempts": 3}'),
('00000000-0000-0000-0000-000000000000', 'ARE', 3, 'TERMS',            'Terms & Conditions',          'الشروط والأحكام',            'Review and accept terms',                               'INTERNAL',       true,  1440, true,  false, null),
('00000000-0000-0000-0000-000000000000', 'ARE', 4, 'IDENTITY_VERIFY',  'Emirates ID Verification',    'التحقق من هوية الإمارات',    'Verify Emirates ID',                                    'EMIRATES_ID',    true,  30,   true,  false, '{"documentType": "EMIRATES_ID"}'),
('00000000-0000-0000-0000-000000000000', 'ARE', 5, 'DATA_ENRICHMENT',  'ICA Data Enrichment',         'إثراء بيانات ICA',           'Fetch resident data from ICA',                          'ICA',            false, 5,    true,  false, '{"fields": "fullName,dob,address,visa"}'),
('00000000-0000-0000-0000-000000000000', 'ARE', 6, 'SCREENING',        'Security Screening',          'الفحص الأمني',               'UAE sanctions and AML screening',                       'UAE_SANCTIONS',  false, 5,    true,  false, '{"checkTypes": "PEP,SANCTIONS,AML"}'),
('00000000-0000-0000-0000-000000000000', 'ARE', 7, 'ADDITIONAL_INFO',  'Additional Information',      'معلومات إضافية',             'Submit employment and banking details',                 'MANUAL',         true,  1440, true,  false, '{"fetchSalary": false, "requireBank": true}'),
('00000000-0000-0000-0000-000000000000', 'ARE', 8, 'PIN_SETUP',        'Set App PIN',                 'PIN تعيين رمز',              'Create a 6-digit PIN',                                  'INTERNAL',       true,  1440, true,  false, '{"pinLength": 6}');

-- ==================== Seed: Pakistan (PAK) Onboarding Steps (disabled) ====================

INSERT INTO country_onboarding_profiles (tenant_id, country_code, step_order, step_type, step_label, step_label_ar, description, provider_code, is_signal_wait, timeout_minutes, is_required, is_enabled, config_json) VALUES
('00000000-0000-0000-0000-000000000000', 'PAK', 1, 'MOBILE_VERIFY',    'Mobile Verification',         'موبائل کی تصدیق',            'Verify mobile via JazzCash/Easypaisa',                  'JAZZCASH',       false, 5,    true,  false, '{"api": "jazzcash/verify"}'),
('00000000-0000-0000-0000-000000000000', 'PAK', 2, 'OTP',              'OTP Verification',            'OTP کی تصدیق',               'SMS OTP verification',                                  'SMS_GATEWAY',    true,  10,   true,  false, '{"maxAttempts": 3}'),
('00000000-0000-0000-0000-000000000000', 'PAK', 3, 'TERMS',            'Terms & Conditions',          'شرائط و ضوابط',              'Review and accept terms',                               'INTERNAL',       true,  1440, true,  false, null),
('00000000-0000-0000-0000-000000000000', 'PAK', 4, 'IDENTITY_VERIFY',  'CNIC Verification',           'CNIC کی تصدیق',              'Verify CNIC via NADRA',                                 'NADRA',          true,  30,   true,  false, '{"documentType": "CNIC"}'),
('00000000-0000-0000-0000-000000000000', 'PAK', 5, 'DATA_ENRICHMENT',  'NIC Data Enrichment',         'NIC ڈیٹا',                   'Fetch citizen data from NIC/NADRA',                     'NIC',            false, 5,    true,  false, '{"fields": "fullName,dob,address,fatherName"}'),
('00000000-0000-0000-0000-000000000000', 'PAK', 6, 'SCREENING',        'Security Screening',          'سیکیورٹی اسکریننگ',          'Pakistan sanctions screening',                          'PAK_SANCTIONS',  false, 5,    true,  false, '{"checkTypes": "PEP,SANCTIONS"}'),
('00000000-0000-0000-0000-000000000000', 'PAK', 7, 'ADDITIONAL_INFO',  'Additional Information',      'اضافی معلومات',              'Submit employment and banking details',                 'MANUAL',         true,  1440, true,  false, '{"fetchSalary": false, "requireBank": true}'),
('00000000-0000-0000-0000-000000000000', 'PAK', 8, 'PIN_SETUP',        'Set App PIN',                 'PIN سیٹ کریں',               'Create a 6-digit PIN',                                  'INTERNAL',       true,  1440, true,  false, '{"pinLength": 6}');
