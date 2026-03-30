-- V8__add_bilingual_names.sql
-- Add bilingual name/description support (en + ar) to third_party_providers and provider_apis

-- ==========================================
-- 1. third_party_providers: rename name/description → _en, add _ar
-- ==========================================
ALTER TABLE third_party_providers RENAME COLUMN name TO name_en;
ALTER TABLE third_party_providers ADD COLUMN name_ar VARCHAR(200);

ALTER TABLE third_party_providers RENAME COLUMN description TO description_en;
ALTER TABLE third_party_providers ADD COLUMN description_ar TEXT;

-- ==========================================
-- 2. provider_apis: rename name/description → _en, add _ar
-- ==========================================
ALTER TABLE provider_apis RENAME COLUMN name TO name_en;
ALTER TABLE provider_apis ADD COLUMN name_ar VARCHAR(200);

ALTER TABLE provider_apis RENAME COLUMN description TO description_en;
ALTER TABLE provider_apis ADD COLUMN description_ar TEXT;
