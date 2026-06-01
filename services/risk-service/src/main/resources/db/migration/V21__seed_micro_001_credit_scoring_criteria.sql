-- ============================================================================
-- V21: Sample credit scoring criteria for the MICRO-001 test product
-- ----------------------------------------------------------------------------
-- BRS LOS §5 Step 4 (Credit Decisioning Black Box) — scorecard logic example.
-- Total max weight = 100 so scorePercentage equals the raw totalScore.
-- Default thresholds (risk-service application.yml):
--   green ≥ 75 → AUTO_APPROVE
--   amber ≥ 50 → REFER_MANUAL_REVIEW
--   below 50  → AUTO_REJECT
--
-- Resolves the MICRO-001 product UUID via postgres_fdw against
-- product_service_db. If the foreign server / product row isn't available
-- (fresh local DB without product seed) the migration logs a notice and
-- skips — credit scoring criteria can later be POSTed via the admin API
-- (`PUT /api/v1/risk/credit-scoring/products/{productId}/criteria`).
-- ============================================================================

DO $$
DECLARE
    default_tenant      UUID := '00000000-0000-0000-0000-000000000001';
    micro_001_product   UUID;
    simah_field         UUID;
    salary_field        UUID;
    age_field           UUID;
    employment_field    UUID;
    obligations_field   UUID;
    defaults_field      UUID;

    crit_simah          UUID;
    crit_salary         UUID;
    crit_age            UUID;
    crit_employment     UUID;
    crit_obligations    UUID;
    crit_defaults       UUID;
BEGIN
    -- ── Ensure FDW link to product_service_db exists ──────────────────────
    -- (V2 already created the FDW + user mapping for customer_db; we mirror
    --  that for product_service_db. CREATE … IF NOT EXISTS is idempotent.)
    BEGIN
        CREATE EXTENSION IF NOT EXISTS postgres_fdw;
    EXCEPTION WHEN insufficient_privilege THEN
        RAISE NOTICE 'V21 seed: postgres_fdw not available, skipping MICRO-001 criteria seed';
        RETURN;
    END;

    IF NOT EXISTS (SELECT 1 FROM pg_foreign_server WHERE srvname = 'product_db_server') THEN
        BEGIN
            CREATE SERVER product_db_server
                FOREIGN DATA WRAPPER postgres_fdw
                OPTIONS (host 'localhost', port '5432', dbname 'product_service_db');

            CREATE USER MAPPING FOR postgres
                SERVER product_db_server
                OPTIONS (user 'postgres', password 'postgres');
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'V21 seed: could not create FDW to product_service_db (%), skipping', SQLERRM;
            RETURN;
        END;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.foreign_tables
         WHERE foreign_server_name = 'product_db_server' AND foreign_table_name = 'ft_products'
    ) THEN
        BEGIN
            CREATE FOREIGN TABLE ft_products (
                id           UUID,
                tenant_id    UUID,
                product_code VARCHAR(50)
            ) SERVER product_db_server
              OPTIONS (schema_name 'public', table_name 'products');
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'V21 seed: could not create ft_products foreign table (%), skipping', SQLERRM;
            RETURN;
        END;
    END IF;

    -- ── Look up MICRO-001 over the foreign table
    BEGIN
        SELECT id INTO micro_001_product
        FROM ft_products
        WHERE tenant_id = default_tenant AND product_code = 'MICRO-001'
        LIMIT 1;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'V21 seed: foreign lookup failed (%), skipping', SQLERRM;
        RETURN;
    END;

    IF micro_001_product IS NULL THEN
        RAISE NOTICE 'V21 seed: MICRO-001 product not found for tenant %, skipping', default_tenant;
        RETURN;
    END IF;

    -- ── Don't double-seed if criteria already exist
    IF EXISTS (
        SELECT 1 FROM product_credit_scoring_criteria
        WHERE tenant_id = default_tenant AND product_id = micro_001_product
    ) THEN
        RAISE NOTICE 'V21 seed: credit scoring criteria already exist for MICRO-001';
        RETURN;
    END IF;

    -- ── Resolve field-definition IDs (seeded in V7)
    SELECT id INTO simah_field       FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'simah_score';
    SELECT id INTO salary_field      FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'salary';
    SELECT id INTO age_field         FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'age';
    SELECT id INTO employment_field  FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'months_in_current_job';
    SELECT id INTO obligations_field FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'existing_obligations';
    SELECT id INTO defaults_field    FROM credit_scoring_field_definitions WHERE tenant_id = default_tenant AND field_key = 'previous_defaults';

    -- ══════ CRITERIA: SIMAH Score (max 30) ══════
    INSERT INTO product_credit_scoring_criteria
        (tenant_id, product_id, field_definition_id, is_enabled, sort_order)
        VALUES (default_tenant, micro_001_product, simah_field, TRUE, 1)
        RETURNING id INTO crit_simah;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight) VALUES
        (default_tenant, crit_simah, 'GTE', '750', 30),
        (default_tenant, crit_simah, 'GTE', '650', 20),
        (default_tenant, crit_simah, 'GTE', '550', 10),
        (default_tenant, crit_simah, 'LT',  '550',  0);

    -- ══════ CRITERIA: Verified Salary (max 25) ══════
    INSERT INTO product_credit_scoring_criteria
        (tenant_id, product_id, field_definition_id, is_enabled, sort_order)
        VALUES (default_tenant, micro_001_product, salary_field, TRUE, 2)
        RETURNING id INTO crit_salary;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight) VALUES
        (default_tenant, crit_salary, 'GTE', '15000', 25),
        (default_tenant, crit_salary, 'GTE',  '8000', 18),
        (default_tenant, crit_salary, 'GTE',  '5000', 10),
        (default_tenant, crit_salary, 'LT',   '5000',  0);

    -- ══════ CRITERIA: Age band (max 10) ══════
    INSERT INTO product_credit_scoring_criteria
        (tenant_id, product_id, field_definition_id, is_enabled, sort_order)
        VALUES (default_tenant, micro_001_product, age_field, TRUE, 3)
        RETURNING id INTO crit_age;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight) VALUES
        (default_tenant, crit_age, 'BETWEEN', '25,55', 10),
        (default_tenant, crit_age, 'BETWEEN', '21,60',  5);

    -- ══════ CRITERIA: Employment tenure (max 15) ══════
    INSERT INTO product_credit_scoring_criteria
        (tenant_id, product_id, field_definition_id, is_enabled, sort_order)
        VALUES (default_tenant, micro_001_product, employment_field, TRUE, 4)
        RETURNING id INTO crit_employment;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight) VALUES
        (default_tenant, crit_employment, 'GTE', '24', 15),
        (default_tenant, crit_employment, 'GTE', '12', 10),
        (default_tenant, crit_employment, 'GTE',  '6',  5),
        (default_tenant, crit_employment, 'LT',   '6',  0);

    -- ══════ CRITERIA: Existing Obligations (max 10) ══════
    INSERT INTO product_credit_scoring_criteria
        (tenant_id, product_id, field_definition_id, is_enabled, sort_order)
        VALUES (default_tenant, micro_001_product, obligations_field, TRUE, 5)
        RETURNING id INTO crit_obligations;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight) VALUES
        (default_tenant, crit_obligations, 'LTE', '1000', 10),
        (default_tenant, crit_obligations, 'LTE', '3000',  7),
        (default_tenant, crit_obligations, 'LTE', '6000',  3),
        (default_tenant, crit_obligations, 'GT',  '6000',  0);

    -- ══════ CRITERIA: Previous Defaults (max 10) ══════
    INSERT INTO product_credit_scoring_criteria
        (tenant_id, product_id, field_definition_id, is_enabled, sort_order)
        VALUES (default_tenant, micro_001_product, defaults_field, TRUE, 6)
        RETURNING id INTO crit_defaults;

    INSERT INTO product_credit_scoring_rules (tenant_id, criteria_id, operator, value, weight) VALUES
        (default_tenant, crit_defaults, 'EQ', 'false', 10),
        (default_tenant, crit_defaults, 'EQ',  'true',  0);

    RAISE NOTICE 'V21 seed complete: 6 criteria (max score 100) inserted for MICRO-001';
END $$;
