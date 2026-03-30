-- V10__add_mobile_number_to_identity.sql
-- Add mobile_number column to user_identity_mapping for returning in login-with-pin response

ALTER TABLE user_identity_mapping
    ADD COLUMN mobile_number VARCHAR(20);

COMMENT ON COLUMN user_identity_mapping.mobile_number IS 'Customer mobile number stored during onboarding registration';
