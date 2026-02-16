# 🚀 Prompt 00: Initial Monorepo Setup

**Objective**: Create the complete directory structure and initial boilerplate files for the KSA Islamic Financing Platform monorepo.

**Reference Documents**:
- Master Blueprint: `/var/www/docs/islamic-financing/master-blueprint/00_MASTER_BLUEPRINT_INDEX.md`
- Architecture Principles: `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
- Technology Stack: `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`

---

## 📋 Tasks to Complete

### 1. Create Complete Directory Structure

Create the following directory tree at `/var/www/islamic-financing-platform/`:

```
islamic-financing-platform/
├── shared-libraries/
│   ├── foundational-infra-sdk/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ksa/financing/infra/
│   │   │   │   │   ├── security/
│   │   │   │   │   ├── logging/
│   │   │   │   │   ├── exception/
│   │   │   │   │   ├── resilience/
│   │   │   │   │   └── idempotency/
│   │   │   │   └── resources/
│   │   │   │       └── META-INF/
│   │   │   └── test/
│   │   │       └── java/com/ksa/financing/infra/
│   │   └── pom.xml
│   │
│   ├── domain-core-sdk/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   └── java/com/ksa/financing/domain/
│   │   │   │       ├── model/
│   │   │   │       ├── valueobject/
│   │   │   │       ├── sharia/
│   │   │   │       ├── port/
│   │   │   │       │   ├── in/
│   │   │   │       │   └── out/
│   │   │   │       ├── event/
│   │   │   │       └── enums/
│   │   │   └── test/
│   │   └── pom.xml
│   │
│   ├── messaging-event-sdk/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ksa/financing/messaging/
│   │   │   │   │   ├── kafka/
│   │   │   │   │   ├── contract/
│   │   │   │   │   └── grpc/
│   │   │   │   ├── proto/
│   │   │   │   ├── avro/
│   │   │   │   └── resources/
│   │   │   └── test/
│   │   └── pom.xml
│   │
│   ├── workflow-orchestration-sdk/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ksa/financing/workflow/
│   │   │   │   │   ├── activity/
│   │   │   │   │   ├── saga/
│   │   │   │   │   ├── versioning/
│   │   │   │   │   └── config/
│   │   │   │   └── resources/
│   │   │   └── test/
│   │   └── pom.xml
│   │
│   ├── lms-adapter-sdk/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ksa/financing/adapter/lms/
│   │   │   │   │   ├── port/
│   │   │   │   │   ├── fineract/
│   │   │   │   │   ├── intent/
│   │   │   │   │   └── ledger/
│   │   │   │   └── resources/
│   │   │   └── test/
│   │   └── pom.xml
│   │
│   ├── compliance-localization-sdk/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ksa/financing/compliance/
│   │   │   │   │   ├── zatca/
│   │   │   │   │   ├── sama/
│   │   │   │   │   ├── date/
│   │   │   │   │   └── vat/
│   │   │   │   └── resources/
│   │   │   │       └── schemas/
│   │   │   └── test/
│   │   └── pom.xml
│   │
│   ├── reporting-projection-sdk/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ksa/financing/projection/
│   │   │   │   │   ├── projector/
│   │   │   │   │   ├── readmodel/
│   │   │   │   │   ├── repository/
│   │   │   │   │   └── view/
│   │   │   │   └── resources/
│   │   │   └── test/
│   │   └── pom.xml
│   │
│   └── test-harness-sdk/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/ksa/financing/test/
│       │   │   │   ├── temporal/
│       │   │   │   ├── adapter/
│       │   │   │   ├── fixture/
│       │   │   │   ├── assertion/
│       │   │   │   └── container/
│       │   │   └── resources/
│       │   └── test/
│       └── pom.xml
│
├── services/
│   ├── service-template/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/ksa/financing/template/
│   │   │   │   │   ├── domain/
│   │   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── port/
│   │   │   │   │   │   │   ├── in/
│   │   │   │   │   │   │   └── out/
│   │   │   │   │   │   └── service/
│   │   │   │   │   ├── application/
│   │   │   │   │   │   ├── usecase/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── infrastructure/
│   │   │   │   │   │   ├── persistence/
│   │   │   │   │   │   ├── messaging/
│   │   │   │   │   │   └── config/
│   │   │   │   │   └── adapter/
│   │   │   │   │       ├── rest/
│   │   │   │   │       ├── temporal/
│   │   │   │   │       └── client/
│   │   │   │   └── resources/
│   │   │   │       ├── application.yml
│   │   │   │       ├── application-dev.yml
│   │   │   │       ├── application-prod.yml
│   │   │   │       ├── logback-spring.xml
│   │   │   │       └── db/
│   │   │   │           └── migration/
│   │   │   │               └── V1__initial_schema.sql
│   │   │   └── test/
│   │   │       ├── java/
│   │   │       └── resources/
│   │   ├── k8s/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   ├── configmap.yaml
│   │   │   └── secret.yaml
│   │   ├── Dockerfile
│   │   ├── .dockerignore
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
└── infrastructure/
    ├── docker-compose/
    │   ├── docker-compose.yml
    │   ├── logging/
    │   │   ├── docker-compose-elk.yml
    │   │   ├── logstash.conf
    │   │   └── elasticsearch.yml
    │   └── .env.example
    │
    ├── kubernetes/
    │   ├── logging/
    │   │   ├── elasticsearch-statefulset.yaml
    │   │   ├── logstash-deployment.yaml
    │   │   ├── kibana-deployment.yaml
    │   │   ├── filebeat-daemonset.yaml
    │   │   └── elastalert-deployment.yaml
    │   ├── monitoring/
    │   │   ├── prometheus/
    │   │   ├── grafana/
    │   │   └── jaeger/
    │   └── services/
    │       └── base/
    │
    └── terraform/
        ├── modules/
        └── environments/
```

---

### 2. Create Initial Configuration Files

#### 2.1 Root .editorconfig
Create `/var/www/islamic-financing-platform/.editorconfig`:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 4
trim_trailing_whitespace = true

[*.{yml,yaml}]
indent_size = 2

[*.{xml,html}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false

[Makefile]
indent_style = tab
```

#### 2.2 Maven Wrapper Configuration
Create `.mvn/wrapper/maven-wrapper.properties`:

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
```

#### 2.3 GitHub Actions Workflow
Create `.github/workflows/ci.yml`:

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven

    - name: Build with Maven
      run: mvn clean verify

    - name: Run tests
      run: mvn test

    - name: Generate coverage report
      run: mvn jacoco:report
```

---

### 3. Create Stub POM Files for All SDKs

For each SDK, create a minimal `pom.xml` that:
1. Extends the parent POM
2. Declares the artifact ID
3. Lists basic dependencies (to be filled in detailed prompts)
4. Includes build configuration

**Example structure for each SDK POM**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ksa.financing</groupId>
        <artifactId>ksa-financing-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>[SDK-NAME]</artifactId>
    <name>[SDK Display Name]</name>
    <description>[SDK Description]</description>

    <dependencies>
        <!-- To be filled in detailed prompts -->
    </dependencies>
</project>
```

---

### 4. Create Common Resource Files

#### 4.1 Logback Base Configuration
Create template at `shared-libraries/foundational-infra-sdk/src/main/resources/logback-base.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <springProperty scope="context" name="springAppName" source="spring.application.name"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>tenantId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <customFields>{"service":"${springAppName}"}</customFields>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH:-logs}/${springAppName}.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH:-logs}/${springAppName}-%d{yyyy-MM-dd}.json</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>tenantId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <customFields>{"service":"${springAppName}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

### 5. Create Documentation Structure

Create the following documentation files:

#### 5.1 Development Guide
`docs/development/GETTING_STARTED.md` - Developer onboarding guide

#### 5.2 Architecture Documentation
`docs/architecture/HEXAGONAL_ARCHITECTURE.md` - Hexagonal architecture patterns

#### 5.3 API Documentation
`docs/api/API_STANDARDS.md` - REST API standards

---

### 6. Create Makefile for Common Commands

Create `Makefile` at root:

```makefile
.PHONY: help build test clean install docker-up docker-down

help:
	@echo "KSA Islamic Financing Platform - Available Commands"
	@echo ""
	@echo "  make build        - Build all modules"
	@echo "  make test         - Run all tests"
	@echo "  make clean        - Clean all build artifacts"
	@echo "  make install      - Install all modules to local Maven repo"
	@echo "  make docker-up    - Start all infrastructure services"
	@echo "  make docker-down  - Stop all infrastructure services"

build:
	./mvnw clean package -DskipTests

test:
	./mvnw test

clean:
	./mvnw clean

install:
	./mvnw clean install

docker-up:
	cd infrastructure/docker-compose && docker-compose up -d

docker-down:
	cd infrastructure/docker-compose && docker-compose down
```

---

## ✅ Success Criteria

After completing this prompt, you should have:

- [ ] Complete directory structure for all 8 SDKs
- [ ] Complete directory structure for 21 microservices (folders only)
- [ ] Infrastructure directories (docker-compose, kubernetes, terraform)
- [ ] Stub POM files for all SDKs
- [ ] Common configuration files (.editorconfig, .mvn, .github)
- [ ] Logback base configuration
- [ ] Documentation structure
- [ ] Makefile with common commands
- [ ] All directories created with proper Java package structure

---

## 📝 Notes

- Do NOT implement any Java classes yet - just create the directory structure
- Do NOT implement detailed configuration yet - just stubs
- Focus on creating a clean, organized structure
- Ensure all paths match the Hexagonal Architecture pattern
- All Java packages should follow: `com.ksa.financing.{module}.{layer}`

---

## 🔄 Next Step

After completing this initial setup, proceed to:
- **Prompt 01**: foundational-infra-sdk implementation
- **Prompt 02**: domain-core-sdk implementation
- And so on...

---

**Execute this prompt first, then move to SDK-specific prompts.**
