-- ============================================================================
-- NOTIFICATION SERVICE DATABASE SCHEMA (NOVU ORCHESTRATOR)
-- PostgreSQL 16+ High-Fidelity Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE notification_channel AS ENUM (
    'SMS',
    'EMAIL',
    'PUSH',
    'WHATSAPP',
    'IN_APP'
);

CREATE TYPE notification_priority AS ENUM (
    'LOW',
    'NORMAL',
    'HIGH',
    'CRITICAL'
);

CREATE TYPE delivery_status AS ENUM (
    'PENDING',
    'QUEUED',
    'SENT',
    'DELIVERED',
    'READ',
    'FAILED',
    'BOUNCED',
    'UNSUBSCRIBED'
);

CREATE TYPE event_status AS ENUM (
    'RECEIVED',
    'PROCESSING',
    'DISPATCHED',
    'COMPLETED',
    'FAILED'
);

CREATE TYPE preference_level AS ENUM (
    'GLOBAL',
    'CATEGORY',
    'EVENT_TYPE'
);

-- ============================================================================
-- NOVU ORCHESTRATION TABLES
-- ============================================================================

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    preference_level preference_level NOT NULL DEFAULT 'GLOBAL',
    category VARCHAR(50),
    event_type VARCHAR(100),
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    timezone VARCHAR(50) DEFAULT 'Asia/Riyadh',
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'ar',
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_consent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_customer_preference UNIQUE (tenant_id, customer_id, preference_level, category, event_type)
);

CREATE TABLE notification_event_log (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    event_source VARCHAR(100) NOT NULL,
    event_payload JSONB NOT NULL,
    customer_id UUID,
    customer_segment VARCHAR(50),
    status event_status NOT NULL DEFAULT 'RECEIVED',
    notifications_dispatched INT NOT NULL DEFAULT 0,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(50),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_notification_event_log PRIMARY KEY (id, created_at),
    CONSTRAINT uq_event_id UNIQUE (tenant_id, event_id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE notification_event_log_2026_01 PARTITION OF notification_event_log FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE notification_event_log_2026_02 PARTITION OF notification_event_log FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE notification_event_log_2026_03 PARTITION OF notification_event_log FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE notification_event_log_2026_04 PARTITION OF notification_event_log FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE notification_event_log_2026_05 PARTITION OF notification_event_log FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE template_routing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    channel notification_channel NOT NULL,
    novu_template_id VARCHAR(100) NOT NULL,
    novu_workflow_id VARCHAR(100),
    priority notification_priority NOT NULL DEFAULT 'NORMAL',
    conditions JSONB,
    fallback_template_id VARCHAR(100),
    ab_test_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ab_variant VARCHAR(50),
    ab_weight INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_routing_rule UNIQUE (tenant_id, event_type, channel, rule_code)
);

CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    novu_template_id VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    channel notification_channel NOT NULL,
    subject_en VARCHAR(255),
    subject_ar VARCHAR(255),
    body_en TEXT,
    body_ar TEXT,
    variables JSONB,
    sample_payload JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_template_code UNIQUE (tenant_id, template_code, channel, version)
);

CREATE TABLE delivery_status_tracking (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    notification_id UUID NOT NULL,
    event_log_id UUID,
    novu_message_id VARCHAR(100) NOT NULL,
    novu_transaction_id VARCHAR(100),
    customer_id UUID,
    recipient_address VARCHAR(255) NOT NULL,
    channel notification_channel NOT NULL,
    status delivery_status NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(50),
    provider_message_id VARCHAR(100),
    queued_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    error_code VARCHAR(50),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    webhook_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_delivery_status_tracking PRIMARY KEY (id, created_at),
    CONSTRAINT uq_novu_message UNIQUE (tenant_id, novu_message_id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE delivery_status_tracking_2026_01 PARTITION OF delivery_status_tracking FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE delivery_status_tracking_2026_02 PARTITION OF delivery_status_tracking FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE delivery_status_tracking_2026_03 PARTITION OF delivery_status_tracking FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE delivery_status_tracking_2026_04 PARTITION OF delivery_status_tracking FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE delivery_status_tracking_2026_05 PARTITION OF delivery_status_tracking FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

-- Indexes
CREATE INDEX idx_prefs_customer ON notification_preferences(customer_id);
CREATE INDEX idx_event_log_type ON notification_event_log(event_type);
CREATE INDEX idx_routing_event ON template_routing_rules(event_type);
CREATE INDEX idx_delivery_novu ON delivery_status_tracking(novu_message_id);
