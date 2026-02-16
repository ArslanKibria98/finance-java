# 🏦 KSA Islamic Financing Platform - Monorepo Boilerplate

**Enterprise-Grade Microservices Platform for Islamic Finance in Saudi Arabia**

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Temporal](https://img.shields.io/badge/Temporal-1.32.1-blue.svg)](https://temporal.io)
[![Keycloak](https://img.shields.io/badge/Keycloak-26.5.2-red.svg)](https://www.keycloak.org/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-9.3.0-yellow.svg)](https://www.elastic.co/)

---

## 📋 Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [Development Guide](#development-guide)
- [SDK Ecosystem](#sdk-ecosystem)
- [Service Template](#service-template)
- [Logging & Observability](#logging--observability)
- [Deployment](#deployment)

> **📖 [Complete Documentation](docs/README.md)** | **🚀 [Quick Start Guide](docs/getting-started/quick-start.md)** | **🏗️ [Infrastructure Setup](docs/infrastructure/local-development/README.md)**

---

## 🎯 Overview

This monorepo contains the complete enterprise platform for **SAMA-compliant Islamic Financing** in the Kingdom of Saudi Arabia. The platform is built using:

- **Hexagonal Architecture** (Ports & Adapters)
- **Domain-Driven Design** (DDD)
- **Event-Driven Architecture** with Kafka
- **Workflow Orchestration** with Temporal.io
- **ELK Stack 9.3.0** for centralized logging
- **Multi-Tenant** support with data sovereignty

**Key Features:**
- ✅ Sharia-compliant financing products (Murabaha, Ijara, Tawarruq)
- ✅ Complete KYC/AML integration (Nafath, Yakeen, Simah)
- ✅ 7-year audit log retention (SAMA compliance)
- ✅ Multi-jurisdictional data sovereignty
- ✅ Real-time fraud detection and risk assessment
- ✅ Closed-loop wallet system
- ✅ Apache Fineract LMS integration

---

## 🏗️ Architecture

The platform follows a **microservices architecture** with 16 core services and 8 shared SDKs:

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (Kong)                        │
│                 + Keycloak Authentication                    │
└────────────────────────┬────────────────────────────────────┘
                         │
           ┌─────────────┴──────────────┐
           │                             │
    ┌──────▼──────┐            ┌────────▼────────┐
    │  16 Core    │            │  8 Shared SDKs  │
    │  Services   │◄───────────┤   (Libraries)   │
    └──────┬──────┘            └─────────────────┘
           │
    ┌──────▼──────────────────────────┐
    │  Temporal.io Orchestration      │
    │  (All cross-service workflows)  │
    └──────┬──────────────────────────┘
           │
    ┌──────▼────────┐
    │  Kafka Events │
    │  PostgreSQL   │
    │  Redis Cache  │
    └───────────────┘
```

---

## 💻 Technology Stack - **Latest Stable Versions (Feb 2026)**

### Core Stack
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21.0.10 | LTS with Virtual Threads |
| **Spring Boot** | 4.0.2 | Application framework |
| **Temporal SDK** | 1.32.1 | Workflow orchestration |
| **Keycloak** | 26.5.2 | Identity & Access Management |
| **PostgreSQL** | 18.1 | Transactional database |
| **Redis** | 8.0.2 | Caching & idempotency |
| **Apache Kafka** | 3.9.1 | Event streaming |
| **Apache Fineract** | 1.13.0 | Core banking system |
| **GraalVM** | 21.0.9 | Native image compilation |

### Logging Stack (ELK)
| Technology | Version | Purpose |
|------------|---------|---------|
| **Logback** | 1.5.28 | Application logging |
| **Logstash Encoder** | 9.0 | JSON log formatting |
| **Elasticsearch** | 9.3.0 | Log storage & search |
| **Logstash** | 9.3.0 | Log processing pipeline |
| **Kibana** | 9.3.0 | Log visualization |
| **Filebeat** | 8.19 | Log shipping |

### Supporting Libraries
| Technology | Version | Purpose |
|------------|---------|---------|
| **Resilience4j** | 2.3.0 | Circuit breaker, retry, bulkhead |
| **OpenTelemetry** | 1.44.1 | Distributed tracing |
| **Testcontainers** | 1.20.4 | Integration testing |

---

## 📁 Repository Structure

```
islamic-financing-platform/
├── pom.xml                           # Parent POM
├── README.md                         # This file
├── ksa-financing-bom/                # Bill of Materials
│   └── pom.xml
│
├── shared-libraries/                 # 8 Core SDKs
│   ├── foundational-infra-sdk/       # Logging, Security, Resilience
│   ├── domain-core-sdk/              # Sharia Math, Entities
│   ├── messaging-event-sdk/          # Kafka, gRPC, Avro
│   ├── workflow-orchestration-sdk/   # Temporal, SAGA
│   ├── lms-adapter-sdk/              # Fineract abstraction
│   ├── compliance-localization-sdk/  # ZATCA, SAMA, Hijri
│   ├── reporting-projection-sdk/     # CQRS read models
│   └── test-harness-sdk/             # Testing utilities
│
├── services/                         # 16 Microservices
│   ├── service-template/             # ⭐ COPY THIS FOR NEW SERVICES
│   │   ├── src/
│   │   │   ├── main/java/
│   │   │   │   └── com/ksa/financing/template/
│   │   │   │       ├── domain/       # Pure domain logic
│   │   │   │       ├── application/  # Use cases
│   │   │   │       ├── infrastructure/ # Framework code
│   │   │   │       └── adapter/      # REST, Kafka, DB
│   │   │   └── main/resources/
│   │   │       ├── application.yml
│   │   │       └── logback-spring.xml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── global-profile-index/
│   ├── pii-vault/
│   ├── kyc-orchestrator/
│   ├── identity-federation/
│   ├── lending-service/
│   ├── sharia-compliance/
│   ├── ledger-service/
│   ├── customer-service/
│   ├── product-service/
│   ├── wallet-service/
│   ├── risk-service/
│   ├── credit-decisioning/
│   ├── collections-service/
│   ├── document-service/
│   ├── partner-service/
│   ├── notification-service/
│   ├── audit-service/
│   ├── kyc-adapter/
│   ├── core-banking-adapter/
│   └── payment-adapter/
│
├── infrastructure/                   # Deployment configs
│   ├── docker-compose/
│   │   ├── docker-compose.yml        # Full local stack
│   │   └── logging/
│   │       └── docker-compose-elk.yml
│   ├── kubernetes/
│   │   ├── logging/                  # ELK Stack manifests
│   │   ├── monitoring/               # Prometheus, Grafana
│   │   └── services/                 # Service deployments
│   └── terraform/                    # Cloud infrastructure
│
└── docs/                             # Documentation
    ├── architecture/
    ├── api/
    └── deployment/
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** (OpenJDK or GraalVM 21.0.9)
- **Maven 3.9+**
- **Docker Desktop** (for local development)
- **Git**

### Quick Start

1. **Clone the repository**
```bash
git clone https://github.com/your-org/islamic-financing-platform.git
cd islamic-financing-platform
```

2. **Build all SDKs**
```bash
mvn clean install -DskipTests
```

3. **Start local infrastructure** (PostgreSQL, Redis, Kafka, Temporal, Keycloak, ELK)
```bash
cd infrastructure/docker-compose
docker-compose up -d
```

4. **Run a service** (example: lending-service)
```bash
cd services/lending-service
mvn spring-boot:run
```

5. **Access Kibana** (view logs)
```
http://localhost:5601
```

---

## 🛠️ Development Guide

### Creating a New Service

1. **Copy the service template**
```bash
cp -r services/service-template services/your-new-service
```

2. **Update package names**
```bash
# Rename com.ksa.financing.template → com.ksa.financing.yourservice
find services/your-new-service -type f -name "*.java" -exec sed -i 's/template/yourservice/g' {} \;
```

3. **Update pom.xml**
```xml
<artifactId>your-new-service</artifactId>
<name>Your New Service</name>
```

4. **Add to parent POM**
```xml
<modules>
    <!-- ... -->
    <module>services/your-new-service</module>
</modules>
```

5. **Implement domain logic** following Hexagonal Architecture

---

## 📦 SDK Ecosystem

### 1. **foundational-infra-sdk**
- Logback configuration with Logstash Encoder 9.0
- Keycloak RBAC integration
- OpenTelemetry distributed tracing
- Resilience4j circuit breaker
- Global exception handling
- Redis-backed idempotency store

### 2. **domain-core-sdk**
- Sharia Math Engine (Murabaha, Ijara, Tawarruq)
- Core domain entities (Loan, Customer, Contract)
- Value objects (Money, ProfitRate, Tenure)
- Domain events

### 3. **messaging-event-sdk**
- Kafka producer/consumer templates
- Avro schema registry integration
- gRPC service definitions

### 4. **workflow-orchestration-sdk**
- Temporal workflow interfaces
- SAGA compensation patterns
- Activity definitions

### 5. **lms-adapter-sdk**
- Apache Fineract abstraction layer
- IBankingPort interface
- Schedule management

### 6. **compliance-localization-sdk**
- ZATCA e-invoicing (XML generation, QR codes)
- SAMA regulatory reporting
- Hijri-Gregorian date conversion
- KSA VAT calculator

### 7. **reporting-projection-sdk**
- CQRS read models
- Event projectors for Kafka
- Materialized views

### 8. **test-harness-sdk**
- Temporal test environment
- Mock adapters (LMS, Keycloak, Kafka)
- Testcontainers configurations

---

## 🎨 Service Template

Every service follows **Hexagonal Architecture**:

```
src/main/java/com/ksa/financing/{service}/
├── domain/                   # ❌ NO framework dependencies
│   ├── model/                # Aggregates, Entities
│   ├── port/
│   │   ├── in/               # Use case interfaces
│   │   └── out/              # SPI (adapter interfaces)
│   └── service/              # Domain services
│
├── application/              # Orchestration layer
│   ├── usecase/              # Use case implementations
│   └── dto/                  # Request/Response DTOs
│
├── infrastructure/           # Framework implementations
│   ├── persistence/          # JPA repositories
│   ├── messaging/            # Kafka listeners
│   └── config/               # Spring configuration
│
└── adapter/                  # External integrations
    ├── rest/                 # REST controllers
    ├── temporal/             # Workflow workers
    └── client/               # External API clients
```

---

## 📊 Logging & Observability

### Structured JSON Logging

Every log entry includes:
```json
{
  "@timestamp": "2026-02-10T15:30:45.123+03:00",
  "level": "INFO",
  "service": "lending-service",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "tenantId": "tenant_001",
  "userId": "usr_12345",
  "loanId": "loan_67890",
  "amount": 50000,
  "message": "Loan application submitted successfully"
}
```

### ELK Stack Integration

1. **Application** → Logback (JSON formatter)
2. **Filebeat** (sidecar) → ships logs to Logstash
3. **Logstash** → PII redaction, enrichment
4. **Elasticsearch** → 7-year retention (SAMA compliance)
5. **Kibana** → Search & dashboards

### Accessing Kibana
```
URL: http://localhost:5601
Index: logs-*
```

**Sample Queries:**
```
# Find all errors for a specific loan
level:ERROR AND loanId:loan_67890

# Track user actions
userId:usr_12345 AND timestamp:[now-1h TO now]

# Multi-tenant isolation
tenantId:tenant_001
```

---

## 🚀 Deployment

### Docker Build (Jib)
```bash
mvn clean package jib:dockerBuild
```

### Native Image (GraalVM)
```bash
mvn -Pnative native:compile
```

### Kubernetes Deployment
```bash
kubectl apply -f infrastructure/kubernetes/services/lending-service/
```

---

## 📖 Documentation

- **Architecture Decision Records**: [docs/architecture/ADR/](/docs/architecture/ADR)
- **API Documentation**: [docs/api/](/docs/api)
- **Deployment Guide**: [docs/deployment/](/docs/deployment)
- **Master Blueprint**: [/var/www/docs/islamic-financing/master-blueprint/](/var/www/docs/islamic-financing/master-blueprint/)

---

## 🤝 Contributing

1. Create a feature branch from `main`
2. Follow Hexagonal Architecture principles
3. Add tests (unit + integration)
4. Update documentation
5. Submit a pull request

---

## 📄 License

Copyright © 2026 KSA Islamic Financing Platform. All rights reserved.

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/your-org/islamic-financing-platform/issues)
- **Documentation**: [Wiki](https://github.com/your-org/islamic-financing-platform/wiki)
- **Slack**: #islamic-financing-dev

---

**Built with ❤️ for SAMA-compliant Islamic Finance**
