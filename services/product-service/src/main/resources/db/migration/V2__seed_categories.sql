-- ============================================================================
-- V2: Seed Master Categories, Sub-Categories, and Credit Scoring Field Definitions
-- ============================================================================

-- Default tenant ID for seed data (will be replaced per deployment)
DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';

    -- Master category IDs
    murabaha_id UUID;
    ijarah_id UUID;
    tawarruq_id UUID;
    bnpl_id UUID;
    crowd_funding_id UUID;
BEGIN

-- ============================================================================
-- MASTER CATEGORIES (5)
-- ============================================================================

INSERT INTO product_master_categories (id, tenant_id, code, name_en, name_ar, description_en, description_ar, sort_order)
VALUES
    (gen_random_uuid(), default_tenant, 'MURABAHA', 'Murabaha', 'مرابحة',
     'Cost-plus financing for asset purchases', 'تمويل التكلفة مع الربح لشراء الأصول', 1),
    (gen_random_uuid(), default_tenant, 'IJARAH', 'Ijarah', 'إجارة',
     'Islamic leasing and rental financing', 'التأجير الإسلامي والتمويل الإيجاري', 2),
    (gen_random_uuid(), default_tenant, 'TAWARRUQ', 'Tawarruq', 'تورق',
     'Monetization-based personal financing', 'التمويل الشخصي القائم على التورق', 3),
    (gen_random_uuid(), default_tenant, 'BNPL', 'Buy Now Pay Later', 'اشتري الآن وادفع لاحقاً',
     'Deferred payment solutions for consumers', 'حلول الدفع المؤجل للمستهلكين', 4),
    (gen_random_uuid(), default_tenant, 'CROWD_FUNDING', 'Crowd Funding', 'التمويل الجماعي',
     'Community-based investment platforms', 'منصات الاستثمار المجتمعي', 5);

-- Retrieve generated IDs
SELECT id INTO murabaha_id FROM product_master_categories WHERE tenant_id = default_tenant AND code = 'MURABAHA';
SELECT id INTO ijarah_id FROM product_master_categories WHERE tenant_id = default_tenant AND code = 'IJARAH';
SELECT id INTO tawarruq_id FROM product_master_categories WHERE tenant_id = default_tenant AND code = 'TAWARRUQ';
SELECT id INTO bnpl_id FROM product_master_categories WHERE tenant_id = default_tenant AND code = 'BNPL';
SELECT id INTO crowd_funding_id FROM product_master_categories WHERE tenant_id = default_tenant AND code = 'CROWD_FUNDING';

-- ============================================================================
-- SUB-CATEGORIES
-- ============================================================================

-- Murabaha sub-categories (4)
INSERT INTO product_sub_categories (tenant_id, master_category_id, code, name_en, name_ar, sort_order)
VALUES
    (default_tenant, murabaha_id, 'HOME_FINANCING', 'Home Financing', 'تمويل المنازل', 1),
    (default_tenant, murabaha_id, 'VEHICLE_FINANCING', 'Vehicle Financing', 'تمويل المركبات', 2),
    (default_tenant, murabaha_id, 'INVENTORY_FINANCING', 'Inventory Financing', 'تمويل المخزون', 3),
    (default_tenant, murabaha_id, 'EQUIPMENT_FINANCING', 'Equipment Financing', 'تمويل المعدات', 4);

-- Ijarah sub-categories (3)
INSERT INTO product_sub_categories (tenant_id, master_category_id, code, name_en, name_ar, sort_order)
VALUES
    (default_tenant, ijarah_id, 'PROPERTY_LEASING', 'Property Leasing', 'تأجير العقارات', 1),
    (default_tenant, ijarah_id, 'VEHICLE_LEASING', 'Vehicle Leasing', 'تأجير المركبات', 2),
    (default_tenant, ijarah_id, 'EQUIPMENT_LEASING', 'Equipment Leasing', 'تأجير المعدات', 3);

-- Tawarruq sub-categories (10)
INSERT INTO product_sub_categories (tenant_id, master_category_id, code, name_en, name_ar, sort_order)
VALUES
    (default_tenant, tawarruq_id, 'PERSONAL_FINANCING', 'Personal Financing', 'التمويل الشخصي (السلعة خاصة في التورق)', 1),
    (default_tenant, tawarruq_id, 'BUSINESS_FINANCING', 'Business Financing', 'تمويل الأعمال', 2),
    (default_tenant, tawarruq_id, 'TRADE_FINANCING', 'Trade Financing', 'تمويل التجارة', 3),
    (default_tenant, tawarruq_id, 'INVESTMENT_FINANCING', 'Investment Financing', 'تمويل الاستثمار', 4),
    (default_tenant, tawarruq_id, 'CASH_MANAGEMENT', 'Cash Management', 'إدارة النقد', 5),
    (default_tenant, tawarruq_id, 'MICROFINANCE', 'Microfinance', 'التمويل الأصغر', 6),
    (default_tenant, tawarruq_id, 'QUICK_CASH', 'Quick Cash', 'النقد السريع', 7),
    (default_tenant, tawarruq_id, 'INVOICE_FACTORING', 'Invoice Factoring', 'تحصيل الفواتير', 8),
    (default_tenant, tawarruq_id, 'INVOICE_DISCOUNTING', 'Invoice Discounting', 'خصم الفواتير', 9),
    (default_tenant, tawarruq_id, 'INCOME_FACTORING', 'Income Factoring', 'التحصيل العكسي', 10);

-- BNPL sub-categories (4)
INSERT INTO product_sub_categories (tenant_id, master_category_id, code, name_en, name_ar, sort_order)
VALUES
    (default_tenant, bnpl_id, 'ECOMMERCE_BNPL', 'E-commerce BNPL', 'اشتري الآن للتجارة الإلكترونية', 1),
    (default_tenant, bnpl_id, 'RETAIL_BNPL', 'Retail BNPL', 'اشتري الآن للتجزئة', 2),
    (default_tenant, bnpl_id, 'HEALTHCARE_BNPL', 'Healthcare BNPL', 'اشتري الآن للرعاية الصحية', 3),
    (default_tenant, bnpl_id, 'EDUCATION_BNPL', 'Education BNPL', 'اشتري الآن للتعليم', 4);

-- Crowd Funding sub-categories (4)
INSERT INTO product_sub_categories (tenant_id, master_category_id, code, name_en, name_ar, sort_order)
VALUES
    (default_tenant, crowd_funding_id, 'REAL_ESTATE_CF', 'Real Estate Crowdfunding', 'التمويل الجماعي العقاري', 1),
    (default_tenant, crowd_funding_id, 'BUSINESS_CF', 'Business Crowdfunding', 'التمويل الجماعي للأعمال', 2),
    (default_tenant, crowd_funding_id, 'PROJECT_CF', 'Project Crowdfunding', 'التمويل الجماعي للمشاريع', 3),
    (default_tenant, crowd_funding_id, 'SOCIAL_CF', 'Social Crowdfunding', 'التمويل الجماعي الاجتماعي', 4);

-- ============================================================================
-- CREDIT SCORING FIELD DEFINITIONS (~40 predefined fields)
-- ============================================================================

INSERT INTO credit_scoring_field_definitions (tenant_id, field_key, name_en, name_ar, data_type, sort_order)
VALUES
    (default_tenant, 'customer_type', 'Customer Type', 'نوع العميل', 'STRING', 1),
    (default_tenant, 'age', 'Age Of Customer', 'عمر العميل', 'NUMERIC', 2),
    (default_tenant, 'gender', 'Gender', 'الجنس', 'STRING', 3),
    (default_tenant, 'nationality', 'Nationality', 'الجنسية', 'STRING', 4),
    (default_tenant, 'residency_type', 'Residency Type', 'نوع الإقامة', 'STRING', 5),
    (default_tenant, 'marital_status', 'Marital Status', 'الحالة الاجتماعية', 'STRING', 6),
    (default_tenant, 'number_of_dependents', 'Number Of Dependents', 'عدد المعالين', 'NUMERIC', 7),
    (default_tenant, 'education_level', 'Education Level', 'المستوى التعليمي', 'STRING', 8),
    (default_tenant, 'salary', 'Salary', 'الراتب', 'NUMERIC', 9),
    (default_tenant, 'basic_salary', 'Basic Salary', 'الراتب الأساسي', 'NUMERIC', 10),
    (default_tenant, 'housing_allowance', 'Housing Allowance', 'بدل السكن', 'NUMERIC', 11),
    (default_tenant, 'transportation_allowance', 'Transportation Allowance', 'بدل النقل', 'NUMERIC', 12),
    (default_tenant, 'other_allowances', 'Other Allowances', 'بدلات أخرى', 'NUMERIC', 13),
    (default_tenant, 'total_income', 'Total Income', 'إجمالي الدخل', 'NUMERIC', 14),
    (default_tenant, 'employment_type', 'Employment Type', 'نوع التوظيف', 'STRING', 15),
    (default_tenant, 'employment_sector', 'Employment Sector', 'قطاع التوظيف', 'STRING', 16),
    (default_tenant, 'employer_name', 'Employer Name', 'اسم جهة العمل', 'STRING', 17),
    (default_tenant, 'years_of_employment', 'Years Of Employment', 'سنوات العمل', 'NUMERIC', 18),
    (default_tenant, 'months_in_current_job', 'Months In Current Job', 'أشهر في الوظيفة الحالية', 'NUMERIC', 19),
    (default_tenant, 'simah_score', 'SIMAH Score', 'درجة سمة', 'NUMERIC', 20),
    (default_tenant, 'simah_defaults', 'SIMAH Defaults Count', 'عدد التعثرات في سمة', 'NUMERIC', 21),
    (default_tenant, 'simah_enquiries', 'SIMAH Enquiries Count', 'عدد الاستعلامات في سمة', 'NUMERIC', 22),
    (default_tenant, 'existing_obligations', 'Existing Obligations', 'الالتزامات الحالية', 'NUMERIC', 23),
    (default_tenant, 'dbr_percentage', 'DBR Percentage', 'نسبة عبء الدين', 'NUMERIC', 24),
    (default_tenant, 'number_of_active_loans', 'Number Of Active Loans', 'عدد القروض النشطة', 'NUMERIC', 25),
    (default_tenant, 'number_of_credit_cards', 'Number Of Credit Cards', 'عدد بطاقات الائتمان', 'NUMERIC', 26),
    (default_tenant, 'loan_amount_requested', 'Loan Amount Requested', 'مبلغ القرض المطلوب', 'NUMERIC', 27),
    (default_tenant, 'loan_tenure_requested', 'Loan Tenure Requested', 'مدة القرض المطلوبة', 'NUMERIC', 28),
    (default_tenant, 'loan_to_value_ratio', 'Loan To Value Ratio', 'نسبة القرض إلى القيمة', 'NUMERIC', 29),
    (default_tenant, 'collateral_type', 'Collateral Type', 'نوع الضمان', 'STRING', 30),
    (default_tenant, 'collateral_value', 'Collateral Value', 'قيمة الضمان', 'NUMERIC', 31),
    (default_tenant, 'property_type', 'Property Type', 'نوع العقار', 'STRING', 32),
    (default_tenant, 'property_value', 'Property Value', 'قيمة العقار', 'NUMERIC', 33),
    (default_tenant, 'down_payment_percentage', 'Down Payment Percentage', 'نسبة الدفعة المقدمة', 'NUMERIC', 34),
    (default_tenant, 'bank_relationship_years', 'Bank Relationship Years', 'سنوات العلاقة مع البنك', 'NUMERIC', 35),
    (default_tenant, 'previous_defaults', 'Previous Defaults', 'التعثرات السابقة', 'BOOLEAN', 36),
    (default_tenant, 'guarantor_available', 'Guarantor Available', 'توفر كفيل', 'BOOLEAN', 37),
    (default_tenant, 'city', 'City / Region', 'المدينة / المنطقة', 'STRING', 38),
    (default_tenant, 'account_balance_avg', 'Average Account Balance', 'متوسط رصيد الحساب', 'NUMERIC', 39),
    (default_tenant, 'salary_transfer_bank', 'Salary Transfer To Bank', 'تحويل الراتب للبنك', 'BOOLEAN', 40);

END $$;
