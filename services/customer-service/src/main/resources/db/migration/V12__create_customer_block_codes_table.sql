-- V12: Create customer block codes management tables
-- This allows assigning multiple block codes to a single customer from the central registry

CREATE TABLE IF NOT EXISTS block_codes (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer_block_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id),
    block_code_id UUID NOT NULL REFERENCES block_codes(id),
    reason TEXT,
    assigned_by VARCHAR(100),
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE (customer_id, block_code_id, is_active)
);

CREATE INDEX idx_customer_block_codes_customer_id ON customer_block_codes(customer_id) WHERE is_active = TRUE;
CREATE INDEX idx_customer_block_codes_block_code_id ON customer_block_codes(block_code_id);
