-- V33: Add Source of Wealth, Source of Funds, Net Worth Range, Occupation as
-- credit scoring field definitions with predefined options (same flow as Gender).
--
-- Codes mirror customer-service reference tables so weightage rules resolve
-- correctly against captured customer values at scoring time:
--   - source_of_wealth_options          (V2__edd_reference_data.sql)
--   - source_of_funds_options           (V2__edd_reference_data.sql)
--   - net_worth_range_options           (V2__edd_reference_data.sql)
--   - occupation_options                (V17__occupation_options.sql)

-- ============================================================================
-- 1. FIELD DEFINITIONS
-- ============================================================================

INSERT INTO credit_scoring_field_definitions (tenant_id, field_key, name_en, name_ar, data_type, sort_order)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'source_of_wealth', 'Source Of Wealth', 'مصدر الثروة',  'STRING', 41),
    ('00000000-0000-0000-0000-000000000001', 'source_of_funds',  'Source Of Funds',  'مصدر الأموال', 'STRING', 42),
    ('00000000-0000-0000-0000-000000000001', 'net_worth_range',  'Net Worth Range',  'نطاق صافي الثروة', 'STRING', 43),
    ('00000000-0000-0000-0000-000000000001', 'occupation',       'Occupation',       'المهنة',       'STRING', 44)
ON CONFLICT (tenant_id, field_key) DO NOTHING;

-- ============================================================================
-- 2. FIELD OPTIONS
-- ============================================================================

-- 2.1 Source Of Wealth
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', d.id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('SALARY',             'Salary / Employment Income',   'الراتب / دخل العمل',             1),
    ('BUSINESS_INCOME',    'Business Income / Ownership',  'دخل الأعمال / الملكية',          2),
    ('INHERITANCE',        'Inheritance',                  'الميراث',                         3),
    ('INVESTMENTS',        'Investments',                  'الاستثمارات',                     4),
    ('REAL_ESTATE',        'Real Estate',                  'العقارات',                        5),
    ('RETIREMENT_PENSION', 'Retirement / Pension',         'التقاعد / المعاش',                6),
    ('GOVERNMENT_GRANTS',  'Government Grants / Benefits', 'المنح الحكومية / الإعانات',       7),
    ('OTHER',              'Other',                        'أخرى',                            8)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'source_of_wealth' AND d.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, field_definition_id, option_key) DO NOTHING;

-- 2.2 Source Of Funds
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', d.id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('EMPLOYMENT_SALARY',  'Employment Salary',  'راتب العمل',           1),
    ('BUSINESS_REVENUE',   'Business Revenue',   'إيرادات الأعمال',      2),
    ('SAVINGS',            'Savings',            'المدخرات',             3),
    ('LOAN_PROCEEDS',      'Loan Proceeds',      'عائدات القرض',         4),
    ('INVESTMENT_RETURNS', 'Investment Returns', 'عوائد الاستثمار',      5),
    ('PROPERTY_SALE',      'Property Sale',      'بيع العقارات',         6),
    ('INHERITANCE_FUNDS',  'Inheritance',        'أموال الميراث',        7),
    ('GIFT',               'Gift',               'هدية',                 8)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'source_of_funds' AND d.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, field_definition_id, option_key) DO NOTHING;

-- 2.3 Net Worth Range
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', d.id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('UNDER_50K',        'Under 50,000 SAR',           'أقل من 50,000 ريال',            1),
    ('RANGE_50K_100K',   '50,000 - 100,000 SAR',       '50,000 - 100,000 ريال',         2),
    ('RANGE_100K_500K',  '100,000 - 500,000 SAR',      '100,000 - 500,000 ريال',        3),
    ('RANGE_500K_1M',    '500,000 - 1,000,000 SAR',    '500,000 - 1,000,000 ريال',      4),
    ('RANGE_1M_5M',      '1,000,000 - 5,000,000 SAR',  '1,000,000 - 5,000,000 ريال',    5),
    ('RANGE_5M_10M',     '5,000,000 - 10,000,000 SAR', '5,000,000 - 10,000,000 ريال',   6),
    ('OVER_10M',         'Over 10,000,000 SAR',        'أكثر من 10,000,000 ريال',       7)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'net_worth_range' AND d.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, field_definition_id, option_key) DO NOTHING;

-- 2.4 Occupation
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', d.id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('EMPLOYED_PRIVATE', 'Employed (Private Sector)',      'موظف (قطاع خاص)',           1),
    ('EMPLOYED_PUBLIC',  'Employed (Public Sector)',       'موظف (قطاع حكومي)',         2),
    ('SELF_EMPLOYED',    'Self-Employed / Business Owner', 'صاحب عمل / تاجر',           3),
    ('RETIRED',          'Retired',                        'متقاعد',                     4),
    ('STUDENT',          'Student',                        'طالب',                       5),
    ('UNEMPLOYED',       'Unemployed / Homemaker',         'عاطل عن العمل / رب منزل',   6),
    ('OTHER',            'Other',                          'أخرى',                       7)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'occupation' AND d.tenant_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (tenant_id, field_definition_id, option_key) DO NOTHING;
