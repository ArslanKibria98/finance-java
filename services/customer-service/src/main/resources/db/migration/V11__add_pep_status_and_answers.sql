-- Add pep_status column to customers (aligns with JPA entity which expects it).
-- PENDING means PEP questions were NOT captured during onboarding and must be answered post-login.
-- COMPLETED means PEP questions were submitted (either during onboarding or post-login).
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS pep_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE customers
    ADD CONSTRAINT chk_pep_status CHECK (pep_status IN ('PENDING', 'COMPLETED'));

-- Detailed PEP/EDD answer storage. Single row per customer (UNIQUE constraint).
-- Both onboarding EDD flow (when user declares PEP=true) and the post-login PEP flow
-- (when user said NO during onboarding but now must confirm) write to this same table.
CREATE TABLE customer_pep_answers (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                    UUID NOT NULL,
    customer_id                  UUID NOT NULL UNIQUE REFERENCES customers(id) ON DELETE CASCADE,
    is_pep                       BOOLEAN NOT NULL,
    political_position           VARCHAR(100),
    government_body              VARCHAR(200),
    country_of_influence         VARCHAR(10),
    position_start_date          VARCHAR(20),
    position_end_date            VARCHAR(20),
    primary_source_of_wealth     VARCHAR(50),
    estimated_net_worth          VARCHAR(50),
    source_of_wealth_description TEXT,
    source_of_funds              VARCHAR(50),
    source_of_funds_details      TEXT,
    related_persons              JSONB,
    additional_notes             TEXT,
    submitted_via                VARCHAR(20) NOT NULL,   -- 'ONBOARDING' | 'POST_LOGIN'
    submitted_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_by                 UUID,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                      INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_pep_submitted_via CHECK (submitted_via IN ('ONBOARDING', 'POST_LOGIN'))
);

CREATE INDEX idx_pep_answers_tenant   ON customer_pep_answers(tenant_id);
CREATE INDEX idx_pep_answers_customer ON customer_pep_answers(customer_id);

COMMENT ON TABLE customer_pep_answers IS
    'Persists detailed PEP/EDD answers. Shared by onboarding and post-login PEP flows.';
COMMENT ON COLUMN customer_pep_answers.submitted_via IS
    'Channel through which the answers were captured: ONBOARDING (during signup) or POST_LOGIN (answered later when skipped at onboarding).';
COMMENT ON COLUMN customers.pep_status IS
    'PENDING: customer declared NO during onboarding (questions not captured). COMPLETED: answers stored in customer_pep_answers.';
