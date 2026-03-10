-- ============================================================
-- Middleware Third Party Service — Production Schema
-- Database: middleware_third_party_db
-- ============================================================

-- ===================== ENUM TYPES =====================

CREATE TYPE provider_status AS ENUM ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DEPRECATED');
CREATE TYPE api_status AS ENUM ('ACTIVE', 'INACTIVE', 'DEPRECATED');
CREATE TYPE environment_type AS ENUM ('DEV', 'PROD');
CREATE TYPE http_method AS ENUM ('GET', 'POST', 'PUT', 'PATCH', 'DELETE');
CREATE TYPE auth_type AS ENUM ('NONE', 'BASIC', 'BEARER', 'API_KEY', 'OAUTH2', 'CUSTOM');
CREATE TYPE client_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
CREATE TYPE access_environment AS ENUM ('DEV', 'PROD', 'BOTH');
CREATE TYPE request_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'TIMEOUT');
CREATE TYPE callback_status AS ENUM ('RECEIVED', 'PROCESSED', 'FAILED');
CREATE TYPE provider_category AS ENUM (
    'IDENTITY', 'CREDIT_BUREAU', 'GOVERNMENT', 'COMMUNICATION',
    'OPEN_BANKING', 'COMMODITY_TRADING', 'PROMISSORY_NOTES',
    'DIGITAL_SIGNING', 'NOTIFICATIONS', 'BANKING', 'PAYMENT_GATEWAY',
    'INCOME_VERIFICATION', 'FINANCIAL', 'AML_SCREENING'
);

-- ===================== TABLE 1: third_party_providers =====================

CREATE TABLE third_party_providers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(50) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category        provider_category NOT NULL,
    base_url_dev    TEXT,
    base_url_prod   TEXT,
    auth_type       auth_type NOT NULL DEFAULT 'NONE',
    status          provider_status NOT NULL DEFAULT 'ACTIVE',
    timeout_ms      INT NOT NULL DEFAULT 30000,
    retry_count     INT NOT NULL DEFAULT 3,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         INT NOT NULL DEFAULT 1,
    UNIQUE (tenant_id, code)
);

CREATE INDEX idx_providers_tenant ON third_party_providers(tenant_id);
CREATE INDEX idx_providers_status ON third_party_providers(tenant_id, status);
CREATE INDEX idx_providers_code ON third_party_providers(tenant_id, code);
CREATE INDEX idx_providers_category ON third_party_providers(tenant_id, category);

-- ===================== TABLE 2: provider_apis =====================

CREATE TABLE provider_apis (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    provider_id     UUID NOT NULL REFERENCES third_party_providers(id),
    code            VARCHAR(100) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    http_method     http_method NOT NULL DEFAULT 'POST',
    endpoint_path   TEXT NOT NULL,
    status          api_status NOT NULL DEFAULT 'ACTIVE',
    is_async        BOOLEAN NOT NULL DEFAULT FALSE,
    timeout_ms      INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         INT NOT NULL DEFAULT 1,
    UNIQUE (tenant_id, code)
);

CREATE INDEX idx_apis_tenant ON provider_apis(tenant_id);
CREATE INDEX idx_apis_provider ON provider_apis(provider_id);
CREATE INDEX idx_apis_code ON provider_apis(tenant_id, code);
CREATE INDEX idx_apis_status ON provider_apis(tenant_id, status);

-- ===================== TABLE 3: api_environment_configs =====================

CREATE TABLE api_environment_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    api_id          UUID NOT NULL REFERENCES provider_apis(id),
    environment     environment_type NOT NULL,
    base_url        TEXT NOT NULL,
    endpoint_path   TEXT,
    credentials     JSONB,
    headers         JSONB,
    query_params    JSONB,
    auth_type       auth_type,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         INT NOT NULL DEFAULT 1,
    UNIQUE (tenant_id, api_id, environment)
);

CREATE INDEX idx_env_configs_tenant ON api_environment_configs(tenant_id);
CREATE INDEX idx_env_configs_api ON api_environment_configs(api_id);

-- ===================== TABLE 4: api_clients =====================

CREATE TABLE api_clients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(50) NOT NULL,
    description     TEXT,
    secret_key      VARCHAR(500),
    callback_url    TEXT,
    ip_whitelist    TEXT[],
    status          client_status NOT NULL DEFAULT 'ACTIVE',
    environment     access_environment NOT NULL DEFAULT 'BOTH',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         INT NOT NULL DEFAULT 1,
    UNIQUE (tenant_id, code)
);

CREATE INDEX idx_clients_tenant ON api_clients(tenant_id);
CREATE INDEX idx_clients_code ON api_clients(tenant_id, code);
CREATE INDEX idx_clients_status ON api_clients(tenant_id, status);

-- ===================== TABLE 5: client_provider_access =====================

CREATE TABLE client_provider_access (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    client_id       UUID NOT NULL REFERENCES api_clients(id),
    provider_id     UUID NOT NULL REFERENCES third_party_providers(id),
    environment     access_environment NOT NULL DEFAULT 'BOTH',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by      UUID,
    UNIQUE (tenant_id, client_id, provider_id)
);

CREATE INDEX idx_client_provider_tenant ON client_provider_access(tenant_id);
CREATE INDEX idx_client_provider_client ON client_provider_access(client_id);
CREATE INDEX idx_client_provider_provider ON client_provider_access(provider_id);

-- ===================== TABLE 6: client_api_access =====================

CREATE TABLE client_api_access (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    client_id       UUID NOT NULL REFERENCES api_clients(id),
    api_id          UUID NOT NULL REFERENCES provider_apis(id),
    environment     access_environment NOT NULL DEFAULT 'BOTH',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by      UUID,
    UNIQUE (tenant_id, client_id, api_id)
);

CREATE INDEX idx_client_api_tenant ON client_api_access(tenant_id);
CREATE INDEX idx_client_api_client ON client_api_access(client_id);
CREATE INDEX idx_client_api_api ON client_api_access(api_id);

-- ===================== TABLE 7: api_request_logs =====================

CREATE TABLE api_request_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    api_id          UUID NOT NULL REFERENCES provider_apis(id),
    client_id       UUID REFERENCES api_clients(id),
    request_id      VARCHAR(100) NOT NULL,
    environment     environment_type NOT NULL,
    http_method     http_method NOT NULL,
    request_url     TEXT NOT NULL,
    request_headers JSONB,
    request_body    JSONB,
    response_status INT,
    response_headers JSONB,
    response_body   JSONB,
    status          request_status NOT NULL DEFAULT 'PENDING',
    duration_ms     BIGINT,
    error_message   TEXT,
    idempotency_key VARCHAR(100),
    national_id     VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_request_logs_tenant ON api_request_logs(tenant_id);
CREATE INDEX idx_request_logs_api ON api_request_logs(api_id);
CREATE INDEX idx_request_logs_client ON api_request_logs(client_id);
CREATE INDEX idx_request_logs_request_id ON api_request_logs(request_id);
CREATE INDEX idx_request_logs_status ON api_request_logs(tenant_id, status);
CREATE INDEX idx_request_logs_created ON api_request_logs(tenant_id, created_at DESC);
CREATE INDEX idx_request_logs_env ON api_request_logs(tenant_id, environment);

-- ===================== TABLE 8: callback_responses =====================

CREATE TABLE callback_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    api_id          UUID NOT NULL REFERENCES provider_apis(id),
    client_id       UUID REFERENCES api_clients(id),
    request_log_id  UUID REFERENCES api_request_logs(id),
    callback_data   JSONB NOT NULL,
    status          callback_status NOT NULL DEFAULT 'RECEIVED',
    processed_at    TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_callbacks_tenant ON callback_responses(tenant_id);
CREATE INDEX idx_callbacks_api ON callback_responses(api_id);
CREATE INDEX idx_callbacks_request ON callback_responses(request_log_id);
CREATE INDEX idx_callbacks_status ON callback_responses(tenant_id, status);
