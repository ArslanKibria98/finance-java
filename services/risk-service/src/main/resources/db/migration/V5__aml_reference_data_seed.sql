-- V5__aml_reference_data_seed.sql
-- Seed AML reference data from EastNets AML Risk Schema (Individuals Schema)
-- Default tenant: system-wide seed (tenant_id = '00000000-0000-0000-0000-000000000001')
-- Each tenant can override/customize via admin APIs

-- Using a fixed seed tenant for default data
-- Services will copy to tenant-specific rows on first use

DO $$
DECLARE
    seed_tenant UUID := '00000000-0000-0000-0000-000000000001';
    cat_pep UUID;
    cat_internal UUID;
    cat_nationality UUID;
    cat_geo UUID;
    cat_product UUID;
    cat_occupation UUID;
    cat_income UUID;
    cat_source UUID;
BEGIN

-- =====================================================================
-- 1. Risk Categories (from Individuals Schema)
-- =====================================================================
INSERT INTO aml_risk_categories (id, tenant_id, category_code, name_en, name_ar, category_type, weight, sort_order)
VALUES
    (gen_random_uuid(), seed_tenant, 'PEP', 'Politically Exposed Person', 'شخص مكشوف سياسياً', 'DOMINANT', 17, 1),
    (gen_random_uuid(), seed_tenant, 'INTERNAL_LIST', 'Internal List Nationalities by AWN', 'القائمة الداخلية للجنسيات', 'DOMINANT', 17, 2),
    (gen_random_uuid(), seed_tenant, 'NATIONALITY', 'Nationality', 'الجنسية', 'MUTUAL_EXCLUSIVE', 16, 3),
    (gen_random_uuid(), seed_tenant, 'GEOGRAPHICAL_LOCATION', 'Geographical Location (City)', 'الموقع الجغرافي (المدينة)', 'MUTUAL_EXCLUSIVE', 3, 4),
    (gen_random_uuid(), seed_tenant, 'PRODUCT_SERVICES', 'Product and Services', 'المنتجات والخدمات', 'MUTUAL_EXCLUSIVE', 2, 5),
    (gen_random_uuid(), seed_tenant, 'OCCUPATIONS', 'Occupations', 'المهن', 'MUTUAL_EXCLUSIVE', 20, 6),
    (gen_random_uuid(), seed_tenant, 'INCOME_RANGE', 'Income Range', 'نطاق الدخل', 'MUTUAL_EXCLUSIVE', 15, 7),
    (gen_random_uuid(), seed_tenant, 'SOURCE_OF_INCOME', 'Source of Income', 'مصدر الدخل', 'MUTUAL_EXCLUSIVE', 10, 8);

-- Get category IDs for factor insertion
SELECT id INTO cat_pep FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'PEP';
SELECT id INTO cat_internal FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'INTERNAL_LIST';
SELECT id INTO cat_nationality FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'NATIONALITY';
SELECT id INTO cat_geo FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'GEOGRAPHICAL_LOCATION';
SELECT id INTO cat_product FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'PRODUCT_SERVICES';
SELECT id INTO cat_occupation FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'OCCUPATIONS';
SELECT id INTO cat_income FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'INCOME_RANGE';
SELECT id INTO cat_source FROM aml_risk_categories WHERE tenant_id = seed_tenant AND category_code = 'SOURCE_OF_INCOME';

-- =====================================================================
-- 2. Category Factors
-- =====================================================================

-- PEP factors (Dominant — auto HIGH if YES)
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_pep, 'PEP_YES', 'PEP Detected', 'شخص مكشوف سياسياً', 100, 17, 1),
    (seed_tenant, cat_pep, 'PEP_NO', 'Not PEP', 'غير مكشوف سياسياً', 0, 0, 2);

-- Internal List factors (Dominant)
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_internal, 'INTERNAL_LIST_YES', 'On Internal List', 'في القائمة الداخلية', 100, 17, 1),
    (seed_tenant, cat_internal, 'INTERNAL_LIST_NO', 'Not On Internal List', 'غير موجود في القائمة', 0, 0, 2);

-- Nationality factors
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_nationality, 'FATF_HIGH_RISK', 'FATF High-Risk Country', 'دولة عالية المخاطر', 100, 16, 1),
    (seed_tenant, cat_nationality, 'REST_OF_WORLD', 'Rest of the World', 'بقية العالم', 37.5, 6, 2),
    (seed_tenant, cat_nationality, 'SAUDI_GCC', 'Saudi / GCC', 'سعودي / مجلس التعاون', 25, 4, 3);

-- Geographical Location factors
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_geo, 'HR_REGION', 'High-Risk Region', 'منطقة عالية المخاطر', 100, 3, 1),
    (seed_tenant, cat_geo, 'MR_REGION', 'Medium-Risk Region', 'منطقة متوسطة المخاطر', 66, 1.98, 2),
    (seed_tenant, cat_geo, 'LR_REGION', 'Low-Risk Region', 'منطقة منخفضة المخاطر', 33, 0.99, 3);

-- Product & Services factors
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_product, 'HR_PRODUCT', 'High-Risk Product', 'منتج عالي المخاطر', 100, 2, 1),
    (seed_tenant, cat_product, 'MR_PRODUCT', 'Medium-Risk Product', 'منتج متوسط المخاطر', 50, 1, 2),
    (seed_tenant, cat_product, 'LR_PRODUCT', 'Low-Risk Product', 'منتج منخفض المخاطر', 50, 1, 3);

-- Occupations factors
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_occupation, 'HR_OCCUPATION', 'High-Risk Occupation', 'مهنة عالية المخاطر', 100, 20, 1),
    (seed_tenant, cat_occupation, 'MR_OCCUPATION', 'Medium-Risk Occupation', 'مهنة متوسطة المخاطر', 75, 15, 2),
    (seed_tenant, cat_occupation, 'LR_OCCUPATION', 'Low-Risk Occupation', 'مهنة منخفضة المخاطر', 25, 5, 3);

-- Income Range factors
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_income, 'INCOME_0_3000', '3000 or less', '3000 أو أقل', 20, 3, 1),
    (seed_tenant, cat_income, 'INCOME_3001_6000', '3001 - 6000', '3001 - 6000', 20, 3, 2),
    (seed_tenant, cat_income, 'INCOME_6001_10000', '6001 - 10000', '6001 - 10000', 40, 6, 3),
    (seed_tenant, cat_income, 'INCOME_10001_20000', '10001 - 20000', '10001 - 20000', 40, 6, 4),
    (seed_tenant, cat_income, 'INCOME_20001_30000', '20001 - 30000', '20001 - 30000', 60, 9, 5),
    (seed_tenant, cat_income, 'INCOME_30001_40000', '30001 - 40000', '30001 - 40000', 60, 9, 6),
    (seed_tenant, cat_income, 'INCOME_40001_60000', '40001 - 60000', '40001 - 60000', 80, 12, 7),
    (seed_tenant, cat_income, 'INCOME_60001_80000', '60001 - 80000', '60001 - 80000', 80, 12, 8),
    (seed_tenant, cat_income, 'INCOME_80000_ABOVE', '80000 and above', '80000 وأكثر', 100, 15, 9);

-- Source of Income factors
INSERT INTO aml_risk_category_factors (tenant_id, category_id, factor_code, name_en, name_ar, factor_weight_pct, computed_rating, sort_order)
VALUES
    (seed_tenant, cat_source, 'SALARY', 'Salary', 'راتب', 20, 2, 1),
    (seed_tenant, cat_source, 'REAL_ESTATE', 'Real Estate', 'عقارات', 100, 10, 2),
    (seed_tenant, cat_source, 'INVESTMENT_RETURNS', 'Investment Returns', 'عوائد استثمارية', 60, 6, 3),
    (seed_tenant, cat_source, 'RENTAL_INCOME', 'Rental Income', 'دخل ايجارات', 40, 4, 4),
    (seed_tenant, cat_source, 'BUSINESS_RETURNS', 'Business Returns', 'اعمال تجارية', 60, 6, 5);

-- =====================================================================
-- 3. Risk Thresholds
-- =====================================================================
INSERT INTO aml_risk_thresholds (tenant_id, risk_level, min_score, max_score, description)
VALUES
    (seed_tenant, 'HIGH', 42, 999.99, 'High Risk - Trigger EDD / Enhanced Due Diligence'),
    (seed_tenant, 'MEDIUM', 31, 41.99, 'Medium Risk - Flag for review'),
    (seed_tenant, 'LOW', 0, 30.99, 'Low Risk - Auto-proceed');

-- =====================================================================
-- 4. FATF Countries (October 2024)
-- =====================================================================

-- High-Risk Jurisdictions (Call for Action)
INSERT INTO aml_fatf_countries (tenant_id, country_code, country_name, fatf_category)
VALUES
    (seed_tenant, 'PRK', 'Democratic People''s Republic of Korea', 'HIGH_RISK'),
    (seed_tenant, 'IRN', 'Iran', 'HIGH_RISK');

-- Increased Monitoring Jurisdictions
INSERT INTO aml_fatf_countries (tenant_id, country_code, country_name, fatf_category)
VALUES
    (seed_tenant, 'DZA', 'Algeria', 'INCREASED_MONITORING'),
    (seed_tenant, 'AGO', 'Angola', 'INCREASED_MONITORING'),
    (seed_tenant, 'BOL', 'Bolivia', 'INCREASED_MONITORING'),
    (seed_tenant, 'BGR', 'Bulgaria', 'INCREASED_MONITORING'),
    (seed_tenant, 'BFA', 'Burkina Faso', 'INCREASED_MONITORING'),
    (seed_tenant, 'CMR', 'Cameroon', 'INCREASED_MONITORING'),
    (seed_tenant, 'CIV', 'Cote d''Ivoire', 'INCREASED_MONITORING'),
    (seed_tenant, 'COD', 'Democratic Republic of Congo', 'INCREASED_MONITORING'),
    (seed_tenant, 'HTI', 'Haiti', 'INCREASED_MONITORING'),
    (seed_tenant, 'KEN', 'Kenya', 'INCREASED_MONITORING'),
    (seed_tenant, 'LAO', 'Lao People''s Democratic Republic', 'INCREASED_MONITORING'),
    (seed_tenant, 'LBN', 'Lebanon', 'INCREASED_MONITORING'),
    (seed_tenant, 'MCO', 'Monaco', 'INCREASED_MONITORING'),
    (seed_tenant, 'MOZ', 'Mozambique', 'INCREASED_MONITORING'),
    (seed_tenant, 'NAM', 'Namibia', 'INCREASED_MONITORING'),
    (seed_tenant, 'NPL', 'Nepal', 'INCREASED_MONITORING'),
    (seed_tenant, 'NGA', 'Nigeria', 'INCREASED_MONITORING'),
    (seed_tenant, 'ZAF', 'South Africa', 'INCREASED_MONITORING'),
    (seed_tenant, 'SSD', 'South Sudan', 'INCREASED_MONITORING'),
    (seed_tenant, 'SYR', 'Syria', 'INCREASED_MONITORING'),
    (seed_tenant, 'VEN', 'Venezuela', 'INCREASED_MONITORING'),
    (seed_tenant, 'VNM', 'Vietnam', 'INCREASED_MONITORING'),
    (seed_tenant, 'VGB', 'Virgin Islands (British)', 'INCREASED_MONITORING'),
    (seed_tenant, 'YEM', 'Yemen', 'INCREASED_MONITORING'),
    (seed_tenant, 'MMR', 'Myanmar', 'INCREASED_MONITORING');

-- GCC Countries (LOW risk — Saudi & GCC factor)
INSERT INTO aml_fatf_countries (tenant_id, country_code, country_name, fatf_category)
VALUES
    (seed_tenant, 'SAU', 'Saudi Arabia', 'STANDARD'),
    (seed_tenant, 'ARE', 'United Arab Emirates', 'STANDARD'),
    (seed_tenant, 'KWT', 'Kuwait', 'STANDARD'),
    (seed_tenant, 'BHR', 'Bahrain', 'STANDARD'),
    (seed_tenant, 'QAT', 'Qatar', 'STANDARD'),
    (seed_tenant, 'OMN', 'Oman', 'STANDARD');

-- =====================================================================
-- 5. Income Range Scores (from Excel KYC sheet)
-- =====================================================================
INSERT INTO aml_income_range_scores (tenant_id, range_min, range_max, label_en, label_ar, factor_weight_pct, sort_order)
VALUES
    (seed_tenant, 0, 3000, '3000 or less', '3000 أو أقل', 20, 1),
    (seed_tenant, 3001, 6000, '3001 - 6000', '3001 - 6000', 20, 2),
    (seed_tenant, 6001, 10000, '6001 - 10000', '6001 - 10000', 40, 3),
    (seed_tenant, 10001, 20000, '10001 - 20000', '10001 - 20000', 40, 4),
    (seed_tenant, 20001, 30000, '20001 - 30000', '20001 - 30000', 60, 5),
    (seed_tenant, 30001, 40000, '30001 - 40000', '30001 - 40000', 60, 6),
    (seed_tenant, 40001, 60000, '40001 - 60000', '40001 - 60000', 80, 7),
    (seed_tenant, 60001, 80000, '60001 - 80000', '60001 - 80000', 80, 8),
    (seed_tenant, 80001, NULL, '80000 and above', '80000 وأكثر', 100, 9);

-- =====================================================================
-- 6. Source of Income Scores (from Excel KYC sheet)
-- =====================================================================
INSERT INTO aml_source_of_income_scores (tenant_id, source_code, name_en, name_ar, factor_weight_pct, sort_order)
VALUES
    (seed_tenant, 'SALARY', 'Salary', 'راتب', 20, 1),
    (seed_tenant, 'REAL_ESTATE', 'Real Estate', 'عقارات', 100, 2),
    (seed_tenant, 'INVESTMENT_RETURNS', 'Investment Returns', 'عوائد استثمارية', 60, 3),
    (seed_tenant, 'RENTAL_INCOME', 'Rental Income', 'دخل ايجارات', 40, 4),
    (seed_tenant, 'BUSINESS_RETURNS', 'Business Returns', 'اعمال تجارية', 60, 5);

-- =====================================================================
-- 7. Sample High-Risk Occupations (PEP + HIGH from Excel)
-- =====================================================================

-- PEP Occupations (16 from Excel)
INSERT INTO aml_occupation_risk_levels (tenant_id, occupation_code, description_ar, description_en, risk_level)
VALUES
    (seed_tenant, '1121011', 'أمير منطقة', 'Prince of Province (Emir of a Region)', 'PEP'),
    (seed_tenant, '1121031', 'رئيس بلدية أو مجتمع قرية', 'Head of Municipality or Village Community', 'PEP'),
    (seed_tenant, '1112011', 'سفير دبلوماسي', 'Diplomatic Ambassador', 'PEP'),
    (seed_tenant, '2433011', 'قاضي', 'Judge', 'PEP'),
    (seed_tenant, '2433021', 'قاضي شرعي', 'Legitimate (Sharia) Judge', 'PEP'),
    (seed_tenant, '1135011', 'قنصل', 'Consul', 'PEP'),
    (seed_tenant, '1135021', 'قنصل عام', 'Consul General', 'PEP'),
    (seed_tenant, '1121021', 'محافظ', 'Governor of Province', 'PEP'),
    (seed_tenant, '2433031', 'نائب عام', 'Attorney General', 'PEP'),
    (seed_tenant, '1135051', 'مساعد قنصل', 'Assistant Consul', 'PEP'),
    (seed_tenant, '1135181', 'ملحق دبلوماسي مساعد', 'Assistant Diplomatic Attache', 'PEP'),
    (seed_tenant, '1135041', 'نائب قنصل', 'Vice-Consul', 'PEP'),
    (seed_tenant, '1135031', 'نائب قنصل عام', 'Deputy Consul General', 'PEP'),
    (seed_tenant, '1135061', 'وزير مفوض أ', 'Minister Plenipotentiary (A)', 'PEP'),
    (seed_tenant, '1135071', 'وزير مفوض ب', 'Minister Plenipotentiary (B)', 'PEP'),
    (seed_tenant, '1111011', 'وكيل وزارة', 'Undersecretary', 'PEP');

-- High-Risk Occupations (sample from Excel)
INSERT INTO aml_occupation_risk_levels (tenant_id, occupation_code, description_ar, description_en, risk_level)
VALUES
    (seed_tenant, '7313011', 'صائغ ذهب وفضة', 'Gold and Silver Jewellery Seller/Smith', 'HIGH'),
    (seed_tenant, '5223041', 'بائع مجوهرات', 'Jeweller', 'HIGH'),
    (seed_tenant, '5221011', 'بائع تحف', 'Heritage Products Seller', 'HIGH'),
    (seed_tenant, '3334021', 'سمسار عقارات', 'Land Broker', 'HIGH'),
    (seed_tenant, '1114011', 'شيخ قبيلة', 'Tribe Sheikh', 'HIGH'),
    (seed_tenant, '4211011', 'أمين صندوق', 'Treasurer', 'HIGH'),
    (seed_tenant, '3411011', 'كاتب عدل', 'Notary', 'HIGH'),
    (seed_tenant, '2611011', 'محامي', 'Lawyer', 'HIGH'),
    (seed_tenant, '2411011', 'مراجع حسابات', 'Auditor', 'HIGH'),
    (seed_tenant, '3339031', 'مرمز جمركي', 'Customs Coder', 'HIGH'),
    (seed_tenant, '3412011', 'مأذون شرعي', 'Marriage Official', 'HIGH'),
    (seed_tenant, '1211061', 'مدير التقارير المالية', 'Financial Reports Director', 'HIGH'),
    (seed_tenant, '0001011', 'فرد من أفراد القبائل', 'Tribes Member', 'HIGH'),
    (seed_tenant, '2413011', 'محلل مالي', 'Financial Analyst', 'HIGH'),
    (seed_tenant, '3311011', 'وسيط مالي', 'Financial Broker', 'HIGH');

-- Medium-Risk Occupations (sample from Excel)
INSERT INTO aml_occupation_risk_levels (tenant_id, occupation_code, description_ar, description_en, risk_level)
VALUES
    (seed_tenant, '5220011', 'بائع', 'Seller', 'MEDIUM'),
    (seed_tenant, '1120011', 'تاجر', 'Merchant', 'MEDIUM'),
    (seed_tenant, '1120021', 'رئيس مجلس إدارة شركة', 'Company Chairman', 'MEDIUM'),
    (seed_tenant, '1120031', 'مدير تنفيذي', 'Executive Director', 'MEDIUM'),
    (seed_tenant, '1346011', 'مدير بنك', 'Bank Manager', 'MEDIUM'),
    (seed_tenant, '2411021', 'محاسب', 'Accountant', 'MEDIUM'),
    (seed_tenant, '2612011', 'مستشار قانوني', 'Legal Advisor', 'MEDIUM'),
    (seed_tenant, '3342011', 'كاتب محكمة', 'Court Clerk', 'MEDIUM');

-- Low-Risk Occupations (representative sample)
INSERT INTO aml_occupation_risk_levels (tenant_id, occupation_code, description_ar, description_en, risk_level)
VALUES
    (seed_tenant, '2310011', 'أستاذ جامعي', 'University Professor', 'LOW'),
    (seed_tenant, '2211011', 'طبيب', 'Doctor', 'LOW'),
    (seed_tenant, '2512011', 'مهندس برمجيات', 'Software Engineer', 'LOW'),
    (seed_tenant, '2341011', 'معلم', 'Teacher', 'LOW'),
    (seed_tenant, '5311011', 'عامل منزلي', 'Domestic Worker', 'LOW'),
    (seed_tenant, '8342011', 'سائق', 'Driver', 'LOW'),
    (seed_tenant, '9112011', 'عامل نظافة', 'Cleaner', 'LOW'),
    (seed_tenant, '5132011', 'نادل', 'Waiter', 'LOW');

END $$;
