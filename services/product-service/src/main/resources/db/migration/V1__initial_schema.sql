-- ============================================================================
-- PRODUCT SERVICE DATABASE SCHEMA (MERGED)
-- PostgreSQL 16+ Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================
--
-- MERGE STRATEGY:
--   KEPT AS-IS:    product_versions, eligibility_rules, pricing_slabs, fees,
--                  simah_rules, outbox_events
--   EXTENDED:      products (new UI columns added)
--   REPLACED:      document_checklists  -> product_documents
--                  product_commodity_vendors -> product_commodity_configs,
--                                              product_commodity_items,
--                                              product_commodity_providers
--   NEW TABLES:    product_master_categories, product_sub_categories,
--                  product_templates, product_application_steps,
--                  product_terms_conditions, product_fee_settings,
--                  product_admin_fee_slabs,
--                  environment_configs, product_environment_configs,
--                  product_duration_settings, product_approval_workflows,
--                  product_approval_conditions, product_approval_actions,
--                  credit_scoring_field_definitions,
--                  product_credit_scoring_criteria, product_credit_scoring_rules,
--                  product_documents,
--                  partners, product_partner_affiliations,
--                  user_affiliations
--
-- TOTAL: 29 tables (7 kept/extended + 2 replaced + 20 new)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES (from existing ERD — kept as-is)
-- ============================================================================

CREATE TYPE product_type AS ENUM (
    'TAWARRUQ',
    'MURABAHA',
    'IJARA',
    'MUSHARAKAH',
    'FACTORING',
    'BNPL',
    'EARLY_WAGES'
);

CREATE TYPE product_status AS ENUM (
    'DRAFT',
    'ACTIVE',
    'INACTIVE',
    'ARCHIVED'
);

CREATE TYPE target_segment AS ENUM (
    'INDIVIDUAL',
    'SME',
    'BOTH'
);

CREATE TYPE fee_calculation_type AS ENUM (
    'FIXED',
    'PERCENTAGE',
    'SLAB'
);

CREATE TYPE fee_timing AS ENUM (
    'UPFRONT',
    'DEDUCT_FROM_DISBURSEMENT',
    'ON_EVENT',
    'MONTHLY'
);

CREATE TYPE rule_type AS ENUM (
    'NUMERIC_RANGE',
    'LIST',
    'BOOLEAN',
    'COMPUTED',
    'EXPRESSION'
);

CREATE TYPE rule_operator AS ENUM (
    'EQ',
    'NE',
    'GT',
    'GTE',
    'LT',
    'LTE',
    'IN',
    'NOT_IN',
    'BETWEEN'
);

-- ============================================================================
-- REFERENCE DATA TABLES (NEW — seeded via V2 migration)
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Product Master Categories
-- Stores the 5 master categories: Murabaha, Ijarah, Tawarruq, BNPL, Crowd Funding
-- -----------------------------------------------------------------------------
CREATE TABLE product_master_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description_en TEXT,
    description_ar TEXT,
    icon_url VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_master_category_code UNIQUE (tenant_id, code)
);

-- -----------------------------------------------------------------------------
-- Product Sub-Categories
-- Sub-categories per master: Personal Financing, Business Financing, etc.
-- -----------------------------------------------------------------------------
CREATE TABLE product_sub_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    master_category_id UUID NOT NULL REFERENCES product_master_categories(id),
    code VARCHAR(50) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_sub_category_code UNIQUE (master_category_id, code)
);

-- -----------------------------------------------------------------------------
-- Product Templates
-- Pre-configured templates for quick product setup
-- -----------------------------------------------------------------------------
CREATE TABLE product_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    master_category_id UUID NOT NULL REFERENCES product_master_categories(id),
    sub_category_id UUID REFERENCES product_sub_categories(id),
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description TEXT,
    badge VARCHAR(50),                      -- e.g., "Reliable"
    estimated_setup_minutes INT,
    features JSONB,                         -- ["Quick approval", "Flexible repayment", ...]
    template_data JSONB,                    -- Full default configuration snapshot

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Products (EXTENDED — existing ERD columns + new UI columns)
-- -----------------------------------------------------------------------------
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Product identification (existing)
    product_code VARCHAR(20) NOT NULL,
    version INT NOT NULL DEFAULT 1,

    -- Identity (existing)
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description_en TEXT,
    description_ar TEXT,
    logo_url VARCHAR(500),

    -- NEW: Short descriptions from UI
    short_description_en TEXT,
    short_description_ar TEXT,

    -- Classification (existing)
    product_type product_type NOT NULL,
    target_segment target_segment NOT NULL DEFAULT 'INDIVIDUAL',

    -- NEW: Category references from UI
    master_category_id UUID REFERENCES product_master_categories(id),
    sub_category_id UUID REFERENCES product_sub_categories(id),
    template_id UUID REFERENCES product_templates(id),

    -- NEW: UI fields
    notification_email VARCHAR(255),
    customer_types VARCHAR(50)[],           -- {INDIVIDUAL, SME, CORPORATE}
    involves_commodity BOOLEAN NOT NULL DEFAULT FALSE,
    setup_method VARCHAR(20),               -- TEMPLATE or CUSTOM

    -- NEW: Wizard progress tracking
    wizard_step INT NOT NULL DEFAULT 1,     -- Track wizard progress (1-5)
    wizard_completed BOOLEAN NOT NULL DEFAULT FALSE,

    -- Sharia configuration (existing)
    sharia_structure VARCHAR(50) NOT NULL,
    commodity_required BOOLEAN NOT NULL DEFAULT FALSE,

    -- Availability (existing)
    status product_status NOT NULL DEFAULT 'DRAFT',
    start_date DATE,
    end_date DATE,
    visible_to_customers BOOLEAN NOT NULL DEFAULT FALSE,
    visible_to_partners BOOLEAN NOT NULL DEFAULT TRUE,

    -- Targeting (existing)
    allowed_nationalities JSONB,            -- ["SA", "ALL_GCC"]
    allowed_residencies JSONB,              -- ["CITIZEN", "RESIDENT"]

    -- Financial terms (existing)
    min_amount NUMERIC(19, 4) NOT NULL,
    max_amount NUMERIC(19, 4) NOT NULL,
    min_tenure_months INT NOT NULL,
    max_tenure_months INT NOT NULL,
    allowed_tenures INT[],                  -- [6, 12, 18, 24, 36, 48, 60]

    -- Profit rate (existing)
    base_profit_rate NUMERIC(7, 6) NOT NULL,
    rate_type VARCHAR(20) NOT NULL DEFAULT 'REDUCING_BALANCE',

    -- Repayment (existing)
    repayment_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    grace_period_days INT NOT NULL DEFAULT 3,

    -- Early settlement (existing)
    early_settlement_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    waive_unearned_profit BOOLEAN NOT NULL DEFAULT TRUE,
    min_tenure_before_settlement INT DEFAULT 3,

    -- Regional (existing)
    currency VARCHAR(3) NOT NULL DEFAULT 'SAR',
    countries JSONB NOT NULL DEFAULT '["SA"]',

    -- Audit columns (existing)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version_number INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT chk_amount_range CHECK (min_amount > 0 AND max_amount >= min_amount),
    CONSTRAINT chk_tenure_range CHECK (min_tenure_months > 0 AND max_tenure_months >= min_tenure_months),
    CONSTRAINT uq_product_code_version UNIQUE (tenant_id, product_code, version)
);

-- -----------------------------------------------------------------------------
-- Product Version History (KEPT AS-IS)
-- -----------------------------------------------------------------------------
CREATE TABLE product_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Link
    product_id UUID NOT NULL REFERENCES products(id),
    version_number INT NOT NULL,

    -- Snapshot
    product_snapshot JSONB NOT NULL,

    -- Change tracking
    change_reason VARCHAR(500),
    change_type VARCHAR(50),                -- MINOR, MAJOR, PRICING, ELIGIBILITY

    -- Approval
    approved_by UUID,
    approved_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,

    -- Constraints
    CONSTRAINT uq_product_version UNIQUE (product_id, version_number)
);

-- -----------------------------------------------------------------------------
-- Eligibility Rules (KEPT AS-IS)
-- -----------------------------------------------------------------------------
CREATE TABLE eligibility_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Link
    product_id UUID NOT NULL REFERENCES products(id),

    -- Rule identification
    rule_id VARCHAR(50) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_description TEXT,

    -- Rule type
    rule_type rule_type NOT NULL,

    -- Field to evaluate
    field_path VARCHAR(255) NOT NULL,       -- e.g., "customer.age", "simah.score"

    -- Operator and value
    operator rule_operator NOT NULL,
    value JSONB NOT NULL,                   -- Can be number, string, array

    -- Computed expression (for COMPUTED type)
    expression VARCHAR(500),

    -- Conditions (when this rule applies)
    conditions JSONB,                       -- [{field, operator, value}]

    -- Error handling
    error_code VARCHAR(20) NOT NULL,
    error_message_en VARCHAR(500) NOT NULL,
    error_message_ar VARCHAR(500),

    -- Ordering
    execution_order INT NOT NULL DEFAULT 0,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_rule_product UNIQUE (product_id, rule_id)
);

-- -----------------------------------------------------------------------------
-- Pricing Slabs (KEPT AS-IS)
-- -----------------------------------------------------------------------------
CREATE TABLE pricing_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Link
    product_id UUID NOT NULL REFERENCES products(id),

    -- Slab identification
    slab_id VARCHAR(50) NOT NULL,
    slab_name VARCHAR(255) NOT NULL,

    -- Dimension
    dimension VARCHAR(50) NOT NULL,         -- SALARY, SIMAH_SCORE, EMPLOYMENT, TENURE

    -- Conditions
    conditions JSONB NOT NULL,              -- [{field, operator, value}]

    -- Rate
    profit_rate NUMERIC(7, 6),              -- Override rate
    rate_adjustment NUMERIC(7, 6),          -- Additive adjustment
    adjustment_type VARCHAR(20),            -- OVERRIDE, ADDITIVE

    -- Priority (lower = higher priority)
    priority INT NOT NULL DEFAULT 100,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_slab_product UNIQUE (product_id, slab_id)
);

-- -----------------------------------------------------------------------------
-- Fees Configuration (KEPT AS-IS)
-- -----------------------------------------------------------------------------
CREATE TABLE fees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Link
    product_id UUID NOT NULL REFERENCES products(id),

    -- Fee identification
    fee_code VARCHAR(50) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),

    -- Calculation
    calculation_type fee_calculation_type NOT NULL,
    fixed_amount NUMERIC(19, 4),
    percentage_rate NUMERIC(7, 6),
    min_amount NUMERIC(19, 4),
    max_amount NUMERIC(19, 4),

    -- Timing
    timing fee_timing NOT NULL,

    -- Tax
    vat_applicable BOOLEAN NOT NULL DEFAULT TRUE,
    vat_rate NUMERIC(5, 4) DEFAULT 0.15,

    -- Sharia compliance
    charity_fund BOOLEAN NOT NULL DEFAULT FALSE,

    -- GL mapping
    gl_account_code VARCHAR(50),

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_fee_product UNIQUE (product_id, fee_code)
);

-- -----------------------------------------------------------------------------
-- Simah Rules (KEPT AS-IS)
-- -----------------------------------------------------------------------------
CREATE TABLE simah_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Link
    product_id UUID NOT NULL REFERENCES products(id),

    -- Score cutoffs
    auto_reject_below INT NOT NULL DEFAULT 500,
    manual_review_below INT NOT NULL DEFAULT 600,
    auto_approve_above INT NOT NULL DEFAULT 650,

    -- Staging rules
    stage_1_dpd_max INT NOT NULL DEFAULT 30,
    stage_2_dpd_max INT NOT NULL DEFAULT 90,
    stage_3_action VARCHAR(20) NOT NULL DEFAULT 'AUTO_REJECT',

    -- Dispute handling
    exclude_disputed_from_dbr BOOLEAN NOT NULL DEFAULT TRUE,

    -- Cache
    cache_period_days INT NOT NULL DEFAULT 30,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_simah_product UNIQUE (product_id)
);

-- ============================================================================
-- STEP 2: COMMODITY TABLES (REPLACES product_commodity_vendors)
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Product Commodity Configs
-- Funding configuration per product (1:1 with product)
-- -----------------------------------------------------------------------------
CREATE TABLE product_commodity_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    funding_types VARCHAR(50)[],            -- {COMPANY_BACKED, CROWD_FUNDED, INVESTOR_FUNDED}

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_commodity_config_product UNIQUE (product_id)
);

-- -----------------------------------------------------------------------------
-- Product Commodity Items
-- Individual commodity definitions (repeatable)
-- -----------------------------------------------------------------------------
CREATE TABLE product_commodity_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    commodity_config_id UUID NOT NULL REFERENCES product_commodity_configs(id),
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    category VARCHAR(100),                  -- Healthcare, Technology, etc.
    unit VARCHAR(50),                       -- kg, piece, lot, etc.
    min_quantity INT,
    max_quantity INT,
    unit_price NUMERIC(19, 4),
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- -----------------------------------------------------------------------------
-- Product Commodity Providers
-- Commodity supplier/provider info
-- -----------------------------------------------------------------------------
CREATE TABLE product_commodity_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    commodity_config_id UUID NOT NULL REFERENCES product_commodity_configs(id),
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    contact_info TEXT,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- ============================================================================
-- STEP 3: SETTINGS TABLES (8 tabs from UI)
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Tab 1: Product Application Steps
-- Workflow steps for the product application process
-- -----------------------------------------------------------------------------
CREATE TABLE product_application_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    step_number INT NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    title_ar VARCHAR(255),
    description TEXT,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- -----------------------------------------------------------------------------
-- Tab 2: Product Terms & Conditions
-- Bilingual terms & conditions (1:1 with product)
-- -----------------------------------------------------------------------------
CREATE TABLE product_terms_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    terms_en TEXT,                           -- Rich text content
    terms_ar TEXT,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_terms_product UNIQUE (product_id)
);

-- -----------------------------------------------------------------------------
-- Tab 3: Product Fee Settings
-- Min/max financing, DBR config (1:1 with product)
-- -----------------------------------------------------------------------------
CREATE TABLE product_fee_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    min_financing_amount NUMERIC(19, 4) NOT NULL,
    max_financing_amount NUMERIC(19, 4) NOT NULL,
    vat_percentage NUMERIC(5, 2) NOT NULL DEFAULT 15.00,
    revenue_eligibility_threshold NUMERIC(19, 4),
    max_dbr_percentage NUMERIC(5, 2),       -- e.g., 33%
    dbr_calculation_method VARCHAR(50),     -- GROSS_INCOME, NET_INCOME
    dbr_exceptions TEXT,                    -- Free text

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_fee_settings_product UNIQUE (product_id),
    CONSTRAINT chk_fee_settings_amount CHECK (min_financing_amount <= max_financing_amount)
);

-- -----------------------------------------------------------------------------
-- Tab 4: Product Admin Fee Slabs
-- Tiered admin fees by amount range
-- -----------------------------------------------------------------------------
CREATE TABLE product_admin_fee_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    min_amount NUMERIC(19, 4) NOT NULL,
    max_amount NUMERIC(19, 4) NOT NULL,
    profit_percentage NUMERIC(7, 4),
    processing_fee NUMERIC(19, 4),
    partner_scope VARCHAR(50) NOT NULL DEFAULT 'ALL_PARTNERS', -- ALL_PARTNERS, SPECIFIC_PARTNERS
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',              -- ACTIVE, INACTIVE
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- -----------------------------------------------------------------------------
-- Tab 5: Environment Configs — 2-table design (master + pivot)
--
-- Table 1: environment_configs       = standalone master of all third-party integrations
--          (Credit Bureau API, Nafath, SADAD, Payment Gateway, etc.)
-- Table 2: product_environment_configs = pivot linking products to their selected
--          third parties (which integrations are active for this product)
--
-- Flow: Admin creates third-party integration once in environment_configs
--       When configuring a product, admin selects which integrations apply
--       Each product can have different integrations enabled/disabled
-- -----------------------------------------------------------------------------

-- Master table: all third-party integrations (defined once, reusable across products)
CREATE TABLE environment_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    config_code VARCHAR(50) NOT NULL,          -- "SIMAH_API", "NAFATH", "SADAD", "YAKEEN", etc.
    configuration_name VARCHAR(255) NOT NULL,  -- "Credit Bureau API", "Nafath Identity", etc.
    configuration_name_ar VARCHAR(255),        -- Arabic label
    http_method VARCHAR(10) NOT NULL DEFAULT 'GET',  -- GET, POST, PUT, DELETE
    api_endpoint VARCHAR(1000),

    -- JSONB fields for flexible configuration
    parameters JSONB,                       -- {"query_params": {...}, "body_params": {...}, "timeout_seconds": 30}
    credentials JSONB,                      -- {"auth_type": "BEARER", "api_key": "...", "api_secret": "..."}
    headers JSONB,                          -- {"Content-Type": "application/json", "X-Custom": "value"}

    test_mode BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_env_config_code UNIQUE (tenant_id, config_code)
);

-- Pivot table: which third parties are selected/active for each product
CREATE TABLE product_environment_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    environment_config_id UUID NOT NULL REFERENCES environment_configs(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,   -- Enable/disable per product
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_product_env_config UNIQUE (product_id, environment_config_id)
);

-- -----------------------------------------------------------------------------
-- Tab 6: Product Duration Settings
-- Process durations (1:1 with product)
-- -----------------------------------------------------------------------------
CREATE TABLE product_duration_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    request_duration_days INT NOT NULL DEFAULT 30,
    approval_duration_days INT NOT NULL DEFAULT 7,
    disbursement_duration_days INT NOT NULL DEFAULT 3,
    repayment_duration_days INT NOT NULL DEFAULT 365,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_duration_product UNIQUE (product_id)
);

-- -----------------------------------------------------------------------------
-- Tab 7: Product Approval Workflows
-- Configurable approval/rejection/auto-approval rules
-- -----------------------------------------------------------------------------
CREATE TABLE product_approval_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    workflow_type VARCHAR(30) NOT NULL,     -- MANUAL_APPROVAL, AUTO_APPROVAL, REJECTION_SCENARIO
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    description TEXT,
    template_source VARCHAR(100),           -- null if custom, template name if predefined
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,        -- Execution order

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- Approval Conditions (child of workflows)
CREATE TABLE product_approval_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    workflow_id UUID NOT NULL REFERENCES product_approval_workflows(id) ON DELETE CASCADE,
    field VARCHAR(255) NOT NULL,            -- e.g., "loan_amount", "credit_score"
    operator VARCHAR(20) NOT NULL,          -- EQ, GT, GTE, LT, LTE, IN, BETWEEN
    value JSONB NOT NULL,                   -- Flexible value
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Approval Actions (child of workflows)
CREATE TABLE product_approval_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    workflow_id UUID NOT NULL REFERENCES product_approval_workflows(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,       -- APPROVE, REJECT, ESCALATE, NOTIFY, ASSIGN_REVIEWER
    configuration JSONB,                    -- {"notify_email": "...", "reviewer_role": "..."}
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Tab 8: Product Credit Scoring — 3-table design
--
-- Table 1: credit_scoring_field_definitions = master lookup (the ~40 dropdown
--          options like Customer Type, Salary, SIMAH Score — dynamically managed)
-- Table 2: credit_scoring_criteria  = which fields are selected per product
--          (references field_definitions for predefined, or stores custom name)
-- Table 3: credit_scoring_rules     = rules per criteria
--          (operator + value + weight + percentage for scoring engine)
--
-- Flow: Admin opens dropdown → options loaded from field_definitions
--       Admin selects "Salary" → criteria row created (FK to field_definitions)
--       Admin adds custom criteria "Dyal Al Plus" → criteria row with is_custom=TRUE
--       Admin adds rules per criteria (salary > 5000 → weight 3, percentage 15%)
--       Multiple rules per criteria supported (range-based scoring)
-- -----------------------------------------------------------------------------

-- Master lookup: the ~40 predefined fields shown in the dropdown (dynamically managed)
CREATE TABLE credit_scoring_field_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    field_key VARCHAR(100) NOT NULL,        -- "customer_type", "age", "salary", "simah_score", etc.
    name_en VARCHAR(255) NOT NULL,          -- "Customer Type", "Age Of Customer", "Salary", etc.
    name_ar VARCHAR(255),                   -- Arabic label
    data_type VARCHAR(50) NOT NULL DEFAULT 'STRING', -- STRING, NUMERIC, BOOLEAN (helps UI render correct input)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_scoring_field_key UNIQUE (tenant_id, field_key)
);

-- Which fields are selected for a specific product
CREATE TABLE product_credit_scoring_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    field_definition_id UUID REFERENCES credit_scoring_field_definitions(id), -- FK for predefined, NULL for custom
    custom_name VARCHAR(255),              -- Only set when is_custom = TRUE
    is_custom BOOLEAN NOT NULL DEFAULT FALSE, -- FALSE = from dropdown, TRUE = admin-added custom
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- Rules per criteria (how to score each field)
CREATE TABLE product_credit_scoring_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    criteria_id UUID NOT NULL REFERENCES product_credit_scoring_criteria(id) ON DELETE CASCADE,
    operator VARCHAR(20) NOT NULL,          -- EQ, GT, GTE, LT, LTE, IN, BETWEEN, NOT_IN
    value VARCHAR(500) NOT NULL,            -- The comparison value (e.g., "5000", "CITIZEN", "500-700")
    weight NUMERIC(5, 2) NOT NULL DEFAULT 1.0,  -- Scoring weight for this rule
    percentage NUMERIC(7, 4),               -- Score percentage contribution (e.g., 15.00%)

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- STEP 4: PARTNERS (SEPARATED — standalone + pivot + admin tables)
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Partners (standalone master table)
-- Single source of truth for partner data, reusable across products
-- -----------------------------------------------------------------------------
CREATE TABLE partners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    partner_code VARCHAR(50) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    contact_person VARCHAR(255),
    logo_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, SUSPENDED

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_partner_code UNIQUE (tenant_id, partner_code)
);

-- -----------------------------------------------------------------------------
-- Product-Partner Affiliations (pivot table)
-- Many-to-many: each partner can be affiliated with multiple products,
-- each with a different commission rate and affiliation type
-- -----------------------------------------------------------------------------
CREATE TABLE product_partner_affiliations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    partner_id UUID NOT NULL REFERENCES partners(id),
    affiliation_type VARCHAR(20) NOT NULL,  -- PRIMARY, SECONDARY, REFERRAL
    commission_percentage NUMERIC(5, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_product_partner UNIQUE (product_id, partner_id)
);

-- -----------------------------------------------------------------------------
-- User Affiliations (single table linking Keycloak users to products/partners)
-- Roles (partner_admin, product_admin) are managed in Keycloak.
-- This table only stores the affiliation mapping — who is linked to what.
-- -----------------------------------------------------------------------------
CREATE TABLE user_affiliations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    user_id UUID NOT NULL,                  -- Keycloak sub claim
    product_id UUID REFERENCES products(id),    -- Nullable: set when user is affiliated with a product
    partner_id UUID REFERENCES partners(id),    -- Nullable: set when user is affiliated with a partner
    department_id UUID,                     -- Nullable: for future department-level scoping
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE

    -- Soft delete
    deleted_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- ============================================================================
-- STEP 5: DOCUMENTS TABLE (REPLACES document_checklists)
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Product Documents
-- Required documents per product (matches v0 UI)
-- -----------------------------------------------------------------------------
CREATE TABLE product_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    product_id UUID NOT NULL REFERENCES products(id),
    name_en VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255),
    document_type VARCHAR(50) NOT NULL,     -- TEMPLATE, GUIDE, AGREEMENT
    file_url VARCHAR(1000),
    file_size_bytes BIGINT,
    file_version VARCHAR(20),               -- "v1", "v2", etc.
    created_by_name VARCHAR(255),           -- "Admin User", "Compliance Team"
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, DRAFT, ARCHIVED
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- ============================================================================
-- OUTBOX EVENTS (KEPT AS-IS)
-- ============================================================================

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,

    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Reference Data
CREATE INDEX idx_master_categories_tenant ON product_master_categories(tenant_id);
CREATE INDEX idx_sub_categories_master ON product_sub_categories(master_category_id);
CREATE INDEX idx_templates_category ON product_templates(master_category_id);

-- Products
CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_type ON products(tenant_id, product_type);
CREATE INDEX idx_products_status ON products(tenant_id, status);
CREATE INDEX idx_products_active ON products(tenant_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_products_segment ON products(tenant_id, target_segment) WHERE status = 'ACTIVE';
CREATE INDEX idx_products_category ON products(master_category_id);

-- Eligibility Rules
CREATE INDEX idx_eligibility_product ON eligibility_rules(product_id);
CREATE INDEX idx_eligibility_active ON eligibility_rules(product_id) WHERE is_active = TRUE;

-- Pricing Slabs
CREATE INDEX idx_pricing_product ON pricing_slabs(product_id);
CREATE INDEX idx_pricing_priority ON pricing_slabs(product_id, priority);

-- Fees
CREATE INDEX idx_fees_product ON fees(product_id);
CREATE INDEX idx_fees_active ON fees(product_id) WHERE is_active = TRUE;

-- Commodity
CREATE INDEX idx_commodity_config_product ON product_commodity_configs(product_id);
CREATE INDEX idx_commodity_items_config ON product_commodity_items(commodity_config_id);
CREATE INDEX idx_commodity_providers_config ON product_commodity_providers(commodity_config_id);

-- Settings
CREATE INDEX idx_app_steps_product ON product_application_steps(product_id);
CREATE INDEX idx_fee_settings_product ON product_fee_settings(product_id);
CREATE INDEX idx_admin_fee_slabs_product ON product_admin_fee_slabs(product_id);
CREATE INDEX idx_env_configs_tenant ON environment_configs(tenant_id);
CREATE INDEX idx_env_configs_active ON environment_configs(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_product_env_configs_product ON product_environment_configs(product_id);
CREATE INDEX idx_product_env_configs_config ON product_environment_configs(environment_config_id);
CREATE INDEX idx_approval_workflows_product ON product_approval_workflows(product_id);
CREATE INDEX idx_approval_conditions_workflow ON product_approval_conditions(workflow_id);
CREATE INDEX idx_approval_actions_workflow ON product_approval_actions(workflow_id);
CREATE INDEX idx_scoring_field_defs_tenant ON credit_scoring_field_definitions(tenant_id);
CREATE INDEX idx_credit_scoring_product ON product_credit_scoring_criteria(product_id);
CREATE INDEX idx_credit_scoring_field_def ON product_credit_scoring_criteria(field_definition_id) WHERE field_definition_id IS NOT NULL;
CREATE INDEX idx_credit_scoring_rules_criteria ON product_credit_scoring_rules(criteria_id);

-- Partners + Affiliations
CREATE INDEX idx_partners_tenant ON partners(tenant_id);
CREATE INDEX idx_partners_status ON partners(tenant_id, status);
CREATE INDEX idx_product_partner_aff_product ON product_partner_affiliations(product_id);
CREATE INDEX idx_product_partner_aff_partner ON product_partner_affiliations(partner_id);

-- User Affiliations
CREATE INDEX idx_user_aff_user ON user_affiliations(user_id);
CREATE INDEX idx_user_aff_product ON user_affiliations(product_id) WHERE product_id IS NOT NULL;
CREATE INDEX idx_user_aff_partner ON user_affiliations(partner_id) WHERE partner_id IS NOT NULL;
CREATE INDEX idx_user_aff_active ON user_affiliations(user_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Documents
CREATE INDEX idx_documents_product ON product_documents(product_id);
CREATE INDEX idx_documents_required ON product_documents(product_id) WHERE is_required = TRUE AND status = 'ACTIVE';

-- Outbox
CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;

-- ============================================================================
-- TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Core tables
CREATE TRIGGER trigger_products_updated
    BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_eligibility_updated
    BEFORE UPDATE ON eligibility_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_pricing_updated
    BEFORE UPDATE ON pricing_slabs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_fees_updated
    BEFORE UPDATE ON fees FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_simah_updated
    BEFORE UPDATE ON simah_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Reference data
CREATE TRIGGER trigger_master_categories_updated
    BEFORE UPDATE ON product_master_categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_sub_categories_updated
    BEFORE UPDATE ON product_sub_categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_templates_updated
    BEFORE UPDATE ON product_templates FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Commodity
CREATE TRIGGER trigger_commodity_configs_updated
    BEFORE UPDATE ON product_commodity_configs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_commodity_items_updated
    BEFORE UPDATE ON product_commodity_items FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_commodity_providers_updated
    BEFORE UPDATE ON product_commodity_providers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Settings
CREATE TRIGGER trigger_app_steps_updated
    BEFORE UPDATE ON product_application_steps FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_terms_conditions_updated
    BEFORE UPDATE ON product_terms_conditions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_fee_settings_updated
    BEFORE UPDATE ON product_fee_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_admin_fee_slabs_updated
    BEFORE UPDATE ON product_admin_fee_slabs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_env_configs_updated
    BEFORE UPDATE ON environment_configs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_product_env_configs_updated
    BEFORE UPDATE ON product_environment_configs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_duration_settings_updated
    BEFORE UPDATE ON product_duration_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_approval_workflows_updated
    BEFORE UPDATE ON product_approval_workflows FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_scoring_field_defs_updated
    BEFORE UPDATE ON credit_scoring_field_definitions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_credit_scoring_updated
    BEFORE UPDATE ON product_credit_scoring_criteria FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_credit_scoring_rules_updated
    BEFORE UPDATE ON product_credit_scoring_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Partners + Affiliations
CREATE TRIGGER trigger_partners_updated
    BEFORE UPDATE ON partners FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_product_partner_aff_updated
    BEFORE UPDATE ON product_partner_affiliations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_user_affiliations_updated
    BEFORE UPDATE ON user_affiliations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Documents
CREATE TRIGGER trigger_documents_updated
    BEFORE UPDATE ON product_documents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

-- Core tables
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE eligibility_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE pricing_slabs ENABLE ROW LEVEL SECURITY;
ALTER TABLE fees ENABLE ROW LEVEL SECURITY;
ALTER TABLE simah_rules ENABLE ROW LEVEL SECURITY;

-- Reference data
ALTER TABLE product_master_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_sub_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_templates ENABLE ROW LEVEL SECURITY;

-- Commodity
ALTER TABLE product_commodity_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_commodity_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_commodity_providers ENABLE ROW LEVEL SECURITY;

-- Settings
ALTER TABLE product_application_steps ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_terms_conditions ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_fee_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_admin_fee_slabs ENABLE ROW LEVEL SECURITY;
ALTER TABLE environment_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_environment_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_duration_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_approval_workflows ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_approval_conditions ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_approval_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_scoring_field_definitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_credit_scoring_criteria ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_credit_scoring_rules ENABLE ROW LEVEL SECURITY;

-- Partners + User Affiliations
ALTER TABLE partners ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_partner_affiliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_affiliations ENABLE ROW LEVEL SECURITY;

-- Documents
ALTER TABLE product_documents ENABLE ROW LEVEL SECURITY;

-- Outbox
ALTER TABLE outbox_events ENABLE ROW LEVEL SECURITY;

-- Tenant isolation policies
CREATE POLICY tenant_isolation ON products
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_versions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON eligibility_rules
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON pricing_slabs
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON fees
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON simah_rules
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_master_categories
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_sub_categories
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_templates
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_commodity_configs
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_commodity_items
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_commodity_providers
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_application_steps
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_terms_conditions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_fee_settings
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_admin_fee_slabs
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON environment_configs
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_environment_configs
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_duration_settings
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_approval_workflows
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_approval_conditions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_approval_actions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON credit_scoring_field_definitions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_credit_scoring_criteria
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_credit_scoring_rules
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON partners
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_partner_affiliations
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON user_affiliations
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON product_documents
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_isolation ON outbox_events
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- ============================================================================
-- COMMENTS
-- ============================================================================

-- Reference data
COMMENT ON TABLE product_master_categories IS 'Master product categories: Murabaha, Ijarah, Tawarruq, BNPL, Crowd Funding';
COMMENT ON TABLE product_sub_categories IS 'Sub-categories per master: Personal, Business, Trade Financing, etc.';
COMMENT ON TABLE product_templates IS 'Pre-configured templates for quick product setup via wizard';

-- Core
COMMENT ON TABLE products IS 'Product catalog with financing terms, Sharia configuration, and wizard progress tracking';
COMMENT ON TABLE product_versions IS 'Version history for audit and rollback';
COMMENT ON TABLE eligibility_rules IS 'Rule engine configuration for customer eligibility';
COMMENT ON TABLE pricing_slabs IS 'Dynamic pricing based on customer attributes';
COMMENT ON TABLE fees IS 'Fee configuration with VAT and GL mapping';
COMMENT ON TABLE simah_rules IS 'Credit bureau scoring thresholds';

-- Commodity (replaces product_commodity_vendors)
COMMENT ON TABLE product_commodity_configs IS 'Funding configuration per product (replaces product_commodity_vendors)';
COMMENT ON TABLE product_commodity_items IS 'Individual commodity definitions with pricing and quantity';
COMMENT ON TABLE product_commodity_providers IS 'Commodity supplier/provider information';

-- Settings (8 tabs)
COMMENT ON TABLE product_application_steps IS 'Tab 1: Ordered application workflow steps';
COMMENT ON TABLE product_terms_conditions IS 'Tab 2: Bilingual terms & conditions (EN/AR)';
COMMENT ON TABLE product_fee_settings IS 'Tab 3: Min/max financing amounts and DBR configuration';
COMMENT ON TABLE product_admin_fee_slabs IS 'Tab 4: Tiered admin fees by amount range';
COMMENT ON TABLE environment_configs IS 'Tab 5: Master table of all third-party integrations (SIMAH, Nafath, SADAD, etc.) — defined once, reusable';
COMMENT ON TABLE product_environment_configs IS 'Tab 5: Pivot — which third-party integrations are selected/active for each product';
COMMENT ON TABLE product_duration_settings IS 'Tab 6: Process durations for request, approval, disbursement, repayment';
COMMENT ON TABLE product_approval_workflows IS 'Tab 7: Configurable approval/rejection/auto-approval workflows';
COMMENT ON TABLE product_approval_conditions IS 'Tab 7: Conditions for approval workflow rules';
COMMENT ON TABLE product_approval_actions IS 'Tab 7: Actions triggered by approval workflow rules';
COMMENT ON TABLE credit_scoring_field_definitions IS 'Tab 8: Master lookup for ~40 predefined scoring fields shown in dropdown (dynamically managed, seeded via V2)';
COMMENT ON TABLE product_credit_scoring_criteria IS 'Tab 8: Which scoring fields are selected per product (FK to field_definitions or custom_name)';
COMMENT ON TABLE product_credit_scoring_rules IS 'Tab 8: Scoring rules per criteria — operator, value, weight, percentage (multiple rules per criteria)';

-- Partners + User Affiliations
COMMENT ON TABLE partners IS 'Standalone partner master data — single source of truth, reusable across products';
COMMENT ON TABLE product_partner_affiliations IS 'Many-to-many pivot: links products to partners with per-product commission and affiliation type';
COMMENT ON TABLE user_affiliations IS 'Links Keycloak users to products/partners/departments. Roles (partner_admin, product_admin) managed in Keycloak';

-- Documents
COMMENT ON TABLE product_documents IS 'Required documents per product (replaces document_checklists, matches v0 UI)';

-- Outbox
COMMENT ON TABLE outbox_events IS 'Transactional outbox for event publishing to Kafka';
