-- V7__add_country_icon_fields.sql
-- Add flag_emoji, dial_code, nationality fields to supported_countries

ALTER TABLE supported_countries ADD COLUMN flag_emoji VARCHAR(10);
ALTER TABLE supported_countries ADD COLUMN dial_code VARCHAR(10);
ALTER TABLE supported_countries ADD COLUMN nationality_en VARCHAR(50);
ALTER TABLE supported_countries ADD COLUMN nationality_ar VARCHAR(50);

UPDATE supported_countries SET flag_emoji = '🇸🇦', dial_code = '+966', nationality_en = 'Saudi', nationality_ar = 'سعودي' WHERE country_code = 'SAU';
UPDATE supported_countries SET flag_emoji = '🇦🇪', dial_code = '+971', nationality_en = 'Emirati', nationality_ar = 'إماراتي' WHERE country_code = 'ARE';
UPDATE supported_countries SET flag_emoji = '🇵🇰', dial_code = '+92', nationality_en = 'Pakistani', nationality_ar = 'باكستاني' WHERE country_code = 'PAK';
