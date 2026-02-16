-- V1__initial_schema.sql
-- Initial schema for service-template
-- Supports multi-tenancy via tenant_id column

-- Create sequences
CREATE SEQUENCE IF NOT EXISTS example_aggregate_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS example_entity_seq START WITH 1 INCREMENT BY 1;

-- Create example_aggregate table
CREATE TABLE IF NOT EXISTS example_aggregate (
    id BIGINT PRIMARY KEY DEFAULT nextval('example_aggregate_seq'),
    aggregate_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(100),
    last_modified_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_aggregate_id_tenant UNIQUE (aggregate_id, tenant_id)
);

-- Create indexes for example_aggregate
CREATE INDEX idx_example_aggregate_tenant_id ON example_aggregate(tenant_id);
CREATE INDEX idx_example_aggregate_status ON example_aggregate(status);
CREATE INDEX idx_example_aggregate_tenant_status ON example_aggregate(tenant_id, status);
CREATE INDEX idx_example_aggregate_deleted ON example_aggregate(deleted);
CREATE INDEX idx_example_aggregate_created_at ON example_aggregate(created_at DESC);

-- Create example_entity table (child entities)
CREATE TABLE IF NOT EXISTS example_entity (
    id BIGINT PRIMARY KEY DEFAULT nextval('example_entity_seq'),
    entity_id VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    value TEXT,
    CONSTRAINT fk_example_entity_aggregate
        FOREIGN KEY (aggregate_id)
        REFERENCES example_aggregate(id)
        ON DELETE CASCADE
);

-- Create indexes for example_entity
CREATE INDEX idx_example_entity_aggregate_id ON example_entity(aggregate_id);
CREATE INDEX idx_example_entity_name ON example_entity(name);

-- Create audit table for tracking changes
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_values JSONB,
    new_values JSONB,
    correlation_id VARCHAR(100)
);

-- Create indexes for audit_log
CREATE INDEX idx_audit_log_tenant_id ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp DESC);
CREATE INDEX idx_audit_log_correlation_id ON audit_log(correlation_id);

-- Create event_outbox table for reliable event publishing
CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    retry_count INT DEFAULT 0,
    error_message TEXT
);

-- Create indexes for event_outbox
CREATE INDEX idx_event_outbox_published ON event_outbox(published);
CREATE INDEX idx_event_outbox_created_at ON event_outbox(created_at);
CREATE INDEX idx_event_outbox_aggregate ON event_outbox(aggregate_type, aggregate_id);

-- Add comments for documentation
COMMENT ON TABLE example_aggregate IS 'Main aggregate table for example domain model';
COMMENT ON COLUMN example_aggregate.tenant_id IS 'Multi-tenant identifier';
COMMENT ON COLUMN example_aggregate.version IS 'Optimistic locking version';
COMMENT ON COLUMN example_aggregate.deleted IS 'Soft delete flag';

COMMENT ON TABLE example_entity IS 'Child entities belonging to example aggregates';

COMMENT ON TABLE audit_log IS 'Audit trail for all entity changes';
COMMENT ON COLUMN audit_log.old_values IS 'JSON representation of old entity state';
COMMENT ON COLUMN audit_log.new_values IS 'JSON representation of new entity state';

COMMENT ON TABLE event_outbox IS 'Transactional outbox for reliable event publishing';