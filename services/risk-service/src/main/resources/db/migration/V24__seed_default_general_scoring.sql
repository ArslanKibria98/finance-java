-- ============================================================================
-- V24: Seed DEFAULT general (onboarding) credit scoring criteria + rules
-- ============================================================================
-- These fields are ONLY the ones onboarding can collect during the workflow
-- (Nafath / Yakeen / GOSI salary / EDD form). Product-specific fields like
-- SIMAH, DBR percentage, collateral, loan amount/tenure live in the
-- per-product scoring tables (product_credit_scoring_criteria) — NOT here.

DO $$
DECLARE
    v_tenant      UUID := '00000000-0000-0000-0000-000000000001';
    -- field definitions
    v_gender      UUID;
    v_nationality UUID;
    v_age         UUID;
    v_city        UUID;
    v_marital     UUID;
    v_education   UUID;
    v_emp_type    UUID;
    v_emp_sector  UUID;
    v_employer    UUID;
    v_salary      UUID;
    v_total_inc   UUID;
    v_yrs_emp     UUID;
    v_sof         UUID;
    v_sow         UUID;
    v_nwr         UUID;
    v_occ         UUID;
    -- criteria placeholders
    v_crit        UUID;
BEGIN
    -- Resolve field_definition ids
    SELECT id INTO v_gender      FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='gender';
    SELECT id INTO v_nationality FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='nationality';
    SELECT id INTO v_age         FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='age';
    SELECT id INTO v_city        FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='city';
    SELECT id INTO v_marital     FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='marital_status';
    SELECT id INTO v_education   FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='education_level';
    SELECT id INTO v_emp_type    FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='employment_type';
    SELECT id INTO v_emp_sector  FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='employment_sector';
    SELECT id INTO v_employer    FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='employer_name';
    SELECT id INTO v_salary      FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='salary';
    SELECT id INTO v_total_inc   FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='total_income';
    SELECT id INTO v_yrs_emp     FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='years_of_employment';
    SELECT id INTO v_sof         FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='source_of_funds';
    SELECT id INTO v_sow         FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='source_of_wealth';
    SELECT id INTO v_nwr         FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='net_worth_range';
    SELECT id INTO v_occ         FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='occupation';

    -- Clean slate (idempotent re-runs)
    DELETE FROM general_credit_scoring_criteria WHERE tenant_id = v_tenant;

    -- ===== 1. GENDER (sort 1) =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_gender, 'Gender', false, true, 1)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'MALE',   2.0, 10),
        (v_tenant, v_crit, 'EQ', 'FEMALE', 2.0, 10);

    -- ===== 2. NATIONALITY (sort 2) =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_nationality, 'Nationality', false, true, 2)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'SAUDI',   3.0, 20),
        (v_tenant, v_crit, 'EQ', 'GCC',     2.5, 15),
        (v_tenant, v_crit, 'EQ', 'ARAB',    2.0, 12),
        (v_tenant, v_crit, 'EQ', 'ASIAN',   1.5, 10),
        (v_tenant, v_crit, 'EQ', 'WESTERN', 2.0, 12),
        (v_tenant, v_crit, 'EQ', 'OTHER',   1.0, 8);

    -- ===== 3. AGE (sort 3) =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_age, 'Age Of Customer', false, true, 3)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'BETWEEN', '21,30', 2.0, 12),
        (v_tenant, v_crit, 'BETWEEN', '31,40', 2.5, 18),
        (v_tenant, v_crit, 'BETWEEN', '41,50', 3.0, 22),
        (v_tenant, v_crit, 'BETWEEN', '51,60', 2.5, 20),
        (v_tenant, v_crit, 'GT',      '60',    1.5, 10);

    -- ===== 4. CITY (sort 4) =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_city, 'City / Region', false, true, 4)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'RIYADH',  2.5, 15),
        (v_tenant, v_crit, 'EQ', 'JEDDAH',  2.5, 15),
        (v_tenant, v_crit, 'EQ', 'MAKKAH',  2.0, 12),
        (v_tenant, v_crit, 'EQ', 'MADINAH', 2.0, 12),
        (v_tenant, v_crit, 'EQ', 'DAMMAM',  2.0, 12),
        (v_tenant, v_crit, 'EQ', 'KHOBAR',  2.0, 12),
        (v_tenant, v_crit, 'EQ', 'OTHER',   1.5, 8);

    -- ===== 5. MARITAL STATUS (sort 5) — EDD =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_marital, 'Marital Status', false, true, 5)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'MARRIED',  2.5, 15),
        (v_tenant, v_crit, 'EQ', 'SINGLE',   2.0, 12),
        (v_tenant, v_crit, 'EQ', 'DIVORCED', 1.5, 10),
        (v_tenant, v_crit, 'EQ', 'WIDOWED',  1.5, 10);

    -- ===== 6. EDUCATION LEVEL (sort 6) — EDD =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_education, 'Education Level', false, true, 6)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'POSTGRADUATE',      3.0, 20),
        (v_tenant, v_crit, 'EQ', 'BACHELOR',          2.5, 18),
        (v_tenant, v_crit, 'EQ', 'DIPLOMA',           2.0, 12),
        (v_tenant, v_crit, 'EQ', 'HIGH_SCHOOL',       1.5, 10),
        (v_tenant, v_crit, 'EQ', 'BELOW_HIGH_SCHOOL', 1.0, 5);

    -- ===== 7. EMPLOYMENT TYPE (sort 7) =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_emp_type, 'Employment Type', false, true, 7)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'GOVERNMENT',      3.5, 22),
        (v_tenant, v_crit, 'EQ', 'MILITARY',        3.0, 20),
        (v_tenant, v_crit, 'EQ', 'SEMI_GOVERNMENT', 3.0, 18),
        (v_tenant, v_crit, 'EQ', 'PRIVATE_SECTOR',  2.5, 15),
        (v_tenant, v_crit, 'EQ', 'SELF_EMPLOYED',   1.5, 8),
        (v_tenant, v_crit, 'EQ', 'RETIRED',         2.0, 12);

    -- ===== 8. EMPLOYMENT SECTOR (sort 8) =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_emp_sector, 'Employment Sector', false, true, 8)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'OIL_AND_GAS',     3.0, 20),
        (v_tenant, v_crit, 'EQ', 'BANKING_FINANCE', 2.5, 18),
        (v_tenant, v_crit, 'EQ', 'HEALTHCARE',      2.5, 15),
        (v_tenant, v_crit, 'EQ', 'EDUCATION',       2.5, 15),
        (v_tenant, v_crit, 'EQ', 'TELECOM',         2.5, 15),
        (v_tenant, v_crit, 'EQ', 'TECHNOLOGY',      2.5, 15),
        (v_tenant, v_crit, 'EQ', 'CONSTRUCTION',    2.0, 12),
        (v_tenant, v_crit, 'EQ', 'RETAIL',          1.5, 10),
        (v_tenant, v_crit, 'EQ', 'OTHER',           1.0, 5);

    -- ===== 9. EMPLOYER NAME (sort 9) — from GOSI =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_employer, 'Employer Name', false, true, 9)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'ARAMCO',       3.5, 25),
        (v_tenant, v_crit, 'EQ', 'SABIC',        3.0, 22),
        (v_tenant, v_crit, 'EQ', 'STC',          2.5, 18),
        (v_tenant, v_crit, 'EQ', 'SEC',          2.5, 18),
        (v_tenant, v_crit, 'EQ', 'MOH',          3.0, 22),
        (v_tenant, v_crit, 'EQ', 'MOE',          3.0, 22),
        (v_tenant, v_crit, 'EQ', 'SNB',          3.0, 22),
        (v_tenant, v_crit, 'EQ', 'ALRAJHI',      3.0, 22),
        (v_tenant, v_crit, 'EQ', 'OTHER_LISTED', 2.0, 12),
        (v_tenant, v_crit, 'EQ', 'OTHER',        1.5, 8);

    -- ===== 10. SALARY (sort 10) — from GOSI =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_salary, 'Salary', false, true, 10)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'BETWEEN', '3000,7999',   1.5, 10),
        (v_tenant, v_crit, 'BETWEEN', '8000,14999',  2.5, 18),
        (v_tenant, v_crit, 'BETWEEN', '15000,29999', 3.0, 22),
        (v_tenant, v_crit, 'GTE',     '30000',       3.5, 25);

    -- ===== 11. TOTAL INCOME (sort 11) — from GOSI =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_total_inc, 'Total Income', false, true, 11)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'BETWEEN', '3000,9999',   1.5, 10),
        (v_tenant, v_crit, 'BETWEEN', '10000,19999', 2.5, 18),
        (v_tenant, v_crit, 'BETWEEN', '20000,39999', 3.0, 22),
        (v_tenant, v_crit, 'GTE',     '40000',       3.5, 25);

    -- ===== 12. YEARS OF EMPLOYMENT (sort 12) — derived from GOSI =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_yrs_emp, 'Years Of Employment', false, true, 12)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'LT',      '1',     1.0, 5),
        (v_tenant, v_crit, 'BETWEEN', '1,3',   1.5, 10),
        (v_tenant, v_crit, 'BETWEEN', '4,7',   2.5, 18),
        (v_tenant, v_crit, 'BETWEEN', '8,15',  3.0, 22),
        (v_tenant, v_crit, 'GT',      '15',    3.5, 25);

    -- ===== 13. SOURCE OF FUNDS (sort 13) — from EDD =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_sof, 'Source Of Funds', false, true, 13)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'EMPLOYMENT_SALARY',  3.0, 22),
        (v_tenant, v_crit, 'EQ', 'BUSINESS_REVENUE',   2.5, 18),
        (v_tenant, v_crit, 'EQ', 'SAVINGS',            2.5, 15),
        (v_tenant, v_crit, 'EQ', 'INVESTMENT_RETURNS', 2.0, 12),
        (v_tenant, v_crit, 'EQ', 'PROPERTY_SALE',      1.5, 10),
        (v_tenant, v_crit, 'EQ', 'INHERITANCE_FUNDS',  1.5, 10),
        (v_tenant, v_crit, 'EQ', 'LOAN_PROCEEDS',      0.5, 3),
        (v_tenant, v_crit, 'EQ', 'GIFT',               1.0, 5),
        (v_tenant, v_crit, 'EQ', 'CRYPTO_PROCEEDS',    0.5, 2);

    -- ===== 14. SOURCE OF WEALTH (sort 14) — from EDD =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_sow, 'Source Of Wealth', false, true, 14)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'BUSINESS_INCOME',    3.0, 22),
        (v_tenant, v_crit, 'EQ', 'REAL_ESTATE',        3.0, 20),
        (v_tenant, v_crit, 'EQ', 'INVESTMENTS',        2.5, 18),
        (v_tenant, v_crit, 'EQ', 'RETIREMENT_PENSION', 2.0, 12),
        (v_tenant, v_crit, 'EQ', 'INHERITANCE',        1.5, 10),
        (v_tenant, v_crit, 'EQ', 'FREELANCE_INCOME',   1.5, 10),
        (v_tenant, v_crit, 'EQ', 'GOVERNMENT_GRANTS',  1.0, 5),
        (v_tenant, v_crit, 'EQ', 'OTHER',              1.0, 5),
        (v_tenant, v_crit, 'EQ', 'CRYPTO_TRADING',     0.5, 2),
        (v_tenant, v_crit, 'EQ', 'DIGITAL_ASSETS',     0.5, 2);

    -- ===== 15. NET WORTH RANGE (sort 15) — from EDD =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_nwr, 'Net Worth Range', false, true, 15)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'OVER_10M',         4.0, 25),
        (v_tenant, v_crit, 'EQ', 'RANGE_5M_10M',     3.5, 22),
        (v_tenant, v_crit, 'EQ', 'RANGE_1M_5M',     3.0, 20),
        (v_tenant, v_crit, 'EQ', 'RANGE_500K_1M',    2.5, 15),
        (v_tenant, v_crit, 'EQ', 'RANGE_100K_500K',  2.0, 12),
        (v_tenant, v_crit, 'EQ', 'RANGE_50K_100K',   1.5, 8),
        (v_tenant, v_crit, 'EQ', 'UNDER_50K',        1.0, 5);

    -- ===== 16. OCCUPATION (sort 16) — from EDD =====
    INSERT INTO general_credit_scoring_criteria (tenant_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_occ, 'Occupation', false, true, 16)
    RETURNING id INTO v_crit;
    INSERT INTO general_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_crit, 'EQ', 'EMPLOYED_PUBLIC',  3.5, 22),
        (v_tenant, v_crit, 'EQ', 'EMPLOYED_PRIVATE', 3.0, 20),
        (v_tenant, v_crit, 'EQ', 'CONSULTANT_IT',    2.5, 18),
        (v_tenant, v_crit, 'EQ', 'SELF_EMPLOYED',    2.0, 12),
        (v_tenant, v_crit, 'EQ', 'RETIRED',          2.0, 10),
        (v_tenant, v_crit, 'EQ', 'OTHER',            1.0, 5),
        (v_tenant, v_crit, 'EQ', 'STUDENT',          0.5, 3),
        (v_tenant, v_crit, 'EQ', 'UNEMPLOYED',       0.5, 2);

END $$;
