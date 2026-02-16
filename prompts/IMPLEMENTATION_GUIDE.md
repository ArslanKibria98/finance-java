# 🚀 Implementation Guide

## Overview

This guide explains how to use the prompts to build the entire KSA Islamic Financing Platform.

---

## 🎯 Approach

Each prompt is **concise and reference-based**:
- Lists **what** to build
- Points to **reference documents** for detailed specifications
- Implementing agent reads references and decides **how** to implement
- No spoon-feeding of complete code

---

## 📋 Execution Steps

### Step 1: Initial Setup
```bash
cd /var/www/islamic-financing-platform
cat prompts/00-INITIAL-SETUP.md
```

This creates:
- Complete directory structure
- All package folders
- Stub POM files
- Configuration files

**Do NOT implement any Java classes yet** - just directory structure.

---

### Step 2: Implement SDKs (In Order!)

SDKs have dependencies, execute in this exact order:

#### SDK 01: Foundational Infrastructure SDK
```bash
cat prompts/sdks/01-FOUNDATIONAL-INFRA-SDK.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
- Read `/var/www/docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md`

**Implement**:
- Keycloak security integration
- Structured JSON logging (Logback + Logstash Encoder)
- OpenTelemetry tracing
- Resilience4j patterns
- Redis idempotency store

**Verify**:
```bash
cd /var/www/islamic-financing-platform
mvn clean install -pl shared-libraries/foundational-infra-sdk
mvn test -pl shared-libraries/foundational-infra-sdk
```

---

#### SDK 02: Domain Core SDK
```bash
cat prompts/sdks/02-DOMAIN-CORE-SDK.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md` (critical!)
- Read `/var/www/docs/islamic-financing/erd-docs/lending-service.md`
- Read `/var/www/docs/islamic-financing/product-specifications/01_MURABAHA_PERSONAL_FINANCING.md`

**Implement**:
- Value objects (Money, ProfitRate, IBAN, NationalId, etc.)
- Domain entities (Loan, Customer, Wallet)
- Sharia calculators (Murabaha, Ijara, Tawarruq, Charity Penalty)
- Domain events
- Ports (Hexagonal Architecture)

**Critical**: NO Spring, NO Jakarta EE, NO infrastructure in this SDK!

**Verify**:
```bash
mvn clean install -pl shared-libraries/domain-core-sdk
mvn test -pl shared-libraries/domain-core-sdk
```

---

#### SDK 03: Messaging & Event SDK
```bash
cat prompts/sdks/03-MESSAGING-EVENT-SDK.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/06_ORCHESTRATION_PATTERNS.md`

**Implement**:
- Kafka producer/consumer with Avro
- Schema Registry integration
- gRPC server/client configuration
- Protobuf definitions
- Dead Letter Queue handling

**Verify**:
```bash
mvn clean install -pl shared-libraries/messaging-event-sdk
```

---

#### SDK 04: Workflow Orchestration SDK
```bash
cat prompts/sdks/04-WORKFLOW-ORCHESTRATION-SDK.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/06_ORCHESTRATION_PATTERNS.md` (especially SAGA)

**Implement**:
- Temporal client configuration
- Activity base classes with retry policies
- SAGA orchestrator
- Workflow versioning utilities

**Verify**:
```bash
mvn clean install -pl shared-libraries/workflow-orchestration-sdk
```

---

#### SDK 05: LMS Adapter SDK
```bash
cat prompts/sdks/05-LMS-ADAPTER-SDK.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/07_CORE_BANKING_ADAPTER.md`

**Implement**:
- Intent-based abstraction layer
- Fineract REST client
- Ledger integration

---

#### SDK 06: Compliance & Localization SDK
```bash
cat prompts/sdks/06-COMPLIANCE-LOCALIZATION-SDK.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/08_KSA_GOVERNMENT_APIS.md`
- Read `/var/www/docs/islamic-financing/master-blueprint/17_LOAN_SERVICING_RESTRUCTURING.md`

**Implement**:
- ZATCA e-invoicing
- VAT calculation (15% on profit)
- Hijri calendar
- SAMA audit logging

---

#### SDK 07: Reporting & Projection SDK
```bash
cat prompts/sdks/07-REPORTING-PROJECTION-SDK.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md` (CQRS section)

**Implement**:
- Event projectors
- Read models
- Materialized views

---

#### SDK 08: Test Harness SDK
```bash
cat prompts/sdks/08-TEST-HARNESS-SDK.md
```

**Implement**:
- Testcontainers wrappers
- Test fixtures
- Temporal test utilities
- Mock servers (Fineract, ZATCA, Simah)

---

### Step 3: Create Service Template

```bash
cat prompts/services/00-SERVICE-TEMPLATE.md
```

**Before implementing**:
- Read `/var/www/docs/islamic-financing/master-blueprint/12_JAVA_OPTIMIZED_ARCHITECTURE.md`
- Read `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`

**Implement**:
- Hexagonal Architecture template
- All 4 layers (domain, application, infrastructure, adapter)
- Example REST controller
- Example use case
- Flyway migration template
- Docker multi-stage build
- Kubernetes manifests

This template will be **copied** for all 20 microservices.

---

### Step 4: Implement Microservices

For each microservice:

1. **Copy service template**:
```bash
cp -r services/service-template services/lending-service
```

2. **Read the service prompt**:
```bash
cat prompts/services/05-LENDING-SERVICE.md
```

3. **Read ALL reference documents** mentioned in prompt:
- Master blueprint sections
- ERD documentation
- Product specifications
- User journeys

4. **Implement** based on your understanding

5. **Verify**:
```bash
mvn clean install -pl services/lending-service
mvn test -pl services/lending-service
docker build -t lending-service:1.0 services/lending-service
```

6. **Move to next service**

---

## 🎯 Example: Implementing Lending Service

### 1. Read Prompt
```bash
cat prompts/services/05-LENDING-SERVICE.md
```

Prompt says:
- Implement loan origination
- Read ERD: `/var/www/docs/islamic-financing/erd-docs/lending-service.md`
- Read Blueprint: `/var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md`

### 2. Read Reference Documents

Open and thoroughly read:
- `erd-docs/lending-service.md` → Database schema
- `master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md` → Business rules
- `product-specifications/01_MURABAHA_PERSONAL_FINANCING.md` → Product details
- `user-journeys/01_INDIVIDUAL_BORROWER.md` → User flows

### 3. Understand Requirements

From ERD: Tables to create
From Blueprint: Business rules to implement
From Product Spec: Murabaha calculations
From User Journey: API endpoints needed

### 4. Implement

```java
// Domain layer
Loan.java - Aggregate root with lifecycle methods

// Application layer
CreateLoanUseCase.java - Use case implementation

// Infrastructure layer
LoanJpaRepository.java - JPA implementation
LoanEntity.java - JPA entity

// Adapter layer
LoanController.java - REST endpoints
CreateLoanActivity.java - Temporal activity
```

### 5. Write Tests

```java
LoanTest.java - Domain logic tests
CreateLoanUseCaseTest.java - Use case tests
LoanControllerTest.java - REST API tests
```

### 6. Create Database Migration

```sql
-- V1__initial_schema.sql
CREATE TABLE loans (
  -- columns from ERD
);
```

### 7. Verify

```bash
mvn clean install -pl services/lending-service
mvn test -pl services/lending-service
```

---

## 📊 Verification Checklist

After each SDK/service implementation:

- [ ] All reference documents read
- [ ] Code follows Hexagonal Architecture
- [ ] Tests written and passing
- [ ] Build successful: `mvn clean install`
- [ ] Docker image builds: `docker build`
- [ ] No TODO comments left in code
- [ ] JavaDoc on all public methods
- [ ] Logging structured (JSON) with correlation ID
- [ ] Multi-tenancy support (tenant_id in JWT)
- [ ] Database migrations versioned (Flyway)

---

## 🚨 Common Mistakes to Avoid

1. **Skipping reference documents**
   - ❌ Just reading prompt and guessing implementation
   - ✅ Read ALL referenced documents first

2. **Breaking Hexagonal Architecture**
   - ❌ Domain entities with @Entity annotation
   - ✅ Separate JPA entities from domain model

3. **Incorrect SAGA compensation**
   - ❌ No compensation actions
   - ✅ Every SAGA step has rollback logic

4. **Missing multi-tenancy**
   - ❌ No tenant_id filtering in queries
   - ✅ All queries filter by tenant_id from JWT

5. **Ignoring Islamic finance rules**
   - ❌ Using "interest rate" terminology
   - ✅ Using "profit rate" (markup percentage)

---

## 📖 Reference Document Map

Quick reference for common lookups:

| Need | Document |
|------|----------|
| Sharia calculations | `master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md` |
| Database schemas | `erd-docs/*.sql` |
| API standards | `master-blueprint/01_ARCHITECTURE_PRINCIPLES.md` |
| Technology versions | `master-blueprint/02_TECHNOLOGY_STACK.md` |
| Temporal patterns | `master-blueprint/06_ORCHESTRATION_PATTERNS.md` |
| Fineract integration | `master-blueprint/07_CORE_BANKING_ADAPTER.md` |
| KSA government APIs | `master-blueprint/08_KSA_GOVERNMENT_APIS.md` |
| Logging/monitoring | `master-blueprint/09_OBSERVABILITY_OPERATIONS.md` |

---

## 🎯 Success Metrics

By the end, you should have:

✅ 8 SDKs fully implemented and tested
✅ 1 service template (Hexagonal Architecture)
✅ 20 microservices (all using template)
✅ 4 infrastructure setups (Docker Compose, Kubernetes)
✅ ~91,000 lines of production code
✅ All tests passing
✅ All Docker images building
✅ Complete platform ready for deployment

---

## 🆘 Getting Help

If any reference document is unclear:
1. Re-read the document slowly
2. Check related documents for context
3. Look at ERD for data structure
4. Look at product specs for business rules
5. Ask specific questions with document references

Example good question:
> "In `05_SHARIA_COMPLIANCE_ENGINE.md` section 3.2, it mentions Ibra calculation. Does this mean the rebate percentage is configurable per tenant or globally?"

---

**Remember**: Prompts guide **what** to build. Reference documents specify **how** to build it. Your understanding bridges the gap.

Good luck! 🚀
