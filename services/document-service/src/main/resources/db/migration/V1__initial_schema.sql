-- document-service initial schema
-- Stores metadata for encrypted document blobs persisted in MinIO.
--
-- Blob bytes never live in this DB: only object key + crypto envelope +
-- ownership / audit data. Decryption requires both the stored
-- encrypted_key (wrapped DEK) and the master key from env.

CREATE TYPE document_kind AS ENUM (
    'ID_CARD',
    'PASSPORT',
    'DRIVING_LICENCE',
    'RESIDENCE_PERMIT',
    'OTHER'
);

CREATE TYPE document_source_flow AS ENUM (
    'KSA',
    'CANADA',
    'FOREIGN',
    'GUEST',
    'OTHER'
);

CREATE TYPE document_status AS ENUM (
    'ACTIVE',
    'REPLACED',
    'DELETED'
);

CREATE TABLE document_metadata (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         UUID,                       -- LEAD customer (may be null at upload time)
    workflow_id         VARCHAR(128),               -- onboarding workflowId for trace
    kind                document_kind NOT NULL,
    source_flow         document_source_flow NOT NULL DEFAULT 'OTHER',
    document_number     VARCHAR(100),               -- extracted from OCR if available
    facia_reference_id  VARCHAR(100),               -- Facia verification ref
    -- Storage envelope
    object_key          TEXT NOT NULL,              -- MinIO object path
    content_type        VARCHAR(80) NOT NULL,
    size_bytes_plain    BIGINT NOT NULL,            -- original plain size (pre-encryption)
    size_bytes_stored   BIGINT NOT NULL,            -- stored size (ciphertext)
    sha256_plain        VARCHAR(64) NOT NULL,       -- integrity hash of plaintext
    -- Crypto envelope (AES-GCM)
    cipher_algo         VARCHAR(32) NOT NULL DEFAULT 'AES/GCM/NoPadding',
    encrypted_key       BYTEA NOT NULL,             -- DEK wrapped with master key
    iv                  BYTEA NOT NULL,             -- 12-byte GCM nonce
    -- Lifecycle
    status              document_status NOT NULL DEFAULT 'ACTIVE',
    idempotency_key     VARCHAR(128),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    version             INT NOT NULL DEFAULT 1,
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_doc_tenant            ON document_metadata(tenant_id);
CREATE INDEX idx_doc_tenant_customer   ON document_metadata(tenant_id, customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_doc_workflow          ON document_metadata(workflow_id) WHERE workflow_id IS NOT NULL;
CREATE UNIQUE INDEX uq_doc_idempotency ON document_metadata(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL AND deleted_at IS NULL;
