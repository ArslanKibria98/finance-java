-- Seed Workflow for Pakistan (PK)

INSERT INTO workflow_configs (country_code, workflow_name, is_active)
VALUES ('PK', 'Pakistan Generic Onboarding', TRUE)
ON CONFLICT (country_code) DO NOTHING;

-- Step 1: Mobile Verification (Generic)
DO $$
DECLARE
    step_id UUID;
BEGIN
    INSERT INTO step_configs (country_code, step_name, order_index, api_url)
    VALUES ('PK', 'Mobile OTP', 1, 'http://kyc-adapter-service:8087/api/v1/kyc/otp/send')
    RETURNING id INTO step_id;

    INSERT INTO field_configs (step_id, field_key, field_label, field_type, is_pii, order_index, validation_regex)
    VALUES 
    (step_id, 'mobileNumber', 'Mobile Number', 'PHONE_NUMBER', TRUE, 1, '^\+92\d{10}$');
END $$;

-- Step 2: CNIC Scan & OCR
DO $$
DECLARE
    step_id UUID;
BEGIN
    INSERT INTO step_configs (country_code, step_name, order_index, api_url)
    VALUES ('PK', 'Identity Verification', 2, 'http://kyc-adapter-service:8087/api/v1/kyc/pk/nadra/verify')
    RETURNING id INTO step_id;

    INSERT INTO field_configs (step_id, field_key, field_label, field_type, is_pii, order_index)
    VALUES 
    (step_id, 'cnic_front', 'Scan CNIC Front', 'DOCUMENT_SCAN', TRUE, 1),
    (step_id, 'cnic_back', 'Scan CNIC Back', 'DOCUMENT_SCAN', TRUE, 2),
    (step_id, 'cnic_number', 'CNIC Number', 'TEXT', TRUE, 3);
END $$;
