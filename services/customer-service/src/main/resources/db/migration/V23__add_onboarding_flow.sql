-- Persist the onboarding flow (CANADA | FOREIGN | GUEST | KSA) on the customer so that
-- downstream consumers (e.g. notification-service) can resolve the display currency.
-- Canada/Foreign flows are CAD-denominated for notification display purposes.
ALTER TABLE customers ADD COLUMN IF NOT EXISTS onboarding_flow VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_customers_onboarding_flow ON customers(tenant_id, onboarding_flow);
