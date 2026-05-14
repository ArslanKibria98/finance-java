-- V10: Create per-environment client_request tables
--
-- ExecuteApiService routes incoming third-party calls based on the calling
-- ApiClient's environment (TEST, DEV, PROD). Each request/response pair is
-- persisted into the matching env-specific table so display & analytics can
-- be filtered cleanly per environment.
--
-- TEST    -> mock responses (no live HTTP call)            -> client_request_test
-- DEV     -> live HTTP call to provider DEV credentials    -> client_request_dev
-- PROD    -> live HTTP call to provider PROD credentials   -> client_request_prod
--
-- The legacy api_request_logs table is preserved for historical data and the
-- callback_responses FK; new flows write only to the env-specific tables.

CREATE TABLE client_request_test (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    api_id              UUID NOT NULL REFERENCES provider_apis(id),
    client_id           UUID NOT NULL REFERENCES api_clients(id),
    request_id          VARCHAR(100) NOT NULL,
    provider_code       VARCHAR(100) NOT NULL,
    api_code            VARCHAR(100) NOT NULL,
    http_method         http_method NOT NULL,
    request_url         TEXT NOT NULL,
    request_headers     JSONB,
    request_body        JSONB,
    response_status     INT,
    response_headers    JSONB,
    response_body       JSONB,
    status              request_status NOT NULL DEFAULT 'PENDING',
    duration_ms         BIGINT,
    error_message       TEXT,
    idempotency_key     VARCHAR(100),
    national_id         VARCHAR(20),
    mobile_number       VARCHAR(30),
    caller_service      VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_creq_test_tenant         ON client_request_test(tenant_id);
CREATE INDEX idx_creq_test_client         ON client_request_test(tenant_id, client_id);
CREATE INDEX idx_creq_test_api            ON client_request_test(tenant_id, api_id);
CREATE INDEX idx_creq_test_request_id     ON client_request_test(tenant_id, request_id);
CREATE INDEX idx_creq_test_national_id    ON client_request_test(tenant_id, national_id);
CREATE INDEX idx_creq_test_api_code       ON client_request_test(tenant_id, api_code);
CREATE INDEX idx_creq_test_status         ON client_request_test(tenant_id, status);
CREATE INDEX idx_creq_test_created        ON client_request_test(tenant_id, created_at DESC);

CREATE TABLE client_request_dev (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    api_id              UUID NOT NULL REFERENCES provider_apis(id),
    client_id           UUID NOT NULL REFERENCES api_clients(id),
    request_id          VARCHAR(100) NOT NULL,
    provider_code       VARCHAR(100) NOT NULL,
    api_code            VARCHAR(100) NOT NULL,
    http_method         http_method NOT NULL,
    request_url         TEXT NOT NULL,
    request_headers     JSONB,
    request_body        JSONB,
    response_status     INT,
    response_headers    JSONB,
    response_body       JSONB,
    status              request_status NOT NULL DEFAULT 'PENDING',
    duration_ms         BIGINT,
    error_message       TEXT,
    idempotency_key     VARCHAR(100),
    national_id         VARCHAR(20),
    mobile_number       VARCHAR(30),
    caller_service      VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_creq_dev_tenant          ON client_request_dev(tenant_id);
CREATE INDEX idx_creq_dev_client          ON client_request_dev(tenant_id, client_id);
CREATE INDEX idx_creq_dev_api             ON client_request_dev(tenant_id, api_id);
CREATE INDEX idx_creq_dev_request_id      ON client_request_dev(tenant_id, request_id);
CREATE INDEX idx_creq_dev_national_id     ON client_request_dev(tenant_id, national_id);
CREATE INDEX idx_creq_dev_api_code        ON client_request_dev(tenant_id, api_code);
CREATE INDEX idx_creq_dev_status          ON client_request_dev(tenant_id, status);
CREATE INDEX idx_creq_dev_created         ON client_request_dev(tenant_id, created_at DESC);

CREATE TABLE client_request_prod (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    api_id              UUID NOT NULL REFERENCES provider_apis(id),
    client_id           UUID NOT NULL REFERENCES api_clients(id),
    request_id          VARCHAR(100) NOT NULL,
    provider_code       VARCHAR(100) NOT NULL,
    api_code            VARCHAR(100) NOT NULL,
    http_method         http_method NOT NULL,
    request_url         TEXT NOT NULL,
    request_headers     JSONB,
    request_body        JSONB,
    response_status     INT,
    response_headers    JSONB,
    response_body       JSONB,
    status              request_status NOT NULL DEFAULT 'PENDING',
    duration_ms         BIGINT,
    error_message       TEXT,
    idempotency_key     VARCHAR(100),
    national_id         VARCHAR(20),
    mobile_number       VARCHAR(30),
    caller_service      VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_creq_prod_tenant         ON client_request_prod(tenant_id);
CREATE INDEX idx_creq_prod_client         ON client_request_prod(tenant_id, client_id);
CREATE INDEX idx_creq_prod_api            ON client_request_prod(tenant_id, api_id);
CREATE INDEX idx_creq_prod_request_id     ON client_request_prod(tenant_id, request_id);
CREATE INDEX idx_creq_prod_national_id    ON client_request_prod(tenant_id, national_id);
CREATE INDEX idx_creq_prod_api_code       ON client_request_prod(tenant_id, api_code);
CREATE INDEX idx_creq_prod_status         ON client_request_prod(tenant_id, status);
CREATE INDEX idx_creq_prod_created        ON client_request_prod(tenant_id, created_at DESC);
