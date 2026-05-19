# QA Environment (PM2)

Runs all 12 active microservices as PM2 processes alongside the existing
Docker dev stack on the same server. Dev (Docker) keeps ports `80XX`;
QA (PM2) uses `91XX` and isolated `*_qa` databases plus Temporal namespace `qa`.

## Port map

| Service | Dev (Docker) | QA (PM2) | DB |
|---|---|---|---|
| identity-service | 8083 | **9183** | identity_db_qa |
| customer-service | 8084 | **9184** | customer_db_qa |
| global-profile-service | 8085 | **9185** | global_profile_db_qa |
| kyc-adapter-service | 8087 | **9187** | kyc_adapter_db_qa |
| wallet-service | 8088 | **9188** | wallet_db_qa |
| risk-service | 8090 | **9190** | risk_service_qa |
| product-service | 8091 | **9191** | product_service_db_qa |
| fraud-service | 8092 | **9192** | fraud_service_db_qa |
| ledger-service | 8095 | **9195** | ledger_db_qa |
| notification-service | 8096 | **9196** | notification_db_qa |
| lending-service | 8097 | **9197** | lending_db_qa |
| collections-service | 8099 | **9199** | collections_db_qa |

Shared infra (re-used from Docker): Keycloak `:8080`, Postgres `:5432`,
Kafka `:9092`, Redis `:6379`, Temporal `:7233`, Fineract `:8443`.

## First-time setup

```bash
# 1. Build all 12 service JARs
./scripts/qa/build-all.sh

# 2. Start (creates QA DBs + Temporal namespace, then boots PM2)
./scripts/qa/start.sh

# 3. (Optional) survive reboot — generate systemd unit
pm2 startup
pm2 save
```

## Day-to-day

```bash
./scripts/qa/status.sh                    # PM2 list + health probe
./scripts/qa/start.sh                     # start all
./scripts/qa/start.sh customer-service    # start one
./scripts/qa/stop.sh                      # stop all
./scripts/qa/restart.sh lending-service   # restart one (after rebuild)

pm2 logs customer-service-qa              # tail logs
pm2 logs --lines 200                      # tail all
pm2 monit                                 # live dashboard
pm2 flush                                 # clear log files
```

Logs land in `logs/qa/<service>.{out,err}.log`.

## Isolation notes

- **DB**: each PM2 service has its own `*_qa` database; Flyway creates the
  schema on first boot.
- **Kafka**: consumer group becomes `<service>-qa` (driven by
  `SPRING_APPLICATION_NAME`), so PM2 and Docker do not steal each other's
  messages.
- **Temporal**: `TEMPORAL_NAMESPACE=qa` keeps workflows separate from dev.
- **Inter-service REST calls**: all `*_SERVICE_URL` env vars in
  `ecosystem.config.js` point at QA ports — risk-service-qa calls
  fraud-service-qa, not the Docker one. Services not in active modules
  (middleware-third-party, onboarding-workflow, pii-vault) still resolve
  to Docker.

## Rebuild + redeploy a single service

```bash
mvn -pl services/customer-service -am clean package -DskipTests
./scripts/qa/restart.sh customer-service
```
