-- V30__expand_approval_condition_operator_length.sql
-- UI sends long operator names like 'greater_than_or_equal' (21 chars) which overflows VARCHAR(20).

ALTER TABLE product_approval_conditions
    ALTER COLUMN operator TYPE VARCHAR(50);
