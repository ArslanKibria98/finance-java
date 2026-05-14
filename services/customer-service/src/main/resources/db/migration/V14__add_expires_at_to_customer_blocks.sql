-- V14: Add expires_at column to customer_block_codes table
ALTER TABLE customer_block_codes ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;
