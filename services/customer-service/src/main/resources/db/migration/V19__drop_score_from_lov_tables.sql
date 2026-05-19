-- V19: Drop score column from LOV reference tables (reversal of V18).
-- Tables: source_of_wealth_options, source_of_income_options, net_worth_range_options, occupation_options

ALTER TABLE source_of_wealth_options    DROP COLUMN IF EXISTS score;
ALTER TABLE source_of_income_options    DROP COLUMN IF EXISTS score;
ALTER TABLE net_worth_range_options     DROP COLUMN IF EXISTS score;
ALTER TABLE occupation_options          DROP COLUMN IF EXISTS score;
