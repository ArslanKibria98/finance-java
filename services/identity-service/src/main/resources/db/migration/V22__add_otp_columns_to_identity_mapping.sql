-- V22: Add OTP columns to user_identity_mapping for forgot passcode flow
-- Replaces Keycloak attribute approach (Keycloak 26 Declarative User Profile blocks unknown attrs)

ALTER TABLE user_identity_mapping
    ADD COLUMN IF NOT EXISTS reset_otp        VARCHAR(10),
    ADD COLUMN IF NOT EXISTS reset_otp_expiry TIMESTAMPTZ;
