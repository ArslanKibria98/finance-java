-- Seed credit scoring criteria + rules for Micro Financing product
-- for the 4 EDD fields: source_of_funds, source_of_wealth, net_worth_range, occupation
-- Values mirror customer-service LOV active codes; weights reflect risk profile.

DO $$
DECLARE
    v_tenant      UUID := '00000000-0000-0000-0000-000000000001';
    v_product     UUID := '4210f18e-8c35-4b12-9b7f-c96de95767c7';  -- Micro Financing
    v_sof_def     UUID;
    v_sow_def     UUID;
    v_nwr_def     UUID;
    v_occ_def     UUID;
    v_sof_crit    UUID;
    v_sow_crit    UUID;
    v_nwr_crit    UUID;
    v_occ_crit    UUID;
BEGIN
    SELECT id INTO v_sof_def FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='source_of_funds';
    SELECT id INTO v_sow_def FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='source_of_wealth';
    SELECT id INTO v_nwr_def FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='net_worth_range';
    SELECT id INTO v_occ_def FROM credit_scoring_field_definitions WHERE tenant_id=v_tenant AND field_key='occupation';

    -- ===== 1. SOURCE OF FUNDS (sort 30) =====
    INSERT INTO product_credit_scoring_criteria (tenant_id, product_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_product, v_sof_def, 'Source Of Funds', false, true, 30)
    RETURNING id INTO v_sof_crit;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_sof_crit, 'EQ', 'EMPLOYMENT_SALARY',  3.5, 25),
        (v_tenant, v_sof_crit, 'EQ', 'BUSINESS_REVENUE',   3,   20),
        (v_tenant, v_sof_crit, 'EQ', 'SAVINGS',            3,   18),
        (v_tenant, v_sof_crit, 'EQ', 'INVESTMENT_RETURNS', 2.5, 15),
        (v_tenant, v_sof_crit, 'EQ', 'PROPERTY_SALE',      2,   12),
        (v_tenant, v_sof_crit, 'EQ', 'INHERITANCE_FUNDS',  2,   10),
        (v_tenant, v_sof_crit, 'EQ', 'LOAN_PROCEEDS',      1,   5),
        (v_tenant, v_sof_crit, 'EQ', 'GIFT',               1,   5),
        (v_tenant, v_sof_crit, 'EQ', 'CRYPTO_PROCEEDS',    0.5, 2);

    -- ===== 2. SOURCE OF WEALTH (sort 31) =====
    INSERT INTO product_credit_scoring_criteria (tenant_id, product_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_product, v_sow_def, 'Source Of Wealth', false, true, 31)
    RETURNING id INTO v_sow_crit;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_sow_crit, 'EQ', 'BUSINESS_INCOME',    3.5, 25),
        (v_tenant, v_sow_crit, 'EQ', 'REAL_ESTATE',        3.5, 22),
        (v_tenant, v_sow_crit, 'EQ', 'INVESTMENTS',        3,   20),
        (v_tenant, v_sow_crit, 'EQ', 'RETIREMENT_PENSION', 2.5, 15),
        (v_tenant, v_sow_crit, 'EQ', 'INHERITANCE',        2,   12),
        (v_tenant, v_sow_crit, 'EQ', 'FREELANCE_INCOME',   2,   10),
        (v_tenant, v_sow_crit, 'EQ', 'GOVERNMENT_GRANTS',  1.5, 8),
        (v_tenant, v_sow_crit, 'EQ', 'OTHER',              1,   5),
        (v_tenant, v_sow_crit, 'EQ', 'CRYPTO_TRADING',     0.5, 2),
        (v_tenant, v_sow_crit, 'EQ', 'DIGITAL_ASSETS',     0.5, 2);

    -- ===== 3. NET WORTH RANGE (sort 32) =====
    INSERT INTO product_credit_scoring_criteria (tenant_id, product_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_product, v_nwr_def, 'Net Worth Range', false, true, 32)
    RETURNING id INTO v_nwr_crit;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_nwr_crit, 'EQ', 'OVER_10M',         4,   25),
        (v_tenant, v_nwr_crit, 'EQ', 'RANGE_5M_10M',     3.5, 22),
        (v_tenant, v_nwr_crit, 'EQ', 'RANGE_1M_5M',      3,   20),
        (v_tenant, v_nwr_crit, 'EQ', 'RANGE_500K_1M',    2.5, 15),
        (v_tenant, v_nwr_crit, 'EQ', 'RANGE_100K_500K',  2,   12),
        (v_tenant, v_nwr_crit, 'EQ', 'RANGE_50K_100K',   1.5, 8),
        (v_tenant, v_nwr_crit, 'EQ', 'UNDER_50K',        1,   5);

    -- ===== 4. OCCUPATION (sort 33) =====
    INSERT INTO product_credit_scoring_criteria (tenant_id, product_id, field_definition_id, custom_name, is_custom, is_enabled, sort_order)
    VALUES (v_tenant, v_product, v_occ_def, 'Occupation', false, true, 33)
    RETURNING id INTO v_occ_crit;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight, percentage) VALUES
        (v_tenant, v_occ_crit, 'EQ', 'EMPLOYED_PUBLIC',  3.5, 25),
        (v_tenant, v_occ_crit, 'EQ', 'EMPLOYED_PRIVATE', 3,   20),
        (v_tenant, v_occ_crit, 'EQ', 'CONSULTANT_IT',    2.5, 18),
        (v_tenant, v_occ_crit, 'EQ', 'SELF_EMPLOYED',    2,   12),
        (v_tenant, v_occ_crit, 'EQ', 'RETIRED',          2,   10),
        (v_tenant, v_occ_crit, 'EQ', 'OTHER',            1,   5),
        (v_tenant, v_occ_crit, 'EQ', 'STUDENT',          0.5, 3),
        (v_tenant, v_occ_crit, 'EQ', 'UNEMPLOYED',       0.5, 2);

END $$;
