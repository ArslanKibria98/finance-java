# CLAUDE.md - KSA Islamic Financing Platform

> **Single Source of Truth** for AI-assisted development with Claude Code.
> Every agent, skill, and tool MUST read and follow this document before writing any code.

## Communication Style — Caveman Mode

Respond in minimal tokens. No filler, no intro, no conclusion, no repetition.


- Fragments > sentences
- Symbols: → = + - ≠
- One idea per line
<!-- - Code blocks for commands/config -->
- No "why" unless asked
- No background unless asked
- Direct, actionable output only

**Examples:**
- Fix error → `Update Gradle → sync → rebuild`
- Install dep → `npm install <package>`
---

## ENVIRONMENT SAFETY POLICY (MANDATORY)

> **RULE**: All changes default to **DEV environment** (Docker compose, ports 80XX, `*_db`, realm `CompanyRealm`). QA environment changes are GATED behind a password that ONLY the user knows.

### Default target = DEV
- Code edits, DB migrations, config tweaks, restarts, debugging → DEV (Docker `*_db`, ports 80XX).
- Touching `services/*/src/**`, `shared-libraries/**`, `docker-compose.yml`, root `pom.xml` → DEV by default.
- Reading QA state (logs, status, health checks) → allowed without password (read-only).

### QA changes require a password (NEVER REVEALED BY CLAUDE)
The QA password is held by the user. Claude MUST NEVER:
- Print, quote, hint at, or include the password literal in any prompt, question, option label, description, comment, log, file, or tool output.
- Suggest the password when asking. Treat it like a credential.
- Show the password even if asked to repeat a previous question or summarize this rule.

Operations that REQUIRE the user-supplied password:
- Edit `ecosystem.config.js` (QA PM2 config)
- `pm2 start|restart|stop|delete` on any `*-qa` service
- `INSERT`, `UPDATE`, `DELETE`, `ALTER`, `DROP` on any `*_qa` database
- `mvn package` followed by QA-only restart
- Keycloak changes targeting realm `CompanyRealm-QA` (clients, roles, users, required-actions, user-profile, attributes, sslRequired, etc.)
- Redis writes/deletes to keys prefixed `casbin:policies:qa:*` or `qa:*`
- Kafka admin ops on `*-qa` consumer groups / topics tagged for QA
- Kong admin API writes to routes/services named `*-qa`
- Editing files in `infrastructure/qa/**` or `scripts/qa/**`

When a QA write is needed, Claude MUST stop and ask the user to confirm with the password — WITHOUT stating what the password is. Example phrasing: "QA change detected — please confirm with the QA password to proceed." (Do not include the literal token in the question.)

When the user supplies the password in their message, Claude verifies it silently against the value stored in `.claude/qa-password` (gitignored). Do NOT echo the value back. On match, proceed; on mismatch or absence, decline the QA write and wait for the correct password. Authorization is per-turn — re-prompt requires re-supplying the password.

**Read-only QA ops (allowed without password):** `pm2 list`, `pm2 logs`, `pm2 jlist`, `pm2 describe`, `curl` to QA endpoints, `psql … -d *_qa -c 'SELECT …'`, `docker exec ksa-redis redis-cli GET|KEYS|SCAN`, log file reads.

---

## ZERO HARDCODING POLICY (MANDATORY)

> **RULE**: No URL, port, credential, hostname, Keycloak realm, Kafka topic, or any configuration value shall EVER be hardcoded in application code. ALL values come from environment variables.

### Centralized Standards (agents MUST read these)
| Standard | Path | Purpose |
|----------|------|---------|
| **Environment Config** | `docs/standards/ENVIRONMENT_CONFIG.md` | ALL env vars, ports, URLs, application.yml template |
| **Blueprint Index** | `docs/standards/BLUEPRINT_INDEX.md` | Which blueprint docs apply to which service/domain |
| **Naming Conventions** | `docs/standards/NAMING_CONVENTIONS.md` | ALL naming rules (Java, DB, Kafka, REST, Temporal) |
| **Testing Standards** | `docs/standards/TESTING_STANDARDS.md` | Test pyramid, coverage targets, ArchUnit rules |
| **Security Architecture** | `docs/standards/SECURITY_ARCHITECTURE.md` | ABAC, roles, encryption, audit trail |
| **Resilience Standards** | `docs/standards/RESILIENCE_STANDARDS.md` | Circuit breakers, retries, timeouts, SAGA, idempotency |

### How Config Works
```yaml
# CORRECT: Environment variable with default
spring.datasource.url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:service_db}
keycloak.issuer: ${KEYCLOAK_BASE_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:islamic-financing}
temporal.address: ${TEMPORAL_ADDRESS:localhost:7233}

# WRONG: Hardcoded values
spring.datasource.url: jdbc:postgresql://localhost:5432/lending_db     # NEVER
keycloak.issuer: http://localhost:8080/realms/islamic-financing         # NEVER
temporal.address: localhost:7233                                        # NEVER
```

### What Gets Externalized
| Category | Example | Env Var |
|----------|---------|---------|
| DB Host/Port | localhost:5432 | `${DB_HOST}:${DB_PORT}` |
| DB Credentials | postgres/postgres | `${DB_USERNAME}/${DB_PASSWORD}` |
| Keycloak URL | http://localhost:8080 | `${KEYCLOAK_BASE_URL}` |
| Keycloak Realm | islamic-financing | `${KEYCLOAK_REALM}` |
| Kafka Brokers | localhost:9092 | `${KAFKA_BOOTSTRAP_SERVERS}` |
| Schema Registry | http://localhost:8082 | `${SCHEMA_REGISTRY_URL}` |
| Temporal Address | localhost:7233 | `${TEMPORAL_ADDRESS}` |
| Redis Host/Port | localhost:6379 | `${REDIS_HOST}:${REDIS_PORT}` |
| Fineract URL | https://localhost:8443/... | `${FINERACT_BASE_URL}` |
| Service Port | 8090 | `${SERVER_PORT}` |
| Log Level | DEBUG | `${LOG_LEVEL}` |

See `docs/standards/ENVIRONMENT_CONFIG.md` for complete registry.

---

## PROJECT IDENTITY

- **Name**: KSA Islamic Financing Platform
- **Type**: Enterprise-grade microservices monorepo
- **Domain**: SAMA-compliant Islamic financing for Kingdom of Saudi Arabia
- **Version**: 1.0.0-SNAPSHOT
- **Java**: 21 LTS (OpenJDK/GraalVM 21.0.10)
- **Spring Boot**: 4.0.2
- **Spring Cloud**: 2024.0.0
- **Build**: Maven 3.9+
- **Root**: `/var/www/islamic-financing-platform`
- **Docs**: `/var/www/docs` and `/var/www/islamic-financing-platform/docs/`
- **License**: Proprietary - All Rights Reserved
- **Target Market**: Kingdom of Saudi Arabia (KSA)
- **Regulatory Bodies**: SAMA, Sharia Board, PDPL, NCA, ZATCA
- **Multi-Tenancy**: Schema-per-tenant isolation
- **Data Residency**: All PII must remain within KSA borders
- **Supported Languages**: English (en), Arabic (ar, ar-SA)
- **Currency**: Saudi Riyal (SAR) - BigDecimal only
- **Calendar**: Dual support - Gregorian + Hijri (via compliance-localization-sdk)

---

## AGENTIC FRAMEWORK

> **Two-Tier System**: Agents (autonomous workers) live in `.claude/agents/`. Skills (user-invocable workflows) live in `.claude/commands/`. They are NOT the same thing.

### Agents vs Skills — Key Differences
| | Agents (`.claude/agents/`) | Skills (`.claude/commands/`) |
|---|---|---|
| **What** | Autonomous workers with isolated context | User-invocable inline workflows |
| **Invocation** | Claude spawns them via Task tool | User types `/skill-name` |
| **Context** | Own isolated context window | Runs in current conversation |
| **Count** | 12 (team leads + leadership) | 14 (specialist tasks) |
| **Role** | Coordinate, plan, review, decide | Execute specific technical tasks |

### Team Hierarchy (12 Agents in `.claude/agents/`)
```
                         ┌──────────────┐
                         │     CTO      │
                         │  [agent]     │
                         └──────┬───────┘
                                │
        ┌───────────┬───────────┼───────────┬───────────┐
        │           │           │           │           │
   ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
   │  Arch   │ │ Domain  │ │ Integr  │ │  API    │ │   QA    │
   │  Team   │ │  Team   │ │  Team   │ │  Team   │ │  Team   │
   │ [agent] │ │ [agent] │ │ [agent] │ │ [agent] │ │ [agent] │
   └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
        │           │           │           │           │
   ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
   │ DevOps  │ │Complian │ │Feature  │ │ Sprint  │ │Incident │
   │  Team   │ │  Team   │ │  Team   │ │ Planner │ │Commander│
   │ [agent] │ │ [agent] │ │ [agent] │ │ [agent] │ │ [agent] │
   └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
```

### Agents Reference (12 — `.claude/agents/`)
| Agent File | Role | Responsibility |
|------------|------|----------------|
| `cto.md` | Chief Technology Officer | Top-level orchestrator, strategic decisions, cross-team coordination |
| `arch-team.md` | Principal Architect | Architecture DESIGN decisions, ADRs, SDK ecosystem |
| `domain-team.md` | Domain Engineering Lead | DDD aggregates, Sharia logic, bounded contexts |
| `integration-team.md` | Integration Lead | Temporal workflows, Kafka events, Fineract, Gov APIs |
| `api-team.md` | API Lead | REST endpoints, Kong gateway, OpenAPI, ABAC security |
| `qa-team.md` | QA Lead | Testing strategy, coverage targets, quality gates |
| `devops-team.md` | DevOps Lead | Docker, K8s, CI/CD, observability, infrastructure |
| `compliance-team.md` | Compliance Lead | SAMA, Sharia, PDPL, NCA, security hardening strategy |
| `feature-team.md` | Feature Delivery Lead | Cross-functional feature coordination (plan + delegate) |
| `code-reviewer.md` | Senior Code Reviewer | Peer code quality reviews (naming, style, zero-hardcoding) |
| `sprint-planner.md` | Scrum Master | Sprint planning, story breakdown, prioritization |
| `incident-commander.md` | Incident Commander | Full incident lifecycle (severity, triage, resolution, post-mortem) |

### Skills Reference (14 — `.claude/commands/`)
| Skill | Invocation | Use When |
|-------|------------|----------|
| `/new-service` | User types `/new-service` | Create new microservice from template |
| `/domain-model` | User types `/domain-model` | Design DDD aggregate/entity/VO |
| `/temporal-workflow` | User types `/temporal-workflow` | Create Temporal workflow + SAGA |
| `/api-endpoint` | User types `/api-endpoint` | Create REST endpoint with security |
| `/flyway-migration` | User types `/flyway-migration` | Create database migration |
| `/kafka-event` | User types `/kafka-event` | Create Kafka event pipeline |
| `/write-tests` | User types `/write-tests` | Write unit/integration/arch tests |
| `/sharia-calculator` | User types `/sharia-calculator` | Islamic finance calculations |
| `/review-arch` | User types `/review-arch` | Hexagonal/ArchUnit compliance audit |
| `/docker-service` | User types `/docker-service` | Add service to Docker Compose |
| `/fineract-integration` | User types `/fineract-integration` | Fineract core banking integration |
| `/implement-feature` | User types `/implement-feature` | End-to-end feature implementation |
| `/debug-service` | User types `/debug-service` | Diagnose and fix service issues |
| `/compliance-check` | User types `/compliance-check` | SAMA/Sharia quick compliance audit |

### How to Use

**Complex multi-team work** — Claude spawns the CTO agent, which coordinates team agents:
```
"Design and implement the complete lending-service with Murabaha support"
→ CTO agent → delegates to domain-team, integration-team, api-team agents
```

**Team-specific work** — Claude spawns the relevant team agent directly:
```
"Design LoanAggregate with Murabaha calculation support"
→ domain-team agent spawned
```

**Focused specialist tasks** — User invokes skills directly:
```
/implement-feature lending-service: Murabaha loan origination
/new-service lending-service
/domain-model LoanAggregate with disbursement and repayment
/temporal-workflow LoanOriginationWorkflow with SAGA
/kafka-event financing.loan.disbursed
/api-endpoint POST /api/v1/loans
/flyway-migration lending_db initial schema
/write-tests LoanAggregate domain model
/sharia-calculator Murabaha profit calculation
/review-arch lending-service
/docker-service lending-service
/fineract-integration loan account creation
/compliance-check lending-service
/debug-service lending-service startup failure
```

### CRITICAL: Scope Boundaries (Prevent Duplication & Wrong Paths)

**Architecture Review (3 tools — DISTINCT purposes, NEVER overlap):**
```
code-reviewer agent  = PEER code quality review (naming, style, security, conventions)
/review-arch skill   = AUTOMATED ArchUnit compliance check (hexagonal rules, dependency direction)
arch-team agent      = Architecture DESIGN decisions (ADRs, SDK design, pattern selection)
```

**Compliance (2 tools — DISTINCT scope):**
```
/compliance-check skill  = Quick code-level audit (scan specific code for violations)
compliance-team agent    = Full compliance STRATEGY (policy, implementation, certification)
```

**Debugging (2 tools — DISTINCT scope):**
```
/debug-service skill        = Debug a SPECIFIC service issue (single service focus)
incident-commander agent    = Full incident lifecycle (severity, triage, coordination, post-mortem)
```

**Feature Implementation (2 tools — DISTINCT roles):**
```
feature-team agent      = COORDINATE cross-functional feature (plan, delegate, validate)
/implement-feature skill = EXECUTE implementation across hexagonal layers (code directly)
```

### Agent Must-Read Chain
Every agent MUST read these docs in order before doing ANYTHING:
```
1. CLAUDE.md (this file)                               → Project rules
2. docs/standards/BLUEPRINT_INDEX.md                   → Find relevant blueprints
3. docs/standards/ENVIRONMENT_CONFIG.md                → Zero hardcoding rules
4. docs/standards/NAMING_CONVENTIONS.md                → Naming rules
5. [Relevant Blueprint from step 2]                    → Domain understanding
6. docs/islamic-financing/erd-docs/{service}.md        → Database schema
```

Additional per-task:
- Writing tests → `docs/standards/TESTING_STANDARDS.md`
- Security/auth → `docs/standards/SECURITY_ARCHITECTURE.md`
- Integration → `docs/standards/RESILIENCE_STANDARDS.md`

---

## WORKFLOW REFERENCE

### End-to-End Workflows (Common Scenarios)

#### Workflow 1: Build a New Service (e.g., lending-service)
```
User: "Build lending-service with Murabaha support"

┌─────────────────────────────────────────────────────────────────────┐
│ Phase 1: PLANNING                                                   │
│                                                                     │
│  CTO agent                                                          │
│  ├── Reads: CLAUDE.md → BLUEPRINT_INDEX.md → ENVIRONMENT_CONFIG.md  │
│  ├── Identifies: blueprints 05, 07, 12, 17 for lending              │
│  ├── Creates: Execution Plan with team assignments                   │
│  └── Delegates to teams in order ↓                                  │
│                                                                     │
│  sprint-planner agent (optional)                                    │
│  ├── Reads: SPRINT_PLAN.md → STORIES.md → Master_BRS.md            │
│  └── Breaks into stories: domain → integration → API → tests        │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 2: DOMAIN DESIGN                                              │
│                                                                     │
│  domain-team agent                                                  │
│  ├── Reads: Blueprint 05 (Sharia), ERD lending.md, BRS docs         │
│  ├── Designs: LoanAggregate, Murabaha calculation, bounded context  │
│  ├── Delegates: /new-service lending-service                        │
│  │   └── Scaffolds hexagonal structure, pom.xml, Dockerfile         │
│  ├── Delegates: /domain-model LoanAggregate                         │
│  │   └── Creates aggregate, VOs, ports, events, status enum         │
│  └── Delegates: /sharia-calculator Murabaha profit                  │
│      └── Implements MurabahaCalculator with BigDecimal               │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 3: INTEGRATION & WORKFLOW                                     │
│                                                                     │
│  integration-team agent                                             │
│  ├── Reads: Blueprint 06, 13 (orchestration, SAGA), RESILIENCE.md  │
│  ├── Designs: LoanOriginationWorkflow with SAGA compensation        │
│  ├── Delegates: /temporal-workflow LoanOriginationWorkflow           │
│  │   └── Creates workflow + activities + compensation + timeouts     │
│  ├── Delegates: /kafka-event financing.loan.disbursed               │
│  │   └── Creates Avro schema, producer, consumer, DLQ               │
│  └── Delegates: /fineract-integration loan account creation         │
│      └── Creates Fineract adapter with circuit breaker               │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 4: API LAYER                                                  │
│                                                                     │
│  api-team agent                                                     │
│  ├── Reads: SECURITY_ARCHITECTURE.md, NAMING_CONVENTIONS.md         │
│  ├── Designs: REST endpoints with ABAC, OpenAPI spec                │
│  └── Delegates: /api-endpoint POST /api/v1/loans                    │
│      └── Creates controller, request/response records, @SecuredEndpoint│
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 5: INFRASTRUCTURE                                             │
│                                                                     │
│  devops-team agent                                                  │
│  ├── Reads: ENVIRONMENT_CONFIG.md, Blueprint 09, 10                 │
│  ├── Delegates: /flyway-migration lending_db initial schema         │
│  │   └── Creates V1__initial_schema.sql with tenant_id, enums       │
│  └── Delegates: /docker-service lending-service                     │
│      └── Adds to docker-compose.yml with ${ENV_VAR} config          │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 6: TESTING                                                    │
│                                                                     │
│  qa-team agent                                                      │
│  ├── Reads: TESTING_STANDARDS.md, coverage targets                  │
│  ├── Designs: test strategy (unit → integration → arch)             │
│  └── Delegates: /write-tests lending-service                        │
│      ├── Domain aggregate tests (>= 90% coverage)                   │
│      ├── Use case tests (mocked ports)                              │
│      ├── Integration tests (Testcontainers)                         │
│      └── ArchUnit test (5 mandatory rules)                          │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 7: VALIDATION (Quality Gates - ALL must pass)                 │
│                                                                     │
│  /review-arch skill → Hexagonal compliance check                    │
│  ├── Domain no Spring imports?              ✓/✗                     │
│  ├── Dependencies inward only?              ✓/✗                     │
│  ├── ArchUnit test exists and passes?       ✓/✗                     │
│  └── Package structure correct?             ✓/✗                     │
│                                                                     │
│  code-reviewer agent → Peer code quality                            │
│  ├── Naming follows NAMING_CONVENTIONS?     ✓/✗                     │
│  ├── @RequiredArgsConstructor (not @Autowired)?  ✓/✗                │
│  ├── BigDecimal for money?                  ✓/✗                     │
│  └── Zero hardcoded values?                 ✓/✗                     │
│                                                                     │
│  /compliance-check skill → SAMA/Sharia audit                       │
│  ├── SecurityConfig with Keycloak?          ✓/✗                     │
│  ├── @SecuredEndpoint on endpoints?          ✓/✗                     │
│  ├── No PII in logs?                        ✓/✗                     │
│  └── Sharia rules: BigDecimal, charity fund? ✓/✗                   │
└─────────────────────────────────────────────────────────────────────┘
```

#### Workflow 2: Add Feature to Existing Service
```
User: "Add repayment processing to lending-service"

  feature-team agent (coordinator)
  │
  ├── 1. Reads BLUEPRINT_INDEX.md → finds blueprints 05, 07, 17
  ├── 2. Reads blueprints + ERD + user journeys
  ├── 3. Creates feature plan with team assignments
  │
  ├── 4. domain-team agent
  │      └── /domain-model RepaymentEntity inside LoanAggregate
  │
  ├── 5. integration-team agent
  │      ├── /temporal-workflow RepaymentProcessingWorkflow
  │      └── /kafka-event financing.loan.repayment.received
  │
  ├── 6. api-team agent
  │      └── /api-endpoint POST /api/v1/loans/{id}/repayments
  │
  ├── 7. devops-team agent
  │      └── /flyway-migration lending_db add repayment tables
  │
  ├── 8. qa-team agent
  │      └── /write-tests repayment processing
  │
  └── 9. Validation chain
         ├── /review-arch lending-service
         ├── code-reviewer agent
         └── /compliance-check lending-service
```

#### Workflow 3: Quick Fix / Single Task
```
User: "/implement-feature lending-service: add early settlement with Ibra"

  /implement-feature skill (executes directly, no coordination)
  │
  ├── 1. Reads: CLAUDE.md → BLUEPRINT_INDEX → ENVIRONMENT_CONFIG → NAMING → TESTING → SECURITY
  ├── 2. Reads: Blueprint 05 (Sharia: Ibra rules), ERD lending.md
  │
  ├── 3. Domain Layer:     EarlySettlementVO, Ibra calculation, aggregate method
  ├── 4. Application Layer: SettleLoanUseCaseImpl, DTOs, mapper
  ├── 5. Infrastructure:    JPA entity update, Flyway migration, Kafka event
  ├── 6. Adapter:           REST endpoint POST /api/v1/loans/{id}/settle
  ├── 7. Config:            application.yml (zero hardcoding)
  └── 8. Tests:             Domain + Use case + Integration + ArchUnit
```

#### Workflow 4: Debug / Incident
```
Scenario A — Single service issue:
  User: "/debug-service lending-service startup failure"
  └── /debug-service skill
      ├── Checks: logs, config, ports, dependencies, DB connectivity
      ├── Identifies root cause
      └── Applies minimal fix (no hardcoded values)

Scenario B — Production incident:
  User: "lending-service 500 errors on disbursement in production"
  └── incident-commander agent
      ├── 1. TRIAGE: Severity assessment (SEV-1 to SEV-4)
      ├── 2. DIAGNOSE: Delegates to /debug-service for deep analysis
      ├── 3. RESOLVE: Minimal fix (no hardcoded values)
      └── 4. POST-MORTEM: Root cause, prevention, action items
```

#### Workflow 5: Sprint Planning
```
User: "Plan the lending-service MVP sprint"

  sprint-planner agent
  ├── Reads: SPRINT_PLAN.md → STORIES.md → Master_BRS.md → BLUEPRINT_INDEX.md
  ├── Breaks epic into stories with priority (P0-P3)
  ├── Assigns stories to team agents:
  │   ├── P0: domain-team → Murabaha aggregate + Sharia calculations
  │   ├── P0: compliance-team → SAMA compliance verification
  │   ├── P1: integration-team → Loan origination workflow + SAGA
  │   ├── P1: api-team → Loan management REST endpoints
  │   ├── P2: devops-team → Docker + Flyway + observability
  │   └── P2: qa-team → Test strategy + quality gates
  └── Outputs: Sprint backlog with dependencies and acceptance criteria
```

#### Workflow 6: Compliance Strategy
```
User: "Ensure lending-service meets all SAMA requirements"

  compliance-team agent (strategy)
  ├── Reads: SECURITY_ARCHITECTURE.md, Blueprint 03, 05, 11
  ├── Defines: Compliance requirements matrix
  ├── Delegates: /compliance-check lending-service (quick scan)
  │   └── Returns: COMPLIANT / NON-COMPLIANT with specific violations
  ├── If violations found:
  │   ├── Security fixes → code-reviewer agent verifies
  │   ├── Sharia fixes → domain-team agent implements
  │   └── Data protection → devops-team agent configures
  └── Outputs: Compliance certification report
```

### Agent Internal Workflow Patterns

#### Pattern A: Coordinator Agent (CTO, feature-team, sprint-planner)
```
Read mandatory docs → Analyze scope → Create execution plan
→ Delegate to team agents (in dependency order)
→ Team agents delegate to specialist skills
→ Validation chain: /review-arch → code-reviewer → /compliance-check
```

#### Pattern B: Team Lead Agent (domain, integration, api, qa, devops, compliance)
```
Read mandatory docs + context-specific blueprints + ERD
→ Design solution within team scope
→ Delegate atomic tasks to specialist /skills
→ Verify output matches blueprint specs
```

#### Pattern C: Review Agent (code-reviewer, arch-team, incident-commander)
```
Read mandatory docs → Read target code/service
→ Apply checklist (naming, security, architecture, hardcoding)
→ Produce structured report with file:line references
→ Verdict: APPROVED / CHANGES REQUESTED / NEEDS REWORK
```

#### Pattern D: Specialist Skill (/implement-feature, /new-service, etc.)
```
Read mandatory docs (6 standards + relevant blueprints)
→ Execute implementation (inside-out: domain → app → infra → adapter)
→ Apply zero-hardcoding (${ENV_VAR:default} everywhere)
→ Run checklist → Output result
```

### Agent → Skill Delegation Map

| Agent | Delegates To Skills | When |
|-------|-------------------|------|
| **domain-team** | `/domain-model`, `/sharia-calculator`, `/new-service` | Aggregate design, Sharia math, new service scaffold |
| **integration-team** | `/temporal-workflow`, `/kafka-event`, `/fineract-integration` | Workflow creation, event pipeline, core banking |
| **api-team** | `/api-endpoint` | REST endpoint creation |
| **qa-team** | `/write-tests` | Writing specific test suites |
| **devops-team** | `/docker-service`, `/flyway-migration` | Docker setup, DB migrations |
| **compliance-team** | `/compliance-check` | Quick code-level compliance scan |
| **incident-commander** | `/debug-service` | Deep single-service diagnosis |
| **feature-team** | All team agents (which delegate to skills) | Cross-functional feature coordination |
| **CTO** | All team agents (which delegate to skills) | Strategic cross-team orchestration |

### Validation Chain (MANDATORY for all features)

Every completed feature MUST pass all 3 gates before it is done:

```
┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
│  Gate 1: /review-arch│    │ Gate 2: code-reviewer │    │ Gate 3: /compliance- │
│  (ArchUnit rules)    │───▶│ (peer code quality)   │───▶│ check (SAMA/Sharia)  │
│                      │    │                       │    │                      │
│ ✓ Hexagonal layers   │    │ ✓ Naming conventions  │    │ ✓ SecurityConfig     │
│ ✓ Domain purity      │    │ ✓ @RequiredArgs (DI)  │    │ ✓ @SecuredEndpoint   │
│ ✓ Dependency flow    │    │ ✓ BigDecimal money    │    │ ✓ No PII in logs     │
│ ✓ Package structure  │    │ ✓ Java records (DTOs) │    │ ✓ Sharia rules       │
│ ✓ ArchUnit test      │    │ ✓ Zero hardcoding     │    │ ✓ Data protection    │
│ ✓ Flyway (no ddl)    │    │ ✓ ErrorCodes usage    │    │ ✓ Audit trail        │
└──────────────────────┘    └──────────────────────┘    └──────────────────────┘
        ALL PASS?                   ALL PASS?                   ALL PASS?
       │                           │                           │
       ▼ NO → Fix & re-run        ▼ NO → Fix & re-run        ▼ NO → Fix & re-run
       ▼ YES → Next gate          ▼ YES → Next gate          ▼ YES → DONE ✓
```

---

## ARCHITECTURE - HEXAGONAL (PORTS & ADAPTERS)

Every service MUST follow this exact layer structure. No exceptions.

```
services/{service-name}/src/main/java/com/ksa/financing/{service}/
├── domain/                          # PURE DOMAIN - Zero framework imports
│   ├── model/                       # Aggregates, Entities, Value Objects
│   │   ├── {Name}Aggregate.java     # Aggregate root (extends AggregateRoot)
│   │   ├── {Name}Entity.java        # Child entities inside aggregate boundary
│   │   ├── {Name}Id.java            # Strongly-typed ID value object
│   │   └── {Name}Status.java        # Status enum for state machine
│   ├── port/
│   │   ├── in/                      # INPUT PORTS - Use case interfaces
│   │   │   └── {Verb}{Name}UseCase.java
│   │   └── out/                     # OUTPUT PORTS - SPI contracts
│   │       ├── {Name}Repository.java
│   │       └── EventPublisher.java
│   └── service/                     # Domain services (cross-aggregate logic)
│       └── {Name}DomainService.java
│
├── application/                     # ORCHESTRATION LAYER
│   ├── usecase/                     # Use case implementations
│   │   └── {Verb}{Name}UseCaseImpl.java
│   ├── dto/                         # Request/Response DTOs
│   │   └── {Name}Dto.java
│   └── mapper/                      # MapStruct mappers
│       └── {Name}Mapper.java
│
├── infrastructure/                  # FRAMEWORK IMPLEMENTATIONS
│   ├── persistence/
│   │   ├── repository/
│   │   │   ├── Jpa{Name}Repository.java      # Spring Data JPA interface
│   │   │   └── {Name}RepositoryImpl.java      # Output port implementation
│   │   ├── entity/
│   │   │   └── {Name}Entity.java              # JPA entity (NOT domain entity)
│   │   └── mapper/
│   │       └── {Name}PersistenceMapper.java   # JPA <-> Domain mapper
│   ├── messaging/
│   │   └── Kafka{Name}Publisher.java          # Kafka event publisher
│   └── config/
│       └── SecurityConfig.java                # Spring Security + Keycloak
│
├── adapter/                         # EXTERNAL INTEGRATIONS
│   ├── rest/
│   │   ├── controller/
│   │   │   └── {Name}Controller.java          # REST endpoints
│   │   ├── request/
│   │   │   └── {Verb}{Name}Request.java       # HTTP request bodies
│   │   └── response/
│   │       └── {Name}Response.java            # HTTP response bodies
│   ├── temporal/
│   │   └── activity/
│   │       ├── {Name}Activity.java            # Temporal activity interface
│   │       └── {Name}ActivityImpl.java        # Temporal activity implementation
│   └── client/                                # External API clients
│
└── {ServiceName}Application.java              # Spring Boot main class
```

### CRITICAL ARCHITECTURE RULES

1. **Domain layer has ZERO framework imports** - No Spring, No JPA, No Kafka annotations
2. **Dependencies flow INWARD** - adapter -> application -> domain (NEVER reverse)
3. **Domain model != JPA entity** - Always map between them using persistence mappers
4. **Aggregates enforce invariants** - All business rules live inside aggregate methods
5. **Factory methods for creation** - Use `static create()` on aggregates, not public constructors
6. **Domain events on state changes** - Call `registerEvent()` inside aggregate methods
7. **Use case = 1 transaction** - Each use case method is one `@Transactional` boundary
8. **Ports are interfaces in domain** - Implementations live in infrastructure/adapter layers

---

## COMPLETE TECH STACK REFERENCE

### Core Runtime
| Technology | Version | Location |
|---|---|---|
| Java | 21 LTS | `pom.xml` -> `<java.version>` |
| Spring Boot | 4.0.2 | Parent POM `<spring.boot.version>` |
| Spring Cloud | 2024.0.0 | Parent POM `<spring.cloud.version>` |
| Maven | 3.9+ | `mvnw` wrapper |

### Workflow & Messaging
| Technology | Version | SDK |
|---|---|---|
| Temporal.io | 1.32.1 | `shared-libraries/workflow-orchestration-sdk/` |
| Apache Kafka | 3.9.1 | `shared-libraries/messaging-event-sdk/` |
| Schema Registry | 7.8.0 | Avro schemas in messaging-event-sdk |
| Apache Avro | 1.12.0 | `shared-libraries/messaging-event-sdk/` |
| gRPC | 1.68.1 | `shared-libraries/messaging-event-sdk/` |

### Security & Identity
| Technology | Version | SDK |
|---|---|---|
| Keycloak | 26.5.2 | `shared-libraries/foundational-infra-sdk/security/` |
| Spring Security | 7.0.0 | `foundational-infra-sdk` auto-config |
| OAuth2 Resource Server | Latest | JWT validation in SecurityConfig |

### Data Layer
| Technology | Version | Usage |
|---|---|---|
| PostgreSQL | 18.1 | Database-per-service pattern |
| Flyway | 10.4.1 | `src/main/resources/db/migration/V{n}__*.sql` |
| JPA/Hibernate | Latest | infrastructure/persistence layer only |
| HikariCP | 5.1.0 | Connection pool (via Spring Boot) |
| Redis | 8.0.2 | Caching, idempotency store |

### Observability Stack (ELK 9.3.0)
| Technology | Version | Config |
|---|---|---|
| Logback + Logstash Encoder | 9.0 | `logback-spring.xml` per service |
| Elasticsearch | 9.3.0 | `infrastructure/kubernetes/elk/` |
| Logstash | 9.3.0 | `infrastructure/logstash/pipeline/` |
| Kibana | 9.3.0 | Port 5601 |
| Filebeat | 8.19 | Sidecar per service |
| OpenTelemetry | 1.44.1 | `foundational-infra-sdk/telemetry/` |
| Jaeger | Latest | Port 16686 |
| Prometheus | Latest | Port 9090 |
| Grafana | Latest | Port 3000 |
| Micrometer | 1.15.0 | Metrics registry |

### Resilience & Utilities
| Technology | Version | SDK |
|---|---|---|
| Resilience4j | 2.3.0 | `foundational-infra-sdk/resilience/` |
| Jackson | 3.0.0 | JSON serialization |
| Lombok | 1.18.36 | Compile-time code gen |
| MapStruct | 1.6.3 | Bean mapping (application/mapper) |
| Moneta (JSR 354) | 1.4.2 | `domain-core-sdk` Money API |

### Core Banking
| Technology | Version | SDK |
|---|---|---|
| Apache Fineract | 1.13.0 | `shared-libraries/lms-adapter-sdk/` |

### Testing
| Technology | Version | SDK |
|---|---|---|
| JUnit 5 | 5.11.4 | `test-harness-sdk` |
| Mockito | 5.15.2 | `test-harness-sdk` |
| TestContainers | 1.20.4 | `test-harness-sdk` |
| AssertJ | 3.27.3 | `test-harness-sdk` |
| ArchUnit | 1.2.1 | Architecture enforcement |
| Rest Assured | Latest | REST API testing |

### Build & Deploy
| Technology | Version | Usage |
|---|---|---|
| GraalVM | 21.0.9 | Native image compilation |
| Jib | 3.4.4 | Containerization |
| Docker Compose | 2.0+ | Local infrastructure (18 services) |

---

## SHARED LIBRARIES (SDKs) - COMPLETE USAGE GUIDE

### 1. `domain-core-sdk` - Pure Domain Logic (MOST IMPORTANT)
**Location**: `shared-libraries/domain-core-sdk/`
**Package**: `com.ksa.financing.domain` and `com.ksa.islamic.finance`
**Depends on**: Nothing (zero external dependencies)
**Used by**: ALL services

```java
// === AGGREGATE ROOT BASE CLASS ===
import com.ksa.financing.domain.base.AggregateRoot;
import com.ksa.financing.domain.base.DomainEvent;

public class LoanAggregate extends AggregateRoot<LoanId> {
    // All aggregates extend AggregateRoot
    // Use registerEvent() to emit domain events
    // Use getUncommittedEvents() to retrieve pending events
    // Use markEventsAsCommitted() after publishing
}

// === SHARIA MATH ENGINE ===
import com.ksa.financing.domain.sharia.MurabahaCalculator;
import com.ksa.financing.domain.sharia.MurabahaCalculation;
import com.ksa.financing.domain.sharia.IjaraCalculator;
import com.ksa.financing.domain.sharia.IjaraCalculation;
import com.ksa.financing.domain.sharia.AmortizationScheduleGenerator;
import com.ksa.financing.domain.sharia.AmortizationEntry;

// Murabaha (cost-plus-profit financing)
MurabahaCalculation calc = MurabahaCalculator.calculate(
    new BigDecimal("100000"),    // costPrice - asset cost in SAR
    new BigDecimal("0.05"),      // profitRate - annual profit rate (5%)
    60,                          // tenureMonths
    new BigDecimal("0.10")       // downPaymentPercent (10%)
);
calc.getSellingPrice();          // 100000 + profit
calc.getTotalProfit();           // total profit amount
calc.getMonthlyInstallment();    // EMI amount
calc.getDownPaymentAmount();     // 10000 SAR

// Ijara (Islamic leasing)
IjaraCalculation ijara = IjaraCalculator.calculate(
    assetValue, residualValue, leaseRate, tenureMonths
);
ijara.getMonthlyRental();
ijara.getTotalRentals();

// Amortization Schedule
List<AmortizationEntry> schedule = AmortizationScheduleGenerator.generate(
    principal, profitRate, tenureMonths
);
// Each entry: month, installment, principalPortion, profitPortion, balance

// === VALUE OBJECTS ===
import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.domain.model.UserId;
import com.ksa.financing.domain.valueobject.SarMoney;

TenantId tenant = new TenantId("uuid-string");
UserId user = new UserId("uuid-string");
SarMoney amount = SarMoney.of(new BigDecimal("50000")); // 50,000 SAR

// === DOMAIN EVENTS ===
import com.ksa.financing.domain.event.DomainEvent;
// Interface with: getEventId(), getOccurredOn(), getEventType()
// Implement as Java records:
public record LoanDisbursed(UUID eventId, LocalDateTime occurredOn,
    LoanId loanId, BigDecimal amount) implements DomainEvent { }

// === DOMAIN MODELS ===
import com.ksa.financing.domain.model.Customer;
import com.ksa.financing.domain.model.Loan;
```

**RULES**: NEVER add Spring/JPA annotations here. All calculations use BigDecimal with RoundingMode.HALF_UP.

### 2. `foundational-infra-sdk` - Security, Logging, Resilience
**Location**: `shared-libraries/foundational-infra-sdk/`
**Package**: `com.ksa.financing.infra`
**Depends on**: Spring Boot, Spring Security, Resilience4j, OpenTelemetry
**Used by**: ALL services (auto-configured)

```java
// === SECURITY - Auto-configured ===
import com.ksa.financing.infra.security.TenantContextFilter;
// Auto-extracts tenant_id from JWT claims
// Sets ThreadLocal: TenantContext.getCurrentTenant()

import com.ksa.financing.infra.security.KeycloakJwtConverter;
// Converts Keycloak JWT to Spring Security authorities
// Maps realm_access.roles to ROLE_ prefixed authorities

// === EXCEPTION HANDLING - Auto-configured ===
import com.ksa.financing.infra.exception.GlobalExceptionHandler;
// @RestControllerAdvice - handles all exceptions globally
// BusinessException -> 422 Unprocessable Entity
// ValidationException -> 400 Bad Request
// NotFoundException -> 404 Not Found
// AccessDeniedException -> 403 Forbidden
// All others -> 500 Internal Server Error
// Returns standardized ErrorResponse with correlationId

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
// Throw these from use cases for proper HTTP mapping

// === RESILIENCE - Configuration beans ===
import com.ksa.financing.infra.resilience.CircuitBreakerConfig;
// Default: failureRateThreshold=50%, waitDuration=60s, slidingWindowSize=10
import com.ksa.financing.infra.resilience.RetryConfig;
// Default: maxAttempts=3, waitDuration=500ms, exponentialBackoff
import com.ksa.financing.infra.resilience.BulkheadConfig;
// Default: maxConcurrentCalls=25, maxWaitDuration=0

// === TELEMETRY - Auto-configured ===
import com.ksa.financing.infra.telemetry.TracingConfig;
// Auto-propagates traceId, spanId across HTTP and Kafka
// Exports to Jaeger via OTLP protocol
// Adds traceId to MDC for structured logging
```

**HOW TO USE**: Just add dependency - Spring Boot auto-config does the rest:
```xml
<dependency>
    <groupId>com.ksa.financing</groupId>
    <artifactId>foundational-infra-sdk</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 3. `messaging-event-sdk` - Kafka, gRPC, Avro
**Location**: `shared-libraries/messaging-event-sdk/`
**Package**: `com.ksa.islamic.messaging`
**Depends on**: Spring Kafka, Avro, gRPC

```java
// === KAFKA PRODUCER ===
import com.ksa.islamic.messaging.kafka.KafkaEventTemplate;
import com.ksa.islamic.messaging.kafka.EventEnvelope;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaEventTemplate template;

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(event -> {
            EventEnvelope envelope = EventEnvelope.wrap(event);
            template.send(event.getEventType(), event.getAggregateId().toString(), envelope);
        });
    }
}

// === KAFKA CONSUMER ===
@Component
@Slf4j
public class LoanEventListener {
    @KafkaListener(topics = "financing.loan.disbursed", groupId = "${spring.application.name}")
    public void onLoanDisbursed(EventEnvelope envelope) {
        log.info("Received loan disbursed event: {}", envelope.getEventId());
        // Process event
    }
}

// === TOPIC NAMING CONVENTION ===
// Pattern: {domain}.{aggregate}.{event-past-tense}
// Examples:
//   financing.loan.created
//   financing.loan.disbursed
//   financing.customer.onboarded
//   financing.risk.assessed
//   financing.wallet.credited

// === AVRO SCHEMAS ===
// Location: src/main/avro/*.avsc
// Compiled to Java classes during build

// === gRPC ===
// Proto definitions: src/main/proto/*.proto
// Generated stubs for service-to-service sync calls
```

### 4. `workflow-orchestration-sdk` - Temporal.io
**Location**: `shared-libraries/workflow-orchestration-sdk/`
**Package**: `com.ksa.islamic.workflow`
**Depends on**: Temporal Java SDK 1.32.1

```java
// === WORKFLOW INTERFACE (in shared activity-api SDK) ===
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.QueryMethod;

@WorkflowInterface
public interface CustomerOnboardingWorkflow {
    @WorkflowMethod
    OnboardingResult execute(OnboardingRequest request);

    @SignalMethod
    void approveManualReview(String reviewerId);

    @QueryMethod
    OnboardingStatus getStatus();
}

// === WORKFLOW IMPLEMENTATION (in service) ===
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

public class CustomerOnboardingWorkflowImpl implements CustomerOnboardingWorkflow {
    private final ActivityOptions options = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(5))
        .setRetryOptions(RetryOptions.newBuilder()
            .setMaximumAttempts(3)
            .setBackoffCoefficient(2.0)
            .build())
        .build();

    private final KycActivity kycActivity = Workflow.newActivityStub(KycActivity.class, options);
    private final CustomerActivity customerActivity = Workflow.newActivityStub(CustomerActivity.class, options);
    private final WalletActivity walletActivity = Workflow.newActivityStub(WalletActivity.class, options);
    private final RiskActivity riskActivity = Workflow.newActivityStub(RiskActivity.class, options);

    @Override
    public OnboardingResult execute(OnboardingRequest request) {
        // Step 1: Risk pre-check
        RiskResult risk = riskActivity.assessRisk(request.getNationalId());
        if (risk.getScore() > 80) {
            throw new BusinessException("Risk too high: " + risk.getScore());
        }

        // Step 2: KYC verification
        KycResult kyc = kycActivity.verifyIdentity(request.getNationalId());

        // Step 3: Create customer profile
        CustomerResult customer = customerActivity.createProfile(kyc);

        // Step 4: Create wallet
        WalletResult wallet = walletActivity.createWallet(customer.getCif());

        return new OnboardingResult(customer, wallet);
    }
}

// === ACTIVITY INTERFACE (in shared-libraries/{service}-activity-api) ===
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface KycActivity {
    @ActivityMethod
    KycResult verifyIdentity(String nationalId);

    @ActivityMethod
    OtpResult sendOtp(String mobile);
}

// === ACTIVITY IMPLEMENTATION (in services/{service}/adapter/temporal/) ===
@Component
@RequiredArgsConstructor
public class KycActivityImpl implements KycActivity {
    private final KycService kycService;  // inject domain service

    @Override
    public KycResult verifyIdentity(String nationalId) {
        return kycService.verify(nationalId);
    }
}

// === SAGA COMPENSATION PATTERN ===
import com.ksa.islamic.workflow.saga.SagaOrchestrator;
import com.ksa.islamic.workflow.saga.SagaStep;
import com.ksa.islamic.workflow.saga.CompensatingAction;

SagaOrchestrator.begin()
    .step("create-customer", () -> customerActivity.create(req),
          (result) -> customerActivity.rollback(result.getCif()))
    .step("create-wallet", () -> walletActivity.create(cif),
          (result) -> walletActivity.close(result.getWalletId()))
    .step("create-loan", () -> loanActivity.originate(req),
          (result) -> loanActivity.cancel(result.getLoanId()))
    .execute();
```

**RULES**:
- Workflows MUST be deterministic: no Random, no System.currentTimeMillis(), no I/O
- Activities do the actual work (API calls, DB queries, external services)
- Activity interfaces go in `shared-libraries/{service}-activity-api/`
- Activity implementations go in `services/{service}/adapter/temporal/activity/`
- Use `Workflow.newActivityStub()` to get activity references in workflows
- Configure timeouts and retry policies on ActivityOptions

### 5. `lms-adapter-sdk` - Apache Fineract Core Banking
**Location**: `shared-libraries/lms-adapter-sdk/`
**Package**: `com.ksa.financing.lms`
**Depends on**: Spring WebClient, Fineract REST API

```java
// === PORT INTERFACE (domain layer uses this) ===
import com.ksa.financing.lms.port.IBankingPort;

public interface IBankingPort {
    LoanAccount createLoanAccount(CreateLoanRequest request);
    DisbursementResult disburse(String loanId, BigDecimal amount);
    RepaymentResult recordRepayment(String loanId, BigDecimal amount);
    LoanSchedule getLoanSchedule(String loanId);
    ClientProfile createClient(CreateClientRequest request);
    SavingsAccount createSavingsAccount(String clientId);
}

// === FINERACT ADAPTER (infrastructure implementation) ===
import com.ksa.financing.lms.adapter.FineractBankingAdapter;
// REST client connecting to Fineract at https://localhost:8443
// Handles: client management, loan products, loan accounts,
//          disbursements, repayments, savings accounts
// Multi-tenant via Fineract-Platform-TenantId header
```

### 6. `compliance-localization-sdk` - ZATCA, SAMA, Hijri
**Location**: `shared-libraries/compliance-localization-sdk/`
**Package**: `com.ksa.financing.compliance`

```java
// === ZATCA E-INVOICING ===
import com.ksa.financing.compliance.zatca.ZatcaInvoiceGenerator;
import com.ksa.financing.compliance.zatca.QrCodeGenerator;
ZatcaInvoice invoice = ZatcaInvoiceGenerator.generate(invoiceData);
String qrCode = QrCodeGenerator.generate(invoice); // ZATCA-compliant QR

// === SAMA REPORTING ===
import com.ksa.financing.compliance.sama.SamaReportGenerator;
SamaReport report = SamaReportGenerator.generateMonthly(yearMonth, tenantId);

// === HIJRI CALENDAR ===
import com.ksa.financing.compliance.hijri.HijriDateConverter;
HijriDate hijri = HijriDateConverter.toHijri(LocalDate.now());
LocalDate gregorian = HijriDateConverter.toGregorian(hijri);

// === KSA VAT ===
import com.ksa.financing.compliance.vat.VatCalculator;
BigDecimal vat = VatCalculator.calculate(amount, VatRate.STANDARD); // 15%
BigDecimal total = VatCalculator.addVat(amount, VatRate.STANDARD);
```

### 7. `reporting-projection-sdk` - CQRS Read Models
**Location**: `shared-libraries/reporting-projection-sdk/`

```java
import com.ksa.financing.reporting.projection.EventProjector;
import com.ksa.financing.reporting.projection.ReadModel;

public class LoanDashboardProjector extends EventProjector {
    @EventHandler
    public void on(LoanDisbursedEvent event) {
        // Update denormalized read-side table for fast dashboard queries
    }
}
```

### 8. `test-harness-sdk` - Testing Utilities
**Location**: `shared-libraries/test-harness-sdk/`

```java
// === TEMPORAL TEST ENVIRONMENT ===
import com.ksa.financing.test.temporal.TemporalTestEnvironment;
@RegisterExtension
static TemporalTestEnvironment temporal = new TemporalTestEnvironment();

// === PRE-CONFIGURED TESTCONTAINERS ===
import com.ksa.financing.test.containers.PostgresContainer;
import com.ksa.financing.test.containers.KafkaContainer;
import com.ksa.financing.test.containers.RedisContainer;
@Container
static PostgresContainer postgres = PostgresContainer.create("service_db");

// === MOCK ADAPTERS ===
import com.ksa.financing.test.mock.MockFineractAdapter;
import com.ksa.financing.test.mock.MockKycAdapter;
import com.ksa.financing.test.mock.MockNafathAdapter;
```

### 9-14. Shared Activity API SDKs

| SDK | Package | Key Interfaces |
|---|---|---|
| `kyc-activity-api` | `com.ksa.financing.kyc.activity` | `KycActivity`: verifyIdentity, sendOtp, querySimah, checkNafath |
| `customer-activity-api` | `com.ksa.financing.customer.activity` | `CustomerActivity`: createProfile, updateCif, getCustomer |
| `wallet-activity-api` | `com.ksa.financing.wallet.activity` | `WalletActivity`: createWallet, creditWallet, debitWallet, getBalance |
| `identity-activity-api` | `com.ksa.financing.identity.activity` | `IdentityActivity`: authenticate, federateIdentity, revokeToken |
| `notification-activity-api` | `com.ksa.financing.notification.activity` | `NotificationActivity`: sendSms, sendEmail, sendPush |
| `risk-activity-api` | `com.ksa.financing.risk.activity` | `RiskActivity`: assessRisk, checkFraud, screenAml, screenSanctions |

---

## ACTIVE MICROSERVICES (9 Running)

| # | Service | Port | Database | Responsibility |
|---|---|---|---|---|
| 1 | `risk-service` | 8090 | risk_service | Fraud, AML, sanctions, velocity checks, risk scoring |
| 2 | `kyc-adapter-service` | 8087 | kyc_adapter_db | Nafath, Yakeen, Simah government KYC integration |
| 3 | `customer-service` | TBD | customer_db | Customer profile, CIF lifecycle management |
| 4 | `wallet-service` | TBD | wallet_db | Closed-loop Islamic compliant wallet |
| 5 | `identity-service` | TBD | identity_db | Identity federation, Keycloak OAuth |
| 6 | `global-profile-service` | TBD | global_profile_db | Global customer profile index |
| 7 | `pii-vault-service` | TBD | pii_vault_db | PII encryption and vault management |
| 8 | `onboarding-workflow-service` | TBD | N/A | Temporal-based customer onboarding orchestrator |
| 9 | `service-template` | 8081 | service_template | Reference template for creating new services |

## PLANNED MICROSERVICES (14 More - commented in pom.xml)

| Service | Domain | Priority |
|---|---|---|
| `lending-service` | Loan origination, disbursement, repayment | HIGH |
| `product-service` | Islamic product catalog (Murabaha, Ijara, Tawarruq) | HIGH |
| `credit-decisioning` | Credit scoring, decision engine | HIGH |
| `ledger-service` | Double-entry accounting ledger | HIGH |
| `notification-service` | SMS, email, push notifications | MEDIUM |
| `collections-service` | Payment collections, delinquency management | MEDIUM |
| `document-service` | Document generation, storage (contracts, statements) | MEDIUM |
| `audit-service` | 7-year SAMA audit trail | MEDIUM |
| `core-banking-adapter` | Fineract bridge service | MEDIUM |
| `partner-service` | Partner/merchant management | LOW |
| `payment-adapter` | Payment gateway integration (SADAD, mada) | LOW |
| `sharia-compliance` | Sharia board rules engine | LOW |
| `sme-service` | SME financing workflows | LOW |
| `restructuring-service` | Loan restructuring and rescheduling | LOW |

---

## DATABASE CONVENTIONS

### Rules
- **Database-per-service**: Each service owns its database exclusively
- **Flyway migrations**: `src/main/resources/db/migration/V{n}__{description}.sql`
- **Naming**: snake_case for tables/columns
- **IDs**: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- **Multi-tenant**: Every table has `tenant_id UUID NOT NULL`
- **Audit columns**: `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`, `version INT`
- **Soft delete**: Use `status` column, not physical delete
- **JPA ddl-auto**: Always `validate` (Flyway handles schema)

### Migration Template
```sql
-- V1__initial_schema.sql
CREATE TYPE {name}_status AS ENUM ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED');

CREATE TABLE {table_name} (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    -- business columns --
    status          {name}_status NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    version         INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_{table}_tenant ON {table_name}(tenant_id);
CREATE INDEX idx_{table}_status ON {table_name}(tenant_id, status);
```

---

## SECURITY PATTERN

### JWT Authentication Flow
```
Client -> Kong API Gateway -> Service SecurityFilterChain
                                  -> Validate JWT against Keycloak JWKS
                                  -> CasbinAuthorizationFilter checks @SecuredEndpoint
                                  -> Reads policies from Redis (per role)
                                  -> Matches obj+act against user's role policies
                                  -> ALLOW → Controller gets @AuthenticationPrincipal Jwt
                                  -> DENY → 403 Forbidden response
```

### Authorization: @SecuredEndpoint (Casbin + Redis)

Every authenticated API endpoint MUST have `@SecuredEndpoint(obj, act)` annotation.
The SDK `CasbinAuthorizationFilter` (auto-configured via `ksa.authorization.enabled=true`)
reads the annotation at runtime and checks the user's role policies from Redis.

```java
// On each controller method:
@SecuredEndpoint(obj = "customers", act = "create")
@PostMapping
public ResponseEntity<CustomerResponse> createCustomer(...) { ... }

@SecuredEndpoint(obj = "customers", act = "read")
@GetMapping("/{id}")
public ResponseEntity<CustomerResponse> getById(...) { ... }

@SecuredEndpoint(obj = "customers.bank-accounts", act = "create")
@PostMapping("/{id}/bank-accounts")
public ResponseEntity<BankAccountResponse> addBankAccount(...) { ... }
```

**Object naming**: `{resource}` or `{resource}.{sub-resource}` (e.g., `customers.bank-accounts`, `kyc.nafath`)
**Action naming**: `create`, `read`, `update`, `delete`, `manage`, `verify`, `check`, `status`, `initiate`, `fetch`

Policies are stored in `casbin_rule` table (identity-service DB) and synced to Redis on IDS startup.
Other services only read from Redis — they never write policies.

### SecurityConfig Template
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/prometheus",
                    "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }
}
```

### application.yml Authorization Config (REQUIRED for every service)
```yaml
ksa:
  authorization:
    enabled: true
    cache-key-prefix: casbin:policies
```

### Keycloak
- **Realm**: `islamic-financing` or `CompanyRealm`
- **Issuer**: `http://localhost:8080/realms/{realm}`
- **JWKS**: `http://localhost:8080/realms/{realm}/protocol/openid-connect/certs`
- **JWT Claims**: `sub` (userId), `tenant_id`, `realm_access.roles`

---

## REST API CONVENTIONS

```
POST   /api/v1/{resource}                  # Create
GET    /api/v1/{resource}/{id}             # Read
PUT    /api/v1/{resource}/{id}             # Update
DELETE /api/v1/{resource}/{id}             # Delete
GET    /api/v1/{resource}                  # List (with pagination)
POST   /api/v1/{resource}/{id}/{action}    # Action (activate, complete, etc.)
```

### Required Headers
| Header | Source | Purpose |
|---|---|---|
| `Authorization: Bearer {jwt}` | Client | Authentication |
| `X-Tenant-Id` | API Gateway (Kong) | Tenant identification for public/pre-auth endpoints |
| `X-Correlation-ID` | Auto/Client | Request tracing |
| `Content-Type: application/json` | Client | Content negotiation |
| `Accept-Language` | Client | Locale for error messages (`en`, `en-US`, `ar`, `ar-SA`). Default: `en` |

---

## ERROR HANDLING & LOCALIZATION

### Architecture
All error handling lives in `foundational-infra-sdk` (auto-configured for every service):
- **`ErrorCodes.java`** — Centralized error code constants
- **`GlobalExceptionHandler.java`** — `@RestControllerAdvice` mapping exceptions → HTTP responses
- **`errors/*.properties`** — Localized messages (English + Arabic)
- **`ErrorMessageProperties.java`** — Config for service-level message extensions

### Error Code Convention
Error codes follow **dot-separated naming**: `{DOMAIN}.{ENTITY}.{PROBLEM}`

```
COMMON.VALIDATION.FAILED         → 400 Bad Request
COMMON.AUTH.ACCESS_DENIED        → 403 Forbidden
COMMON.AUTH.INVALID_CREDENTIALS  → 401 Unauthorized
COMMON.RESOURCE.NOT_FOUND        → 404 Not Found
COMMON.RESOURCE.CONFLICT         → 409 Conflict
COMMON.REQUEST.BAD_REQUEST       → 400 Bad Request
COMMON.SYSTEM.INTERNAL_ERROR     → 500 Internal Server Error
COMMON.SYSTEM.TECHNICAL_ERROR    → 500 Internal Server Error
```

Domain-specific codes use nested classes in `ErrorCodes`:
```java
ErrorCodes.Customer.DUPLICATE_CODE   // "CUSTOMER.REFERENCE_DATA.DUPLICATE_CODE"
ErrorCodes.Customer.NOT_FOUND        // "CUSTOMER.PROFILE.NOT_FOUND"
ErrorCodes.Kyc.VERIFICATION_FAILED   // "KYC.VERIFICATION.FAILED"
ErrorCodes.Wallet.INSUFFICIENT_FUNDS // "WALLET.BALANCE.INSUFFICIENT"
ErrorCodes.Risk.SCORE_TOO_HIGH       // "RISK.SCORE.TOO_HIGH"
ErrorCodes.Loan.LIMIT_EXCEEDED       // "LOAN.AMOUNT.LIMIT_EXCEEDED"
ErrorCodes.Identity.TOKEN_EXPIRED    // "IDENTITY.TOKEN.EXPIRED"
```

### Exception Types — MANDATORY Usage
| Exception Class | HTTP Status | When To Use |
|---|---|---|
| `BusinessException(errorCode, message, args...)` | 422 | Business rule violations (duplicate, invalid state) |
| `NotFoundException.forEntity(type, id)` | 404 | Resource not found |
| `TechnicalException(errorCode, message, cause)` | 500 | Infrastructure/system failures |
| `IllegalArgumentException` (domain layer only) | 400 | Domain invariant violations (no framework imports) |
| `IllegalStateException` (domain layer only) | 409 | Invalid state transitions (no framework imports) |

### How To Throw Exceptions in Services
```java
// Application/Adapter layer — use SDK exceptions with ErrorCodes:
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;

// Duplicate / business rule violation → 422
throw new BusinessException(
    ErrorCodes.Customer.DUPLICATE_CODE,
    "Customer with this NID already exists: " + nid,
    nid);  // args for localized message {0}

// Not found → 404
throw NotFoundException.forEntity("Customer", customerId.toString());

// Domain layer — use plain JDK exceptions (no framework imports):
throw new IllegalArgumentException("Amount must be positive");
throw new IllegalStateException("Cannot cancel a disbursed loan");
```

### Localized Error Response
The `GlobalExceptionHandler` reads `Accept-Language` header and resolves messages from `errors/*.properties`:

```
Accept-Language: en → English message
Accept-Language: ar → Arabic message
No header          → English (default)
```

**Error Response Structure**:
```json
{
  "timestamp": "2026-02-24T06:57:08Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "CUSTOMER.REFERENCE_DATA.DUPLICATE_CODE",
  "message": "خيار البيانات المرجعية بالرمز موجود بالفعل: SALARY",
  "path": "/api/v1/reference-data/source-of-wealth",
  "traceId": "a1b2c3d4"
}
```

### Adding New Error Codes
1. Add constant to `ErrorCodes.java` (or nested class) in `foundational-infra-sdk`
2. Add English message to `errors/errors.properties`
3. Add Arabic message to `errors/errors_ar.properties`
4. Error code string = message property key (e.g., `CUSTOMER.PROFILE.NOT_FOUND`)

### Service-Level Message Extensions
Services can add their own message files via `application.yml`:
```yaml
ksa:
  error:
    additional-basenames:
      - errors/customer-errors
```
Then provide `src/main/resources/errors/customer-errors.properties` + `customer-errors_ar.properties`.

---

## CODING CONVENTIONS

### Java Style
- `var` for local variables when type is obvious
- Java `record` for DTOs, commands, queries, domain events
- `@Slf4j` (Lombok) for logging
- `@RequiredArgsConstructor` for constructor injection (NEVER `@Autowired`)
- `BigDecimal` for all money/rate calculations, `RoundingMode.HALF_UP`
- `Optional` for return types, never for parameters

### Naming Conventions
| Type | Pattern | Example |
|---|---|---|
| Aggregate | `{Name}Aggregate` | `LoanAggregate` |
| Domain Entity | `{Name}Entity` | `RepaymentEntity` |
| JPA Entity | `{Name}Entity` (in infrastructure.persistence.entity) | `LoanJpaEntity` |
| Value Object | descriptive | `SarMoney`, `TenantId`, `LoanId` |
| Use Case | `{Verb}{Name}UseCase` | `ManageLoanUseCase` |
| Repository Port | `{Name}Repository` | `LoanRepository` |
| Controller | `{Name}Controller` | `LoanController` |
| Temporal Activity | `{Name}Activity` | `DisbursementActivity` |
| Temporal Workflow | `{Name}Workflow` | `LoanOriginationWorkflow` |
| Domain Event | `{Aggregate}{PastTense}` | `LoanDisbursed` |
| DTO | `{Name}Dto` | `LoanDto` |
| Request | `{Verb}{Name}Request` | `CreateLoanRequest` |
| Response | `{Name}Response` | `LoanResponse` |
| Kafka Topic | `financing.{aggregate}.{event}` | `financing.loan.disbursed` |
| DB Migration | `V{n}__{description}.sql` | `V1__initial_schema.sql` |

---

## IMPORTANT DOMAIN CONCEPTS

### Islamic Financing Products
| Product | Description | Calculator |
|---|---|---|
| **Murabaha** | Cost-plus-profit sale. Bank buys asset, sells to customer at agreed markup | `MurabahaCalculator` |
| **Ijara** | Islamic leasing. Bank owns asset, leases to customer with optional ownership transfer | `IjaraCalculator` |
| **Tawarruq** | Commodity Murabaha. Customer buys commodity on credit, sells for cash | Planned |
| **Musharakah** | Diminishing partnership. Joint ownership with gradual transfer | Planned |

### KYC/AML Integration Points
| System | Purpose | Service |
|---|---|---|
| **Nafath** | National digital identity verification (Saudi) | `kyc-adapter-service` |
| **Yakeen** | Government identity data lookup (NIC) | `kyc-adapter-service` |
| **Simah** | Saudi credit bureau check | `kyc-adapter-service` |
| **SAMA Sanctions** | Sanctions list screening | `risk-service` |

### Risk Scoring Model (risk-service)
```
Weights:
  - Watchlist match:       40%
  - Velocity anomaly:      25%
  - New customer profile:  15%
  - Multi-NID per device:  30%

Velocity Limits:
  - IP: max 10 per hour
  - Device: max 5 per day
  - NID: max 3 per day
  - Mobile: max 3 per day

Risk Levels: LOW (0-30) | MEDIUM (31-60) | HIGH (61-80) | CRITICAL (81-100)
```

---

## BUILD & RUN COMMANDS

```bash
# === BUILD ===
mvn clean install -DskipTests            # Build all SDKs + services
mvn clean package -DskipTests -pl services/risk-service  # Single service
mvn clean verify                          # Full build with tests
mvn -Pnative native:compile              # GraalVM native image
mvn clean package jib:dockerBuild         # Docker image via Jib

# === INFRASTRUCTURE ===
docker compose up -d                      # Start all 18 Docker services
docker compose down                       # Stop all
./scripts/start-infra.sh                  # Start script
./scripts/stop-infra.sh                   # Stop script
./scripts/logs.sh risk-service            # View service logs
./scripts/validate-setup.sh              # Validate environment

# === RUN SERVICE LOCALLY ===
cd services/risk-service && mvn spring-boot:run
```

---

## LOCAL DEVELOPMENT URLs

| Service | URL | Credentials |
|---|---|---|
| Keycloak | http://localhost:8080 | admin/admin |
| Grafana | http://localhost:3000 | admin/admin |
| Kibana | http://localhost:5601 | N/A |
| Temporal UI | http://localhost:8233 | N/A |
| Jaeger | http://localhost:16686 | N/A |
| Prometheus | http://localhost:9090 | N/A |
| Fineract | https://localhost:8443 | N/A |
| Risk Service | http://localhost:8090 | JWT required |
| KYC Service | http://localhost:8087 | JWT required |
| Schema Registry | http://localhost:8082 | N/A |
| Portainer | https://localhost:9443 | N/A |
| Kong Gateway | http://localhost:8000 | N/A |

---

## CREATING A NEW SERVICE - STEP BY STEP

1. Copy `services/service-template/` to `services/{new-service}/`
2. Rename all `template`/`Template` references in code and pom.xml
3. Add `<module>{new-service}</module>` to `services/pom.xml`
4. Uncomment/add module in root `pom.xml`
5. Add database to `infrastructure/postgres/init.sql`: `CREATE DATABASE {service}_db;`
6. Add service block to `docker-compose.yml`
7. Create `V1__initial_schema.sql` Flyway migration
8. Implement domain model (aggregate with factory method + invariants)
9. Define ports (in/out interfaces in domain layer)
10. Implement application layer (use cases, DTOs, MapStruct mappers)
11. Implement infrastructure (JPA entities, repos, Kafka publisher, SecurityConfig)
12. Implement adapters (REST controllers, Temporal activities)
13. Write tests: unit (domain), integration (JPA+Testcontainers), architecture (ArchUnit)
14. Add `logback-spring.xml` for structured JSON logging

---

## TESTING REQUIREMENTS

```
src/test/java/
├── unit/domain/           # Pure domain logic (no Spring context)
├── unit/application/      # Use cases with mocked ports
├── integration/           # @SpringBootTest + Testcontainers
│   ├── persistence/       # @DataJpaTest
│   ├── rest/              # @WebMvcTest
│   └── temporal/          # Temporal test environment
└── architecture/          # ArchUnit hexagonal enforcement (MANDATORY)
```

### Architecture Test (REQUIRED for every service)
```java
@AnalyzeClasses(packages = "com.ksa.financing.{service}")
class ArchitectureTest {
    @ArchTest
    static final ArchRule domain_not_depend_on_infra =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_not_use_spring =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");
}
```

---

## WHAT NOT TO DO

### Architecture
1. DO NOT put business logic in controllers
2. DO NOT use JPA entities in domain layer
3. DO NOT share databases between services
4. DO NOT add Spring annotations in domain layer
5. DO NOT skip Flyway - never use `ddl-auto: create/update`
6. DO NOT use `@Autowired` field injection
7. DO NOT call another service's database directly
8. DO NOT skip architecture tests
9. DO NOT use mutable DTOs - use Java records

### Error Handling
10. DO NOT throw raw `IllegalArgumentException` from application/adapter layers — use `BusinessException` or `NotFoundException` with `ErrorCodes`
11. DO NOT hardcode error messages — use `ErrorCodes` constants and localized `errors/*.properties`
12. DO NOT define custom exception classes per service — use the SDK exceptions

### ZERO HARDCODING (CRITICAL)
13. DO NOT hardcode URLs, ports, or hostnames — use `${ENV_VAR:default}` in application.yml
14. DO NOT hardcode database credentials — use `${DB_USERNAME}`, `${DB_PASSWORD}`
15. DO NOT hardcode Keycloak realm names — use `${KEYCLOAK_REALM}`
16. DO NOT hardcode Kafka bootstrap servers — use `${KAFKA_BOOTSTRAP_SERVERS}`
17. DO NOT hardcode Temporal address — use `${TEMPORAL_ADDRESS}`
18. DO NOT hardcode Fineract URL — use `${FINERACT_BASE_URL}`
19. DO NOT hardcode service ports — use `${SERVER_PORT}`
20. DO NOT hardcode Kafka topic names in @KafkaListener — use `${kafka.topics.xxx}`
21. DO NOT store secrets in .env files for production — use Vault/K8s Secrets
See `docs/standards/ENVIRONMENT_CONFIG.md` for complete env var registry.

### Security
22. DO NOT log PII/credentials
23. DO NOT use `double` or `float` for money — use `BigDecimal` with `RoundingMode.HALF_UP`
24. DO NOT skip @SecuredEndpoint on API endpoints — every authenticated endpoint MUST have `@SecuredEndpoint(obj = "...", act = "...")` annotation
25. DO NOT use cookie-based sessions — use stateless JWT

### Naming Conventions (CRITICAL - enforce on every review)
26. DO NOT name JPA entities without `JpaEntity` suffix — `CustomerEntity.java` in persistence layer MUST be `CustomerJpaEntity.java`
27. DO NOT add `Event` suffix to domain events — use `{Aggregate}{PastTense}` (e.g., `LoanDisbursed`, NOT `LoanDisbursedEvent`)
28. DO NOT use singular table names — always plural snake_case (e.g., `customers`, NOT `customer`)
29. DO NOT name aggregates without `Aggregate` suffix in domain-core-sdk

### Tenant Isolation (CRITICAL - SAMA multi-tenant compliance)
30. DO NOT use DEFAULT_TENANT_ID fallback — if `tenant_id` claim is missing from JWT, ALWAYS throw `BusinessException(ErrorCodes.INVALID_CREDENTIALS, "No tenant_id claim found in JWT token")`
31. DO NOT skip tenant_id extraction in ANY controller that has `@SecuredEndpoint` — every authenticated endpoint MUST extract and use tenant_id
32. DO NOT create endpoints that return data across tenants — ALL repository queries MUST filter by `tenant_id`
33. DO NOT hardcode tenant values like `"system"` or `UUID("00000000-...")` — always extract from JWT or request context
34. DO NOT hardcode UUIDs in SQL INSERT/UPDATE statements — tenant_id and customer_id MUST come from domain model parameters, never be zero-filled placeholders
35. DO NOT skip tenant propagation on public/pre-auth endpoints — use `X-Tenant-Id` header (set by API gateway) or request body field for endpoints that have no JWT (e.g., onboarding initiate, auth register)

### Resilience (CRITICAL - per RESILIENCE_STANDARDS.md)
36. DO NOT make external HTTP calls without `@CircuitBreaker` + `@Retry` — ALL inter-service REST calls, Keycloak calls, and government API calls MUST have resilience annotations
37. DO NOT skip fallback methods on non-critical circuit breakers — use `fallbackMethod` parameter for graceful degradation
38. DO NOT use default Resilience4j config for government APIs — use named instances: `internal-rest` (30s wait), `keycloak` (30s wait), `government-api` (120s wait, 40% threshold)

### Idempotency (CRITICAL - financial operations)
39. DO NOT create financial operations without idempotency — ALL disbursements, repayments, wallet top-ups, and customer creation MUST check `idempotency_key` before executing
40. DO NOT skip `idempotency_key` column in financial tables — every financial table MUST have `idempotency_key VARCHAR(100)` with `UNIQUE(tenant_id, idempotency_key)` constraint
41. DO NOT process duplicate requests — check `repository.findByIdempotencyKey(tenantId, key)` before creating new records

---

## MANDATORY AUDIT CHECKLIST (Run before any release/review)

> **RULE**: Every service MUST pass ALL checks below. Agents MUST verify these during `/review-arch`, `code-reviewer`, and `/compliance-check`.

### 1. Security Audit
```
□ SecurityConfig has @EnableMethodSecurity
□ application.yml has ksa.authorization.enabled: true
□ SecurityConfig has oauth2ResourceServer with JWT decoder
□ SecurityConfig permits only: /actuator/health/**, /v3/api-docs/**, /swagger-ui/**
□ ALL /api/** endpoints require authentication
□ ALL controller methods have @SecuredEndpoint(obj, act) annotation
□ No endpoint returns data without tenant_id filtering
```

### 2. Zero Hardcoding Audit
```
□ application.yml uses ${ENV_VAR:default} for ALL config values
□ Keycloak realm = ${KEYCLOAK_REALM:islamic-financing}
□ Server port = ${SERVER_PORT:XXXX} (per ENVIRONMENT_CONFIG.md port registry)
□ DB connection uses ${DB_HOST}, ${DB_PORT}, ${DB_NAME}, ${DB_USERNAME}, ${DB_PASSWORD}
□ Kafka uses ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
□ Log levels use ${LOG_LEVEL:INFO} and ${APP_LOG_LEVEL:DEBUG}
□ No hardcoded IPs, URLs, or credentials in Java code or YAML
□ No hardcoded UUIDs (00000000-...) in SQL or Java — use parameters from context
□ No hardcoded tenant values ("system", UUID.randomUUID()) — use JWT/header/request
```

### 3. Naming Convention Audit
```
□ JPA entities in infrastructure/persistence/entity/ have JpaEntity suffix
□ Domain events follow {Aggregate}{PastTense} pattern (no Event suffix)
□ Domain aggregates have Aggregate suffix
□ Table names are plural snake_case
□ Kafka topics follow financing.{aggregate}.{event} pattern
□ Use cases follow {Verb}{Name}UseCase pattern
□ Controllers follow {Name}Controller pattern
```

### 4. Architecture Audit
```
□ Domain layer has ZERO Spring/JPA/Kafka imports
□ Dependencies flow inward: adapter → application → domain
□ Application layer does NOT import adapter layer classes
□ Controllers do NOT directly access repositories (use cases only)
□ ArchUnit test exists with 8 mandatory rules
□ Domain model != JPA entity (separate classes with persistence mapper)
```

### 5. Tenant Isolation Audit
```
□ extractTenantId() throws BusinessException if tenant_id missing (NO default fallback)
□ ALL authenticated endpoints extract and pass tenant_id to use cases
□ ALL repository queries filter by tenant_id
□ ALL database tables have tenant_id column with index
□ No cross-tenant data leakage possible
□ Public/pre-auth endpoints accept tenant from X-Tenant-Id header or request body
□ No hardcoded UUIDs in SQL INSERT/UPDATE — tenant_id flows from domain parameters
□ No DEFAULT_TENANT_ID constants, no UUID.randomUUID() for tenant, no "system" strings
```

### 6. Resilience Audit
```
□ ALL external HTTP calls have @CircuitBreaker + @Retry annotations
□ Named instances configured: internal-rest, keycloak, government-api
□ Fallback methods exist for non-critical operations
□ RestTemplate/WebClient has connection and read timeouts configured
□ Kafka consumers have retry + DLQ configured
```

### 7. Idempotency Audit
```
□ Financial tables have idempotency_key column with UNIQUE constraint
□ Use cases check findByIdempotencyKey() before creating records
□ Controllers accept idempotencyKey in request body or X-Idempotency-Key header
□ Duplicate requests return cached result, not error
```

### 8. Test Audit
```
□ ArchUnit test exists with 8 rules (domain→infra, domain→adapter, etc.)
□ Domain model tests exist (>= 90% coverage target)
□ Use case tests exist with mocked ports
□ No ddl-auto: create/update in test configs (use Flyway + Testcontainers)
```

---

## ENVIRONMENT VARIABLES

> **Complete registry**: See `docs/standards/ENVIRONMENT_CONFIG.md` for ALL env vars, port registry, and the complete application.yml template with zero hardcoding.

```bash
# Core (see ENVIRONMENT_CONFIG.md for complete list)
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SCHEMA_REGISTRY_URL=http://localhost:8082
TEMPORAL_ADDRESS=localhost:7233
TEMPORAL_NAMESPACE=default
KEYCLOAK_BASE_URL=http://localhost:8080
KEYCLOAK_REALM=islamic-financing
FINERACT_BASE_URL=https://localhost:8443/fineract-provider/api/v1
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
ELASTICSEARCH_URL=http://localhost:9200
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=DEBUG
```

---

## DOCUMENTATION INDEX

| Document | Path |
|---|---|
| Master Blueprint (17+ docs) | `docs/islamic-financing/master-blueprint/` |
| Quick Start Guide | `docs/getting-started/quick-start.md` |
| Fineract API Reference | `docs/fineract/FINERACT-API-REFERENCE.md` |
| Fineract Postgres Migration | `docs/fineract/FINERACT-POSTGRES-MIGRATION.md` |
| ERD Diagrams | `docs/islamic-financing/erd-docs/` |
| ELK Deployment | `docs/infrastructure/kubernetes/elk-implementation-report.md` |
| Local Dev Setup | `docs/infrastructure/local-development/` |
| API Collections | `docs/postman/` |
| K8s Manifests | `infrastructure/kubernetes/` |
| Audit Report | `docs/islamic-financing/master-blueprint/11_CRITICAL_AUDIT_REPORT.md` |
| SME Workflows | `docs/islamic-financing/master-blueprint/13_SME_WORKFLOWS.md` |
| Partner Management | `docs/islamic-financing/master-blueprint/14_PARTNER_MANAGEMENT.md` |
| Loan Servicing | `docs/islamic-financing/master-blueprint/17_LOAN_SERVICING_RESTRUCTURING.md` |
| **Standards: Environment Config** | **`docs/standards/ENVIRONMENT_CONFIG.md`** |
| **Standards: Blueprint Index** | **`docs/standards/BLUEPRINT_INDEX.md`** |
| **Standards: Naming Conventions** | **`docs/standards/NAMING_CONVENTIONS.md`** |
| **Standards: Testing Standards** | **`docs/standards/TESTING_STANDARDS.md`** |
| **Standards: Security Architecture** | **`docs/standards/SECURITY_ARCHITECTURE.md`** |
| **Standards: Resilience Standards** | **`docs/standards/RESILIENCE_STANDARDS.md`** |
| **Agents (12 team leads)** | **`.claude/agents/`** (autonomous workers) |
| **Skills (14 specialists)** | **`.claude/commands/`** (user-invocable workflows) |
| Product Requirements | `docs/islamic-financing/ProdDocs/` (10 BRS docs) |
| User Journeys | `docs/islamic-financing/user journey/` (14 personas) |
| Global CRM Architecture | `docs/islamic-financing/master-blueprint/19_GLOBAL_CRM_ARCHITECTURE.md` |
| SDK Ecosystem | `docs/islamic-financing/master-blueprint/18_SDK_ECOSYSTEM.md` |
| Database Strategy | `docs/islamic-financing/master-blueprint/15_DATABASE_STRATEGY.md` |
| Sharia Compliance Engine | `docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md` |
| Open Banking | `docs/islamic-financing/master-blueprint/16_OPEN_BANKING_INTEGRATION.md` |
| Development Plan | `docs/islamic-financing/dev-plan/` |

---

## DECISION FRAMEWORK (ALL AGENTS MUST FOLLOW)

### Priority Order
```
1. SAMA Compliance        > Everything else (regulatory mandate)
2. Sharia Compliance      > Feature delivery (Islamic finance requirement)
3. Security & Data Privacy > Performance (PDPL, NCA requirements)
4. Data Integrity         > Speed (financial data must be accurate)
5. Architecture Purity    > Framework convenience (hexagonal enforcement)
6. Test Coverage          > Feature velocity (quality gates are non-negotiable)
7. Domain Correctness     > Code brevity (BigDecimal, immutability)
```

### Technology Decision Matrix
| Decision | Always Use | Never Use |
|----------|-----------|-----------|
| Money/Rates | `BigDecimal` + `RoundingMode.HALF_UP` | `double`, `float`, `Double` |
| DTOs/Events | Java `record` | Mutable POJOs |
| DI | `@RequiredArgsConstructor` | `@Autowired` field injection |
| Logging | `@Slf4j` (Lombok) | `System.out.println` |
| DB Schema | Flyway migrations | `ddl-auto: create/update` |
| Session | Stateless JWT | Cookie-based sessions |
| Complex Flows | Temporal.io orchestration | Event choreography |
| Async Notifications | Kafka events | Sync REST calls |
| High-throughput Sync | gRPC | REST for internal |
| Error Handling | `ErrorCodes` + SDK exceptions | Raw string messages |
| Domain Layer | Pure Java (zero imports) | Spring/JPA annotations |

### Agent Workflow Guidelines
1. **Always read CLAUDE.md first** before any implementation
2. **Use `cto` agent for strategic decisions** that affect multiple services
3. **Use team agents for domain-specific work** within a team's scope
4. **Use `/skill` commands for atomic tasks** like creating a single endpoint
5. **Chain for complex features**: CTO agent → feature-team agent → specialist `/skills`
6. **Always run `/review-arch` after implementation** to verify compliance
7. **Always run `/compliance-check` for financial features** before marking done
8. **Use `code-reviewer` agent before committing** any significant changes
9. **File system note**: Root directory owned by root. Use `sudo` (password: 1236) for creating new files/directories outside existing service directories

### Cross-Service Communication Rules
```
Service A needs data from Service B?
  → Temporal Activity (for workflow context)
  → REST API call (for simple sync query)
  → Kafka Event (for async notification ONLY)
  → NEVER direct database access

Financial operation across services?
  → ALWAYS use Temporal SAGA with compensation
  → ALWAYS use idempotency keys
  → ALWAYS log to audit trail
```
