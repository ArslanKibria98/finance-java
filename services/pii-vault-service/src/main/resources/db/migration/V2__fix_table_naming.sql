-- Fix singular table names to plural per NAMING_CONVENTIONS.md
ALTER TABLE pii_individual RENAME TO pii_individuals;
ALTER TABLE pii_access_audit RENAME TO pii_access_audits;
