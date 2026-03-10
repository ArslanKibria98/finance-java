-- ============================================================================
-- OTP VERIFICATIONS TABLE
-- Persists OTP state for mobile number verification during onboarding.
-- Replaces in-memory ConcurrentHashMap with database-backed storage.
-- ============================================================================

CREATE TYPE otp_status AS ENUM (
    'SENT',
    'VERIFIED',
    'EXPIRED',
    'MAX_ATTEMPTS_EXCEEDED',
    'FAILED'
);

CREATE TABLE otp_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    otp_request_id VARCHAR(100) NOT NULL UNIQUE,
    national_id VARCHAR(20) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    status otp_status NOT NULL DEFAULT 'SENT',
    verify_attempts INT NOT NULL DEFAULT 0,
    max_verify_attempts INT NOT NULL DEFAULT 3,
    resend_count INT NOT NULL DEFAULT 0,
    max_resend_count INT NOT NULL DEFAULT 3,
    failure_reason VARCHAR(100),
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_otp_request_id ON otp_verifications(otp_request_id);
CREATE INDEX idx_otp_national_id ON otp_verifications(national_id);
CREATE INDEX idx_otp_status ON otp_verifications(status) WHERE status = 'SENT';
CREATE INDEX idx_otp_expires_at ON otp_verifications(expires_at) WHERE status = 'SENT';
