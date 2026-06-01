/**
 * PM2 ecosystem for QA environment.
 *
 * Runs all 12 active microservices on QA ports (91XX) alongside Docker dev (80XX).
 * Each service uses a dedicated *_qa database, isolated Kafka consumer group
 * (suffix -qa), and Temporal namespace `qa` to prevent collision with Docker.
 *
 * Usage:
 *   pm2 start ecosystem.config.js               # start all
 *   pm2 start ecosystem.config.js --only customer-service-qa
 *   pm2 logs customer-service-qa
 *   pm2 stop ecosystem.config.js
 *   pm2 delete ecosystem.config.js
 */

const PROJECT_ROOT = __dirname;
const LOG_DIR = `${PROJECT_ROOT}/logs/qa`;

// Shared infra (re-use Docker containers)
const sharedEnv = {
  // JVM
  // -D system properties override the hardcoded application.yml values (e.g. casbin
  // cache key prefix is hardcoded in every service's yaml, so KSA_AUTHORIZATION_*
  // env vars don't override it — JVM -D does).
  JAVA_TOOL_OPTIONS: '-Xms256m -Xmx768m -XX:+ExitOnOutOfMemoryError -Dksa.authorization.cache-key-prefix=casbin:policies:qa',
  // logback-spring.xml only attaches loggers for dev/docker/prod profiles —
  // we keep `docker` so Spring Boot logs aren't silently dropped. ENV=qa
  // tags log entries so Kibana can still filter QA from dev.
  SPRING_PROFILES_ACTIVE: 'docker',
  ENV: 'qa',

  // Postgres (Docker)
  DB_HOST: 'localhost',
  DB_PORT: '5432',
  DB_USERNAME: 'postgres',
  DB_PASSWORD: 'postgres',

  // Redis (Docker)
  REDIS_HOST: 'localhost',
  REDIS_PORT: '6379',
  SPRING_DATA_REDIS_HOST: 'localhost',
  SPRING_DATA_REDIS_PORT: '6379',

  // Kafka (Docker)
  KAFKA_BOOTSTRAP_SERVERS: 'localhost:9092',
  SCHEMA_REGISTRY_URL: 'http://localhost:8082',

  // Temporal (Docker) — isolated namespace for QA
  TEMPORAL_ADDRESS: 'localhost:7233',
  TEMPORAL_NAMESPACE: 'qa',

  // Keycloak (Docker, but PM2 runs on host so use localhost — Docker hostname `keycloak`
  // is unresolvable from host network).
  // BASE_URL is browser-facing (used to build SSO authUrl returned to client) → must be public.
  // AUTH_SERVER_URL is server-to-server (token exchange, JWK fetch) → stays localhost.
  // QA uses dedicated realm `CompanyRealm-QA` (isolated from dev `CompanyRealm`) so QA
  // user data (tokens, sessions, registrations) cannot bleed into dev and vice versa.
  KEYCLOAK_BASE_URL: 'http://46.62.226.94:8080',
  KEYCLOAK_AUTH_SERVER_URL: 'http://localhost:8080',  // IDS reads this for token + JWK endpoints
  KEYCLOAK_ISSUER_URI: 'http://localhost:8080/realms/CompanyRealm-QA',  // QA-specific issuer
  KEYCLOAK_JWK_SET_URI: 'http://localhost:8080/realms/CompanyRealm-QA/protocol/openid-connect/certs',
  KEYCLOAK_REALM: 'CompanyRealm-QA',
  KEYCLOAK_ADMIN_CLIENT_ID: 'admin-dashboard',
  KEYCLOAK_ADMIN_CLIENT_SECRET: 'admin-dashboard-secret',

  // Fineract (Docker)
  FINERACT_BASE_URL: 'https://localhost:8443/fineract-provider/api/v1',

  // Inter-service QA URLs (PM2 ports)
  IDENTITY_SERVICE_URL: 'http://localhost:9183',
  CUSTOMER_SERVICE_URL: 'http://localhost:9184',
  GLOBAL_PROFILE_SERVICE_URL: 'http://localhost:9185',
  KYC_SERVICE_URL: 'http://localhost:9187',
  KYC_ADAPTER_URL: 'http://localhost:9187',  // onboarding-workflow-service uses this name
  WALLET_SERVICE_URL: 'http://localhost:9188',
  RISK_SERVICE_URL: 'http://localhost:9190',
  PRODUCT_SERVICE_URL: 'http://localhost:9191',
  FRAUD_SERVICE_URL: 'http://localhost:9192',
  LEDGER_SERVICE_URL: 'http://localhost:9195',
  NOTIFICATION_SERVICE_URL: 'http://localhost:9196',
  LENDING_SERVICE_URL: 'http://localhost:9197',
  COLLECTIONS_SERVICE_URL: 'http://localhost:9199',

  // Services not in active modules — still point to Docker
  MIDDLEWARE_SERVICE_URL: 'http://localhost:8093',
  ONBOARDING_SERVICE_URL: 'http://localhost:8089',
  PII_VAULT_SERVICE_URL: 'http://localhost:8086',

  // Logging — host-side PM2 can't resolve Docker hostname "logstash";
  // point to the host-mapped port instead.
  LOGSTASH_HOST: 'localhost',
  LOGSTASH_PORT: '5000',
  LOG_LEVEL: 'INFO',
  APP_LOG_LEVEL: 'DEBUG',

  // Auth — isolated Casbin policy cache so QA IDS doesn't overwrite dev's Redis keys
  KSA_AUTHORIZATION_ENABLED: 'true',
  KSA_AUTHORIZATION_CACHE_KEY_PREFIX: 'casbin:policies:qa',

  // Redis namespace isolation for risk-service velocity + foundational-infra-sdk blacklist.
  // Without these, dev and qa share the same Redis keys → blacklisted device/IP/NID in
  // either env affects the other; velocity counters get double-incremented.
  KSA_RISK_VELOCITY_PREFIX: 'qa:velocity:',
  KSA_BLACKLIST_CACHE_KEY_PREFIX: 'qa:blacklist',

  // CORS — QA browser portals served from 65.108.31.172:7380 (alongside existing 7374).
  // Without this override, services fall back to their application.yml defaults which
  // include 7374 but not 7380 → preflight returns "Invalid CORS request".
  CORS_ALLOWED_ORIGINS: 'http://localhost:3000,http://localhost:3001,http://localhost:4200,http://46.62.226.94:3000,http://46.62.226.94:4200,http://65.108.31.172:7374,http://65.108.31.172:7380',
};

function service(name, port, dbName, extraEnv = {}) {
  return {
    name: `${name}-qa`,
    cwd: `${PROJECT_ROOT}/services/${name}`,
    script: 'java',
    args: `-jar target/${name}-1.0.0-SNAPSHOT.jar`,
    interpreter: 'none',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    kill_timeout: 30000,
    listen_timeout: 120000,
    out_file: `${LOG_DIR}/${name}.out.log`,
    error_file: `${LOG_DIR}/${name}.err.log`,
    merge_logs: true,
    time: true,
    env: {
      ...sharedEnv,
      SERVER_PORT: String(port),
      SPRING_APPLICATION_NAME: `${name}-qa`,
      DB_NAME: dbName,
      ...extraEnv,
    },
  };
}

module.exports = {
  apps: [
    // gRPC server (from messaging-event-sdk) defaults to 9090 — collides with host Prometheus.
    // Assign per-service QA gRPC ports in the 192XX range for services that pull the gRPC starter.
    service('identity-service',        9183, 'identity_db_qa', { GRPC_SERVER_PORT: '19183' }),
    service('customer-service',        9184, 'customer_db_qa', { GRPC_SERVER_PORT: '19184' }),
    service('global-profile-service',  9185, 'global_profile_db_qa'),
    service('kyc-adapter-service',     9187, 'kyc_adapter_db_qa'),
    service('wallet-service',          9188, 'wallet_db_qa'),
    service('risk-service',            9190, 'risk_service_qa'),
    service('product-service',         9191, 'product_service_db_qa', { TEMPORAL_TASK_QUEUE: 'product-activation-queue-qa' }),
    service('fraud-service',           9192, 'fraud_service_db_qa'),
    service('ledger-service',          9195, 'ledger_db_qa'),
    // notification-service uses single DATABASE_URL env var, not split
    service('notification-service',    9196, 'notification_db_qa', {
      DATABASE_URL: 'jdbc:postgresql://localhost:5432/notification_db_qa',
      GRPC_SERVER_PORT: '19196',
    }),
    service('lending-service',         9197, 'lending_db_qa', { TEMPORAL_TASK_QUEUE: 'loan-application-queue-qa' }),
    service('collections-service',     9199, 'collections_db_qa'),
    // onboarding-workflow-service uses SPRING_DATASOURCE_URL (single var, like notification)
    service('onboarding-workflow-service', 9189, 'onboarding_db_qa', {
      SPRING_DATASOURCE_URL: 'jdbc:postgresql://localhost:5432/onboarding_db_qa?stringtype=unspecified',
    }),
  ],
};
