-- V4__convert_enums_to_varchar.sql
-- Convert PostgreSQL custom enum types to VARCHAR for Hibernate compatibility.
-- The application layer validates enum values; DB doesn't need to enforce them.

-- 1. status: application_status -> varchar (loan_applications + history table)
ALTER TABLE loan_applications
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE application_status_history
    ALTER COLUMN from_status TYPE VARCHAR(50) USING from_status::text;

ALTER TABLE application_status_history
    ALTER COLUMN to_status TYPE VARCHAR(50) USING to_status::text;

-- 2. purpose_of_finance: purpose_of_finance -> varchar (nullable)
ALTER TABLE loan_applications
    ALTER COLUMN purpose_of_finance TYPE VARCHAR(50) USING purpose_of_finance::text;

-- 3. sharia_structure: sharia_structure -> varchar (loan_applications + loans)
ALTER TABLE loan_applications
    ALTER COLUMN sharia_structure TYPE VARCHAR(50) USING sharia_structure::text;

ALTER TABLE loans
    ALTER COLUMN sharia_structure TYPE VARCHAR(50) USING sharia_structure::text;

-- Make sharia_structure nullable in loan_applications (not set at DRAFT stage)
ALTER TABLE loan_applications
    ALTER COLUMN sharia_structure DROP NOT NULL;

-- Drop the enum types (no longer needed)
DROP TYPE IF EXISTS application_status CASCADE;
DROP TYPE IF EXISTS purpose_of_finance CASCADE;
DROP TYPE IF EXISTS sharia_structure CASCADE;
