-- Adding dynamic button support to fields
ALTER TABLE field_configs 
ADD COLUMN button_label VARCHAR(100),
ADD COLUMN action_api_method VARCHAR(10) DEFAULT 'POST';

-- Update Pakistan Step 1 (Mobile OTP) to include a Verify button
UPDATE field_configs 
SET button_label = 'Send OTP',
    action_api_url = 'http://kyc-adapter-service:8087/api/v1/kyc/otp/send'
WHERE field_key = 'mobileNumber';
