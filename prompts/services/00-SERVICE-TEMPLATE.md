# 🏗️ Prompt 00: Service Template Implementation

**Objective**: Create a reusable service template following Hexagonal Architecture that all 20 microservices will copy.

**Prerequisites**:
- ✅ All SDK prompts (01-08) complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
  - Section: Hexagonal Architecture (Ports & Adapters)
  - Section: Domain-Driven Design patterns

- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
  - All technology versions

- `/var/www/docs/islamic-financing/master-blueprint/12_JAVA_OPTIMIZED_ARCHITECTURE.md`
  - Java 21 features
  - Spring Boot 4.0.2 patterns

- `/var/www/docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md`
  - Logging, tracing, metrics

---

## 🎯 Implementation Requirements

### Technologies
- **Java**: 21.0.10
- **Spring Boot**: 4.0.2
- **PostgreSQL**: 18.1
- **Flyway**: 10.x
- **Docker**: Multi-stage build

### Hexagonal Architecture Layers

Create template with these layers:

#### 1. Domain Layer (`domain/`)
- `model/` - Aggregates (Loan, Customer, etc.)
- `service/` - Domain services
- `port/in/` - Use case interfaces
- `port/out/` - Repository interfaces
- NO infrastructure dependencies

#### 2. Application Layer (`application/`)
- `usecase/` - Use case implementations (orchestration)
- `dto/` - DTOs for API contracts
- `mapper/` - Domain ↔ DTO mapping
- Depends on: Domain layer only

#### 3. Infrastructure Layer (`infrastructure/`)
- `persistence/` - JPA repositories, entities
  - `entity/` - JPA entities (separate from domain!)
  - `repository/` - JPA repository implementations
  - `mapper/` - Domain ↔ JPA entity mapping
- `messaging/` - Kafka producers/consumers
- `config/` - Spring configuration classes
- Implements: Output ports from domain

#### 4. Adapter Layer (`adapter/`)
- `rest/` - REST controllers
  - `controller/` - @RestController classes
  - `request/` - Request DTOs
  - `response/` - Response DTOs
- `temporal/` - Temporal activities
  - `activity/` - Activity implementations
- `client/` - External service clients
- Implements: Input ports from domain

### What to Implement

#### 1. POM Configuration
- Parent: `ksa-financing-platform`
- Dependencies:
  - All 8 internal SDKs
  - Spring Boot Starter Web
  - Spring Boot Starter Data JPA
  - Spring Boot Starter Validation
  - PostgreSQL Driver
  - Flyway
  - OpenTelemetry
  - Keycloak Spring Security
  - Lombok

#### 2. Application Configuration (`resources/`)
- `application.yml` - Main config
- `application-dev.yml` - Local development
- `application-prod.yml` - Production
- `logback-spring.xml` - Logging (extends base from infra SDK)
- `db/migration/V1__initial_schema.sql` - Flyway template

#### 3. Docker Configuration
- `Dockerfile` - Multi-stage build with GraalVM
- `.dockerignore` - Exclude unnecessary files

#### 4. Kubernetes Manifests (`k8s/`)
- `deployment.yaml` - Deployment with all env vars
- `service.yaml` - ClusterIP service
- `configmap.yaml` - Non-sensitive config
- `secret.yaml` - Sensitive config (example)
- `hpa.yaml` - Horizontal Pod Autoscaler

#### 5. Main Application Class
- `ServiceNameApplication.java` - @SpringBootApplication
- Enable all required configurations

#### 6. Example REST Controller
- `HealthController.java` - Health check endpoint
- Shows proper error handling, logging, validation

#### 7. Example Use Case
- Template use case implementation
- Shows how to use domain ports
- Shows transaction boundaries
- Shows event publishing

#### 8. Example JPA Entity
- Shows separation from domain model
- Shows proper auditing fields
- Shows multi-tenancy support (tenant_id column)

#### 9. Example Temporal Activity
- Shows activity implementation
- Shows retry policies
- Shows error handling

#### 10. Security Configuration
- Keycloak OAuth2 integration
- Tenant extraction from JWT
- Method-level security

---

## 🧪 Testing Requirements

Create test templates:
- `*Test.java` - Unit test template
- `*IntegrationTest.java` - Integration test with containers
- `*RestControllerTest.java` - REST API test
- `*TemporalActivityTest.java` - Temporal activity test

---

## ✅ Success Criteria

- [ ] All layers follow Hexagonal Architecture
- [ ] Domain layer has NO infrastructure dependencies
- [ ] REST endpoints secured with Keycloak
- [ ] Database migrations work with Flyway
- [ ] Docker image builds successfully
- [ ] Kubernetes manifests are valid
- [ ] Service starts and passes health check
- [ ] Logs are JSON-structured with correlation ID
- [ ] Metrics exported to Prometheus
- [ ] Tests pass: `mvn test -pl services/service-template`
- [ ] Build succeeds: `mvn clean install -pl services/service-template`

---

## 🔄 Next Step

After creating this template, copy it for each microservice:
- **Prompt 01**: Global Profile Index service
- **Prompt 02**: PII Vault service
- And so on...

---

**This template is the foundation for all 20 microservices. Make it perfect.**
