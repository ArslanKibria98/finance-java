-- ============================================================================
-- V7: Add HELD_AML to withdrawal_status enum
-- Must be in its own migration — Postgres requires the ALTER TYPE to commit
-- before the new value can be used in DDL (e.g. WHERE clauses on partial indexes).
-- ============================================================================

ALTER TYPE withdrawal_status ADD VALUE IF NOT EXISTS 'HELD_AML' BEFORE 'DEBITED';
