-- Universal No-Code Onboarding Engine Schema

-- 1. Workflow Configurations (Country level)
CREATE TABLE workflow_configs (
    country_code VARCHAR(10) PRIMARY KEY,
    workflow_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Step Configurations
CREATE TABLE step_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(10) REFERENCES workflow_configs(country_code),
    step_name VARCHAR(100) NOT NULL,
    order_index INTEGER NOT NULL,
    api_url TEXT, -- External/Internal API to call after step submission
    api_method VARCHAR(10) DEFAULT 'POST',
    success_condition_jsonpath TEXT, -- JSONPath to validate API response
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (country_code, order_index)
);

-- 3. Field Configurations
CREATE TABLE field_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_id UUID REFERENCES step_configs(id) ON DELETE CASCADE,
    field_key VARCHAR(50) NOT NULL,
    field_label VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL, -- TEXT, DOCUMENT_SCAN, DATE, OTP, etc.
    is_pii BOOLEAN DEFAULT FALSE,
    is_mandatory BOOLEAN DEFAULT TRUE,
    validation_regex TEXT,
    action_api_url TEXT, -- Optional field-level API (e.g. Verify button)
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (step_id, field_key)
);

-- 4. Step Submissions (Dynamic JSON Store)
CREATE TABLE step_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100) NOT NULL, -- GlobalUID
    country_code VARCHAR(10) NOT NULL,
    step_id UUID NOT NULL,
    raw_data JSONB NOT NULL, -- Stores merged user_input + api_response
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_submissions_session_id ON step_submissions(session_id);
CREATE INDEX idx_submissions_country_step ON step_submissions(country_code, step_id);
CREATE INDEX idx_submissions_data ON step_submissions USING GIN (raw_data);
