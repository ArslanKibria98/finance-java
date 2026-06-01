-- Drop dynamic onboarding engine tables.
-- KSA flow is fully Temporal-driven and does not depend on these tables.

DROP TABLE IF EXISTS step_submissions CASCADE;
DROP TABLE IF EXISTS field_configs CASCADE;
DROP TABLE IF EXISTS step_configs CASCADE;
DROP TABLE IF EXISTS workflow_configs CASCADE;
