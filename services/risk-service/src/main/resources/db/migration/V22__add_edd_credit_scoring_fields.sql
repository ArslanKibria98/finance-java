-- V22: Add Source of Wealth, Source of Funds, Net Worth Range, Occupation as
-- credit scoring field definitions in risk-service (same as product-service V33).
--
-- Risk-service does not store field options (only definitions); options for
-- these STRING fields are served by product-service. Codes mirror customer-service:
--   - source_of_wealth_options          (customer-service V2)
--   - source_of_funds_options           (customer-service V2)
--   - net_worth_range_options           (customer-service V2)
--   - occupation_options                (customer-service V17)

INSERT INTO credit_scoring_field_definitions (tenant_id, field_key, name_en, name_ar, data_type, sort_order)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'source_of_wealth', 'Source Of Wealth', 'مصدر الثروة',    'STRING', 41),
    ('00000000-0000-0000-0000-000000000001', 'source_of_funds',  'Source Of Funds',  'مصدر الأموال',   'STRING', 42),
    ('00000000-0000-0000-0000-000000000001', 'net_worth_range',  'Net Worth Range',  'نطاق صافي الثروة', 'STRING', 43),
    ('00000000-0000-0000-0000-000000000001', 'occupation',       'Occupation',       'المهنة',          'STRING', 44)
ON CONFLICT (tenant_id, field_key) DO NOTHING;
