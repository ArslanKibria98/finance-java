-- V8: Convert remaining JSONB columns to VARCHAR where JPA sends plain strings

ALTER TABLE product_approval_actions ALTER COLUMN configuration TYPE VARCHAR(2000) USING configuration::text;

ALTER TABLE environment_configs ALTER COLUMN parameters TYPE VARCHAR(2000) USING parameters::text;
ALTER TABLE environment_configs ALTER COLUMN credentials TYPE VARCHAR(2000) USING credentials::text;
ALTER TABLE environment_configs ALTER COLUMN headers TYPE VARCHAR(2000) USING headers::text;
