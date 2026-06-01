-- V5__change_default_language_to_english.sql
-- Default onboarding language: ar → en
-- Supported languages: en, ar, ur, es (validated at application layer)
-- Existing rows are NOT modified — only future inserts default to 'en'.

ALTER TABLE notification_preferences
    ALTER COLUMN preferred_language SET DEFAULT 'en';
