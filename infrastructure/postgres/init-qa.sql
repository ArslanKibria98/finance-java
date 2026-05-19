-- QA databases for PM2-managed services running alongside Docker dev.
-- Schemas are created by Flyway when each service first boots.
--
-- Run from host:
--   docker exec -i ksa-postgres psql -U postgres < infrastructure/postgres/init-qa.sql

CREATE DATABASE identity_db_qa;
CREATE DATABASE customer_db_qa;
CREATE DATABASE global_profile_db_qa;
CREATE DATABASE kyc_adapter_db_qa;
CREATE DATABASE wallet_db_qa;
CREATE DATABASE risk_service_qa;
CREATE DATABASE product_service_db_qa;
CREATE DATABASE fraud_service_db_qa;
CREATE DATABASE ledger_db_qa;
CREATE DATABASE notification_db_qa;
CREATE DATABASE lending_db_qa;
CREATE DATABASE collections_db_qa;
CREATE DATABASE onboarding_db_qa;
