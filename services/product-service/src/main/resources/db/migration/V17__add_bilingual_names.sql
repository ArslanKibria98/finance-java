-- V17__add_bilingual_names.sql
-- Add bilingual name support (name_en + name_ar) to template_types and contract_templates

-- ==========================================
-- 1. template_types: rename name → name_en, add name_ar
-- ==========================================
ALTER TABLE template_types RENAME COLUMN name TO name_en;
ALTER TABLE template_types ADD COLUMN name_ar VARCHAR(100);

-- Update unique constraint to use name_en
DROP INDEX IF EXISTS idx_template_types_unique_name;
CREATE UNIQUE INDEX idx_template_types_unique_name ON template_types(tenant_id, name_en, category);

-- Seed Arabic translations for existing rows
UPDATE template_types SET name_ar = 'عقد' WHERE name_en = 'Contract';
UPDATE template_types SET name_ar = 'اتفاقية' WHERE name_en = 'Agreement';
UPDATE template_types SET name_ar = 'إفصاح' WHERE name_en = 'Disclosure';
UPDATE template_types SET name_ar = 'الشروط والأحكام' WHERE name_en = 'Terms & Conditions';
UPDATE template_types SET name_ar = 'خطاب الموافقة' WHERE name_en = 'Approval Letter';
UPDATE template_types SET name_ar = 'خطاب الرفض' WHERE name_en = 'Rejection Letter';
UPDATE template_types SET name_ar = 'إشعار رسالة نصية' WHERE name_en = 'SMS Notification';
UPDATE template_types SET name_ar = 'إشعار بريد إلكتروني' WHERE name_en = 'Email Notification';

-- ==========================================
-- 2. contract_templates: rename name → name_en, add name_ar
-- ==========================================
ALTER TABLE contract_templates RENAME COLUMN name TO name_en;
ALTER TABLE contract_templates ADD COLUMN name_ar VARCHAR(200);
