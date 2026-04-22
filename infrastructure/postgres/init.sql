-- Initialize databases for KSA Islamic Financing Platform
-- Fineract Core Banking databases
CREATE DATABASE fineract_tenants;
CREATE DATABASE fineract_default;

-- Platform shared database
CREATE DATABASE ksa_financing;

-- Kong API Gateway database
CREATE DATABASE kong;

-- Microservice databases (database-per-service pattern)
CREATE DATABASE identity_db;
CREATE DATABASE customer_db;
CREATE DATABASE global_profile_db;
CREATE DATABASE pii_vault_db;
CREATE DATABASE kyc_adapter_db;
CREATE DATABASE wallet_db;
CREATE DATABASE product_service_db;
CREATE DATABASE lending_db;
CREATE DATABASE middleware_third_party_db;
CREATE DATABASE fraud_service_db;
CREATE DATABASE ledger_db;
CREATE DATABASE collections_db;
