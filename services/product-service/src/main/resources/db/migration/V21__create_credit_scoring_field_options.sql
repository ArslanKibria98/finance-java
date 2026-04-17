-- V21: Create credit_scoring_field_options table for predefined dropdown values
-- STRING/BOOLEAN fields get predefined options; NUMERIC fields have no options (freetext input)

CREATE TABLE credit_scoring_field_options (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    field_definition_id UUID NOT NULL REFERENCES credit_scoring_field_definitions(id) ON DELETE CASCADE,
    option_key          VARCHAR(100) NOT NULL,
    label_en            VARCHAR(255) NOT NULL,
    label_ar            VARCHAR(255) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_field_options_field_def ON credit_scoring_field_options(field_definition_id);
CREATE INDEX idx_field_options_tenant ON credit_scoring_field_options(tenant_id);
CREATE UNIQUE INDEX idx_field_options_unique ON credit_scoring_field_options(tenant_id, field_definition_id, option_key);

-- Trigger for updated_at
CREATE TRIGGER trg_field_options_updated_at
    BEFORE UPDATE ON credit_scoring_field_options
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- SEED OPTIONS FOR STRING/BOOLEAN FIELDS
-- =============================================

-- 1. Customer Type
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('CITIZEN',      'Citizen',       'مواطن',         1),
    ('RESIDENT',     'Resident',      'مقيم',          2),
    ('GCC_NATIONAL', 'GCC National',  'مواطن خليجي',   3)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'customer_type' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 2. Gender
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('MALE',   'Male',   'ذكر',  1),
    ('FEMALE', 'Female', 'أنثى', 2)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'gender' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 3. Nationality
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('SAUDI',   'Saudi',   'سعودي',  1),
    ('GCC',     'GCC',     'خليجي',  2),
    ('ARAB',    'Arab',    'عربي',   3),
    ('ASIAN',   'Asian',   'آسيوي',  4),
    ('WESTERN', 'Western', 'غربي',   5),
    ('OTHER',   'Other',   'أخرى',   6)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'nationality' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 4. Residency Type
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('OWNED',             'Owned',             'ملك',            1),
    ('RENTED',            'Rented',            'إيجار',          2),
    ('EMPLOYER_PROVIDED', 'Employer Provided', 'مقدم من جهة العمل', 3),
    ('FAMILY',            'Family',            'عائلي',          4)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'residency_type' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 5. Marital Status
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('MARRIED',  'Married',  'متزوج',  1),
    ('SINGLE',   'Single',   'أعزب',   2),
    ('DIVORCED', 'Divorced', 'مطلق',   3),
    ('WIDOWED',  'Widowed',  'أرمل',   4)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'marital_status' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 6. Education Level
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('POSTGRADUATE',      'Postgraduate',      'دراسات عليا',     1),
    ('BACHELOR',          'Bachelor',          'بكالوريوس',       2),
    ('DIPLOMA',           'Diploma',           'دبلوم',           3),
    ('HIGH_SCHOOL',       'High School',       'ثانوية عامة',     4),
    ('BELOW_HIGH_SCHOOL', 'Below High School', 'أقل من ثانوية',   5)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'education_level' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 7. Employment Type
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('GOVERNMENT',      'Government',      'حكومي',         1),
    ('MILITARY',        'Military',        'عسكري',         2),
    ('SEMI_GOVERNMENT', 'Semi-Government', 'شبه حكومي',     3),
    ('PRIVATE_SECTOR',  'Private Sector',  'قطاع خاص',      4),
    ('SELF_EMPLOYED',   'Self Employed',   'عمل حر',        5),
    ('RETIRED',         'Retired',         'متقاعد',        6)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'employment_type' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 8. Employment Sector
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('OIL_AND_GAS',      'Oil & Gas',         'نفط وغاز',           1),
    ('BANKING_FINANCE',  'Banking & Finance', 'بنوك وتمويل',        2),
    ('HEALTHCARE',       'Healthcare',        'رعاية صحية',         3),
    ('EDUCATION',        'Education',         'تعليم',              4),
    ('TELECOM',          'Telecom',           'اتصالات',            5),
    ('CONSTRUCTION',     'Construction',      'مقاولات وبناء',      6),
    ('RETAIL',           'Retail',            'تجزئة',              7),
    ('HOSPITALITY',      'Hospitality',       'ضيافة وسياحة',       8),
    ('TRANSPORTATION',   'Transportation',    'نقل ومواصلات',       9),
    ('TECHNOLOGY',       'Technology',        'تقنية المعلومات',    10),
    ('REAL_ESTATE',      'Real Estate',       'عقارات',            11),
    ('MANUFACTURING',    'Manufacturing',     'تصنيع',             12),
    ('OTHER',            'Other',             'أخرى',              13)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'employment_sector' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 9. Employer Name (major KSA employers)
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('ARAMCO',       'Saudi Aramco',                    'أرامكو السعودية',        1),
    ('SABIC',        'SABIC',                           'سابك',                   2),
    ('STC',          'STC',                             'الاتصالات السعودية',      3),
    ('SEC',          'Saudi Electricity Company',       'الكهرباء السعودية',       4),
    ('MOH',          'Ministry of Health',              'وزارة الصحة',            5),
    ('MOE',          'Ministry of Education',           'وزارة التعليم',          6),
    ('MOD',          'Ministry of Defense',             'وزارة الدفاع',           7),
    ('MOI',          'Ministry of Interior',            'وزارة الداخلية',         8),
    ('MAADEN',       'Maaden',                          'معادن',                  9),
    ('SNB',          'Saudi National Bank',             'البنك الأهلي السعودي',   10),
    ('ALRAJHI',      'Al Rajhi Bank',                   'مصرف الراجحي',          11),
    ('OTHER_LISTED', 'Other Listed Company',            'شركة مدرجة أخرى',       12),
    ('OTHER',        'Other',                           'أخرى',                  13)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'employer_name' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 10. Collateral Type
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('REAL_ESTATE',       'Real Estate',       'عقار',            1),
    ('VEHICLE',           'Vehicle',           'مركبة',           2),
    ('DEPOSIT',           'Cash Deposit',      'وديعة نقدية',     3),
    ('SALARY_ASSIGNMENT', 'Salary Assignment', 'تحويل راتب',      4),
    ('GOLD',              'Gold',              'ذهب',             5),
    ('SHARES',            'Shares / Stocks',   'أسهم',            6),
    ('NONE',              'None',              'بدون ضمان',       7)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'collateral_type' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 11. Property Type
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('VILLA',      'Villa',               'فيلا',         1),
    ('APARTMENT',  'Apartment',           'شقة',          2),
    ('TOWNHOUSE',  'Townhouse',           'تاون هاوس',    3),
    ('DUPLEX',     'Duplex',              'دوبلكس',       4),
    ('LAND',       'Land',                'أرض',          5),
    ('COMMERCIAL', 'Commercial Property', 'عقار تجاري',   6)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'property_type' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 12. City / Region
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('RIYADH',   'Riyadh',   'الرياض',       1),
    ('JEDDAH',   'Jeddah',   'جدة',          2),
    ('MAKKAH',   'Makkah',   'مكة المكرمة',  3),
    ('MADINAH',  'Madinah',  'المدينة المنورة', 4),
    ('DAMMAM',   'Dammam',   'الدمام',        5),
    ('KHOBAR',   'Khobar',   'الخبر',         6),
    ('DHAHRAN',  'Dhahran',  'الظهران',       7),
    ('JUBAIL',   'Jubail',   'الجبيل',        8),
    ('TABUK',    'Tabuk',    'تبوك',          9),
    ('ABHA',     'Abha',     'أبها',         10),
    ('TAIF',     'Taif',     'الطائف',       11),
    ('HAIL',     'Hail',     'حائل',        12),
    ('JAZAN',    'Jazan',    'جازان',        13),
    ('NAJRAN',   'Najran',   'نجران',        14),
    ('BAHA',     'Al Baha',  'الباحة',       15),
    ('QASSIM',   'Qassim',   'القصيم',       16),
    ('SAKAKA',   'Sakaka',   'سكاكا',        17),
    ('ARAR',     'Arar',     'عرعر',         18),
    ('OTHER',    'Other',    'أخرى',         19)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'city' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 13. Previous Defaults (BOOLEAN)
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('true',  'Yes - Has Defaults',  'نعم - يوجد تعثرات', 1),
    ('false', 'No - Clean Record',   'لا - سجل نظيف',    2)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'previous_defaults' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 14. Guarantor Available (BOOLEAN)
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('true',  'Yes - Available',     'نعم - متوفر',     1),
    ('false', 'No - Not Available',  'لا - غير متوفر',  2)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'guarantor_available' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';

-- 15. Salary Transfer To Bank (BOOLEAN)
INSERT INTO credit_scoring_field_options (tenant_id, field_definition_id, option_key, label_en, label_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', id, v.option_key, v.label_en, v.label_ar, v.sort_order
FROM credit_scoring_field_definitions d,
(VALUES
    ('true',  'Yes - Transferred',     'نعم - محول',       1),
    ('false', 'No - Not Transferred',  'لا - غير محول',    2)
) AS v(option_key, label_en, label_ar, sort_order)
WHERE d.field_key = 'salary_transfer_bank' AND d.tenant_id = '00000000-0000-0000-0000-000000000001';
