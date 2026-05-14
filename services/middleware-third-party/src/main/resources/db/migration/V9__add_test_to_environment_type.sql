-- V9: Add TEST value to environment_type enum
-- Allows mock executions to be tagged as TEST environment in api_request_logs
-- and the new client_request_test table.
ALTER TYPE environment_type ADD VALUE IF NOT EXISTS 'TEST' BEFORE 'DEV';
