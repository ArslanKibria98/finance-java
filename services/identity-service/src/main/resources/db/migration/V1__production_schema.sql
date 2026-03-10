-- ============================================================================
-- IDENTITY SERVICE DATABASE SCHEMA (KEYCLOAK BRIDGE)
-- PostgreSQL 16+ Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE user_type AS ENUM (
    'CUSTOMER',
    'EMPLOYEE',
    'PARTNER',
    'SYSTEM',
    'API_CLIENT'
);

CREATE TYPE user_status AS ENUM (
    'PENDING_VERIFICATION',
    'ACTIVE',
    'SUSPENDED',
    'LOCKED',
    'DEACTIVATED'
);

CREATE TYPE permission_effect AS ENUM (
    'ALLOW',
    'DENY'
);

CREATE TYPE session_status AS ENUM (
    'ACTIVE',
    'EXPIRED',
    'REVOKED'
);

-- ============================================================================
-- IDENTITY MAPPING TABLES
-- ============================================================================

CREATE TABLE user_identity_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    keycloak_user_id UUID NOT NULL,
    keycloak_realm VARCHAR(100) NOT NULL,
    keycloak_username VARCHAR(255) NOT NULL,
    internal_user_id UUID NOT NULL,
    internal_customer_id UUID,
    internal_partner_id UUID,
    global_uid UUID,
    user_type user_type NOT NULL,
    status user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_keycloak_user UNIQUE (keycloak_realm, keycloak_user_id),
    CONSTRAINT uq_internal_user UNIQUE (tenant_id, internal_user_id)
);

CREATE TABLE user_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_identity_id UUID NOT NULL REFERENCES user_identity_mapping(id),
    from_status user_status,
    to_status user_status NOT NULL,
    changed_by UUID,
    change_reason VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_identity_id UUID NOT NULL REFERENCES user_identity_mapping(id),
    display_name VARCHAR(255),
    display_name_ar VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(20),
    preferred_language VARCHAR(10) DEFAULT 'ar',
    avatar_url VARCHAR(500),
    device_tokens JSONB,
    preferences JSONB,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_methods VARCHAR(50)[],
    last_login_at TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ,
    login_count INT NOT NULL DEFAULT 0,
    last_ip_address VARCHAR(45),
    trusted_ips VARCHAR(45)[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_user_metadata UNIQUE (user_identity_id)
);

-- ============================================================================
-- RBAC TABLES
-- ============================================================================

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    role_name_ar VARCHAR(255),
    description TEXT,
    keycloak_role_id UUID,
    keycloak_realm VARCHAR(100),
    parent_role_id UUID REFERENCES roles(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_role_code UNIQUE (tenant_id, role_code)
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    description TEXT,
    resource_type VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permission_code UNIQUE (tenant_id, permission_code)
);

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    effect permission_effect NOT NULL DEFAULT 'ALLOW',
    conditions JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id)
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_identity_id UUID NOT NULL REFERENCES user_identity_mapping(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    scope_type VARCHAR(50),
    scope_id UUID,
    valid_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_until TIMESTAMPTZ,
    assigned_by UUID,
    assignment_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_role UNIQUE (user_identity_id, role_id, scope_type, scope_id)
);

CREATE TABLE permission_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_identity_id UUID NOT NULL,
    permissions JSONB NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    cache_version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_permission_cache UNIQUE (user_identity_id)
);

-- ============================================================================
-- SESSION TABLES
-- ============================================================================

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_identity_id UUID NOT NULL REFERENCES user_identity_mapping(id),
    session_id VARCHAR(100) NOT NULL,
    keycloak_session_id VARCHAR(100),
    device_id VARCHAR(100),
    device_type VARCHAR(50),
    device_name VARCHAR(255),
    user_agent TEXT,
    ip_address VARCHAR(45),
    geo_location JSONB,
    status session_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uq_session UNIQUE (session_id)
);

CREATE TABLE change_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(100),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_identity_keycloak ON user_identity_mapping(keycloak_user_id);
CREATE INDEX idx_identity_internal ON user_identity_mapping(internal_user_id);
CREATE INDEX idx_identity_customer ON user_identity_mapping(internal_customer_id) WHERE internal_customer_id IS NOT NULL;
CREATE INDEX idx_identity_global_uid ON user_identity_mapping(global_uid) WHERE global_uid IS NOT NULL;
CREATE INDEX idx_identity_status ON user_identity_mapping(tenant_id, status);

CREATE INDEX idx_metadata_user ON user_metadata(user_identity_id);

CREATE INDEX idx_roles_active ON roles(tenant_id) WHERE is_active = TRUE;

CREATE INDEX idx_user_roles_user ON user_roles(user_identity_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

CREATE INDEX idx_permission_cache_user ON permission_cache(user_identity_id);
CREATE INDEX idx_permission_cache_valid ON permission_cache(tenant_id) WHERE is_valid = TRUE;

CREATE INDEX idx_sessions_user ON user_sessions(user_identity_id);
CREATE INDEX idx_sessions_active ON user_sessions(tenant_id) WHERE status = 'ACTIVE';

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE user_identity_mapping ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_metadata ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON user_identity_mapping
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON user_metadata
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON roles
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE user_identity_mapping IS 'Maps Keycloak user IDs to internal domain IDs';
COMMENT ON COLUMN user_identity_mapping.global_uid IS 'Links to Global Profile Index for multi-jurisdictional identity federation';
COMMENT ON TABLE user_metadata IS 'Extended user profile and preferences';
COMMENT ON TABLE roles IS 'RBAC roles (cached from Keycloak)';
COMMENT ON TABLE permission_cache IS 'Flattened permission cache for fast authorization';
COMMENT ON TABLE user_sessions IS 'Active user session tracking';
