#!/usr/bin/env bash
# Reset transactional business data for local/dev testing (PostgreSQL).
# Preserves Flyway history and most reference/policy rows.
# See scripts/README-reset-dev-test-data.md
#
# Does NOT clear Keycloak users (ksa_financing). Delete test users in Keycloak Admin if needed.

set -euo pipefail

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
export PGPASSWORD="${PGPASSWORD:-postgres}"

DRY_RUN="${DRY_RUN:-0}"

info() { printf '%s\n' "$*" >&2; }

psql_db() {
  local db="$1"
  shift
  psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$db" "$@"
}

db_exists() {
  local name="$1"
  psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres -tAc \
    "SELECT 1 FROM pg_database WHERE datname='${name}'" 2>/dev/null | grep -q 1
}

run_sql() {
  local db="$1"
  local sql="$2"
  if [[ "$DRY_RUN" == "1" ]]; then
    printf '%s\n%s\n\n' "-- ${db}" "$sql" >&2
    return 0
  fi
  if ! db_exists "$db"; then
    info "Skipping missing database: ${db}"
    return 0
  fi
  info "Resetting database: ${db}"
  psql_db "$db" -c "$sql"
}

# --- SQL blocks per database -----------------------------------------------

SQL_IDENTITY=$(cat <<'SQL'
-- User bridge only (keeps roles, permissions, role_permissions, casbin_rule, …)
TRUNCATE TABLE user_sessions, permission_cache, user_roles, user_status_history,
  user_metadata, user_identity_mapping
RESTART IDENTITY CASCADE;
TRUNCATE TABLE change_log, outbox_events RESTART IDENTITY CASCADE;
SQL
)

# Single root truncate: all customer-* tables reference `customers` (directly or via chain).
SQL_CUSTOMER=$(cat <<'SQL'
TRUNCATE TABLE customers RESTART IDENTITY CASCADE;
TRUNCATE TABLE outbox_events RESTART IDENTITY CASCADE;
SQL
)

SQL_GLOBAL_PROFILE=$(cat <<'SQL'
TRUNCATE TABLE global_customer_events, pii_access_tokens, regional_profile_history,
  regional_profiles, business_authorized_signatories, regional_business_profiles,
  global_business_customers, global_customers
RESTART IDENTITY CASCADE;
REFRESH MATERIALIZED VIEW customer_360_view;
SQL
)

SQL_PII_VAULT=$(cat <<'SQL'
-- Keeps vault_config, pii_retention_policy (V2: pluralized pii_individuals, pii_access_audits)
TRUNCATE TABLE pii_access_audits, pii_deletion_log, pii_documents, pii_biometrics,
  pii_business, pii_individuals
RESTART IDENTITY CASCADE;
SQL
)

SQL_WALLET=$(cat <<'SQL'
TRUNCATE TABLE top_up_status_history, top_up_transactions, scheduled_debits, reserved_funds,
  balance_snapshots, wallet_movements, wallet_status_history, ledger_sync_status, wallets
RESTART IDENTITY CASCADE;
TRUNCATE TABLE change_log, outbox_events RESTART IDENTITY CASCADE;
SQL
)

SQL_KYC=$(cat <<'SQL'
TRUNCATE TABLE otp_verifications, session_status_history, verification_sessions, identity_mapping,
  provider_response_cache, provider_api_logs
RESTART IDENTITY CASCADE;
TRUNCATE TABLE change_log, outbox_events RESTART IDENTITY CASCADE;
SQL
)

SQL_LENDING=$(cat <<'SQL'
-- Keeps: banks, purpose_of_finance, eligibility_*, reschedule_configs, product ref data
TRUNCATE TABLE manual_approval_tasks, loan_reschedules RESTART IDENTITY CASCADE;
TRUNCATE TABLE loan_applications RESTART IDENTITY CASCADE;
TRUNCATE TABLE change_log, outbox_events RESTART IDENTITY CASCADE;
SQL
)

SQL_COLLECT=$(cat <<'SQL'
-- Keeps: dunning_policies, delinquency_rules, early_settlement_configs
TRUNCATE TABLE payment_allocations, payment_status_history, payments,
  installment_status_history, installments, dunning_actions, dunning_cases,
  settlements, penalty_waiver_requests, write_off_records, penalty_waivers,
  delinquency_rule_audit, repayment_schedules
RESTART IDENTITY CASCADE;
TRUNCATE TABLE change_log, outbox_events RESTART IDENTITY CASCADE;
SQL
)

SQL_LEDGER=$(cat <<'SQL'
-- Keeps: accounts (COA), coa_*, product_coa_* configuration
TRUNCATE TABLE journal_lines, entry_status_history, journal_entries,
  journal_entry_sync_log, account_balances, gl_reconciliation_records, sync_failures,
  idempotency_keys, fineract_loan_mappings, fineract_customer_mappings,
  fineract_account_mappings, accrual_schedules, period_closes,
  fineract_audit_log, fineract_proxy_idempotency, product_coa_audit_log
RESTART IDENTITY CASCADE;
TRUNCATE TABLE change_log, outbox_events RESTART IDENTITY CASCADE;
SQL
)

SQL_RISK=$(cat <<'SQL'
-- Keeps: tenant_configs, lov_*, risk_parameters, scoring_thresholds, scenario_rules,
--        AML dimension tables, credit_scoring definitions, fraud_rules (risk copy), blacklists optional
TRUNCATE TABLE audit_entries, review_tasks, assessment_answers, assessment_sessions,
  entity_status_records, aml_risk_assessments,
  fraud_case_actions, fraud_case_alerts, fraud_cases, fraud_alerts, fraud_evaluations,
  fraud_events, session_events, fraud_user_profiles,
  account_locks, device_registry, velocity_checks, watchlist_entries,
  screening_results, fraud_signal_logs, scoring_model_results, risk_assessments
RESTART IDENTITY CASCADE;
TRUNCATE TABLE change_log, outbox_events RESTART IDENTITY CASCADE;
SQL
)

SQL_FRAUD=$(cat <<'SQL'
-- Keeps fraud_rules; clears runtime fraud data and optional blacklist rows for a clean slate
TRUNCATE TABLE fraud_case_actions, fraud_case_alerts, fraud_cases, fraud_alerts,
  fraud_evaluations, fraud_events, session_events, fraud_user_profiles,
  device_blacklist, country_blacklist, iban_blacklist
RESTART IDENTITY CASCADE;
SQL
)

SQL_MIDDLEWARE=$(cat <<'SQL'
TRUNCATE TABLE callback_responses, api_request_logs,
  client_request_prod, client_request_dev, client_request_test
RESTART IDENTITY CASCADE;
SQL
)

# ---------------------------------------------------------------------------

info "PGHOST=${PGHOST} PGPORT=${PGPORT} PGUSER=${PGUSER} DRY_RUN=${DRY_RUN}"

run_sql "identity_db" "$SQL_IDENTITY"
run_sql "customer_db" "$SQL_CUSTOMER"
run_sql "global_profile_db" "$SQL_GLOBAL_PROFILE"
run_sql "pii_vault_db" "$SQL_PII_VAULT"
run_sql "wallet_db" "$SQL_WALLET"
run_sql "kyc_adapter_db" "$SQL_KYC"
run_sql "lending_db" "$SQL_LENDING"
run_sql "collections_db" "$SQL_COLLECT"
run_sql "ledger_db" "$SQL_LEDGER"
run_sql "risk_service" "$SQL_RISK"
run_sql "fraud_service_db" "$SQL_FRAUD"
run_sql "middleware_third_party_db" "$SQL_MIDDLEWARE"

info "Done. Clear Keycloak test users if logins should match fresh identity mappings."
