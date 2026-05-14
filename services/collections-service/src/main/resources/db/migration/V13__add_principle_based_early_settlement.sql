-- V13: Add fields for Principle-based Early Settlement
ALTER TABLE delinquency_rules
    ADD COLUMN settlement_strategy           SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN settlement_discount_type      VARCHAR(20),
    ADD COLUMN settlement_months             INT DEFAULT 0,
    ADD COLUMN settlement_amount_per_month   NUMERIC(19, 4) DEFAULT 0.00;

COMMENT ON COLUMN delinquency_rules.settlement_strategy IS '1=Invoice Based, 2=Principle Based';
