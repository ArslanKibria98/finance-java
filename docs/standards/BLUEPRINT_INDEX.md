# Blueprint Index - Which Docs Apply to Which Domain

> Every agent MUST consult the relevant blueprint docs before implementing. This index maps domains to their authoritative blueprint documents.

## Blueprint Path
`/var/www/docs/islamic-financing/master-blueprint/`

## Quick Reference Matrix

| If You're Working On... | Read These Blueprints (in order) |
|------------------------|----------------------------------|
| **Any service** | `01_ARCHITECTURE_PRINCIPLES.md`, `02_TECHNOLOGY_STACK.md` |
| **New microservice** | `04_MICROSERVICE_DECOMPOSITION.md`, `12_JAVA_OPTIMIZED_ARCHITECTURE.md` |
| **Domain modeling** | `01_ARCHITECTURE_PRINCIPLES.md`, `04_MICROSERVICE_DECOMPOSITION.md` |
| **Sharia/Islamic finance** | `05_SHARIA_COMPLIANCE_ENGINE.md`, `12_PRODUCT_CONFIGURATION_ENGINE.md` |
| **Temporal workflows** | `06_ORCHESTRATION_PATTERNS.md`, `13_HARDENED_SAGA_PATTERNS.md` |
| **Fineract/banking** | `07_CORE_BANKING_ADAPTER.md` |
| **KSA gov APIs (Nafath/Simah)** | `08_KSA_GOVERNMENT_APIS.md` |
| **Observability/logging** | `09_OBSERVABILITY_OPERATIONS.md` |
| **Deployment/K8s** | `10_DEPLOYMENT_TOPOLOGY.md` |
| **Roles/permissions/ABAC** | `11_USER_ROLES_PERMISSIONS.md` |
| **Product config/pricing** | `12_PRODUCT_CONFIGURATION_ENGINE.md` |
| **SAGA/idempotency** | `13_HARDENED_SAGA_PATTERNS.md` |
| **SME financing** | `13_SME_WORKFLOWS.md` |
| **Partner management** | `14_PARTNER_MANAGEMENT.md` |
| **Database design** | `15_DATABASE_STRATEGY.md` |
| **Open banking** | `16_OPEN_BANKING_INTEGRATION.md` |
| **Loan servicing/restructuring** | `17_LOAN_SERVICING_RESTRUCTURING.md` |
| **SDK development** | `18_SDK_ECOSYSTEM.md` |
| **Global CRM/PII** | `19_GLOBAL_CRM_ARCHITECTURE.md`, `20_GLOBAL_CRM_SERVICE_DECOMPOSITION.md` |
| **Security/compliance** | `03_SECURITY_DATA_RESIDENCY.md`, `11_CRITICAL_AUDIT_REPORT.md` |

## Service → Blueprint Mapping

| Service | Primary Blueprint | Secondary Blueprints |
|---------|------------------|---------------------|
| `risk-service` | `04` (decomposition) | `03` (security), `08` (gov APIs), `11` (roles) |
| `kyc-adapter-service` | `08` (gov APIs) | `04`, `03`, `19` (global CRM) |
| `customer-service` | `04` (decomposition) | `19` (global CRM), `20` (CRM decomposition) |
| `wallet-service` | `07` (core banking) | `04`, `13` (SAGA) |
| `identity-service` | `03` (security) | `11` (roles), `19` (identity federation) |
| `global-profile-service` | `19` (global CRM) | `20` (CRM decomposition), `03` |
| `pii-vault-service` | `19` (global CRM) | `03` (security/data residency) |
| `onboarding-workflow-service` | `06` (orchestration) | `08` (gov APIs), `13` (SAGA) |
| `lending-service` | `04` (decomposition) | `05` (Sharia), `06` (orchestration), `07` (banking), `12` (product config), `17` (servicing) |
| `product-service` | `12` (product config) | `05` (Sharia), `04` |
| `credit-decisioning` | `04` (decomposition) | `08` (Simah), `16` (open banking) |
| `ledger-service` | `04` (decomposition) | `15` (database), `07` (banking) |
| `collections-service` | `04` (decomposition) | `06` (dunning workflow), `17` (servicing) |
| `notification-service` | `04` (decomposition) | `09` (operations) |
| `document-service` | `04` (decomposition) | `03` (encryption) |
| `audit-service` | `04` (decomposition) | `03` (security), `09` (observability) |
| `core-banking-adapter` | `07` (core banking) | `13` (SAGA/idempotency) |
| `partner-service` | `14` (partner mgmt) | `04`, `11` (roles) |
| `payment-adapter` | `04` (decomposition) | `08` (SADAD), `13` (SAGA) |
| `sharia-compliance` | `05` (Sharia engine) | `12` (product config) |
| `sme-service` | `13_SME` (SME workflows) | `04`, `08` (Waitik/Simah) |
| `restructuring-service` | `17` (loan servicing) | `05` (Ibra), `07` (banking) |

## ERD Docs Path
`/var/www/docs/islamic-financing/erd-docs/`

Every service has a corresponding ERD doc: `{service-name}.md`

## User Journey Docs Path
`/var/www/docs/islamic-financing/user journey/`

14 personas with step-by-step workflows.

## Product Requirements Path
`/var/www/docs/islamic-financing/ProdDocs/`

Business requirement specifications (BRS) for each module.
