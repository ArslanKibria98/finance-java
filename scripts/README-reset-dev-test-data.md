# Reset dev/test business data (PostgreSQL)

Use this when you want a **fresh slate for local/integration testing**: customers, loan applications, collections/repayments, ledger journals (reports), and related logs — **without** dropping databases or re-running migrations.

## What it does

- Truncates **transactional / runtime** tables across these databases (when present):
  `identity_db`, `customer_db`, `global_profile_db`, `pii_vault_db`, `wallet_db`,
  `kyc_adapter_db`, `lending_db`, `collections_db`, `ledger_db`, `risk_service`,
  `fraud_service_db`, `middleware_third_party_db`.
- **Keeps** `flyway_schema_history` (schema intact).
- **Keeps** RBAC seed data in `identity_db` (`roles`, `permissions`, `casbin_rule`, …).
- **Keeps** reference/config where sensible (`banks`, `purpose_of_finance`, `dunning_policies`, COA `accounts`, …).
- Does **not** truncate `product_service_db` (product catalog). Add manual SQL there if you need it empty.
- If `risk_service` database does not exist (only some environments create it), that step is skipped.

## What it does **not** do

- Does **not** remove **Keycloak** realm users (`ksa_financing` DB). After reset, old Keycloak users may still exist while platform DB mappings are cleared — either delete test users in Keycloak Admin or recreate mappings via onboarding.
- Does **not** replace `docker compose down -v` (full volume wipe). For a completely empty Postgres, use volume removal + `docker compose up` instead.

## Requirements

- `psql` client
- Postgres reachable (default: `localhost:5432`, user `postgres`, password `postgres`)

## Usage

```bash
cd /var/www/islamic-financing-platform

# defaults match docker-compose postgres service
export PGHOST=localhost
export PGPORT=5432
export PGUSER=postgres
export PGPASSWORD=postgres

chmod +x scripts/reset-dev-test-data.sh
./scripts/reset-dev-test-data.sh
```

Optional: only print SQL without executing:

```bash
DRY_RUN=1 ./scripts/reset-dev-test-data.sh
```

## Safety

**Dev/test only.** Do not run against production.
