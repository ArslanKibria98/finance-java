# Foundational Infrastructure SDK

Core infrastructure components for the KSA Islamic Financing Platform, providing enterprise-grade security, logging, exception handling, resilience, and idempotency.

## Overview

This SDK provides cross-cutting concerns for all microservices in the platform:

- **Security**: Keycloak JWT authentication, role-based access control, multi-tenant context management
- **Logging**: Structured JSON logging with OpenTelemetry distributed tracing
- **Exception Handling**: Global exception handlers with standardized error responses
- **Resilience**: Circuit breaker, retry, and bulkhead patterns via Resilience4j
- **Idempotency**: Redis-backed idempotent operation execution

## Package Structure

```
com.ksa.financing.infra/
├── security/              # Keycloak JWT, RBAC, Tenant Context
│   ├── KeycloakSecurityConfig.java
│   ├── KeycloakRoleConverter.java
│   ├── TenantContextHolder.java
│   └── TenantContextFilter.java
├── logging/               # Structured Logging & Tracing
│   ├── StructuredLogger.java
│   ├── CorrelationIdFilter.java
│   ├── OpenTelemetryConfig.java
│   └── TracingMdcAspect.java
├── exception/             # Exception Handling
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   ├── TechnicalException.java
│   ├── NotFoundException.java
│   └── ErrorResponse.java
├── resilience/            # Resilience Patterns
│   ├── CircuitBreakerConfiguration.java
│   └── RetryConfiguration.java
└── idempotency/           # Idempotency
    ├── IdempotencyStore.java
    ├── RedisIdempotencyStore.java
    ├── Idempotent.java
    └── IdempotencyAspect.java
```

## Usage

### Maven Dependency

Add to your service `pom.xml`:

```xml
<dependency>
    <groupId>com.ksa.financing</groupId>
    <artifactId>foundational-infra-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Security - Multi-Tenant Context

```java
// Extract tenant context (automatic via filter)
String tenantId = TenantContextHolder.getTenantId();
String userId = TenantContextHolder.getUserId();
```

### Structured Logging

```java
private static final StructuredLogger log = StructuredLogger.getLogger(MyService.class);

log.info("Loan approved",
    "loanId", loanId,
    "amount", amount,
    "customerId", customerId);
```

### Exception Handling

```java
// Throw domain exceptions
throw new BusinessException("INVALID_AMOUNT", "Loan amount exceeds limit");

// Throw technical exceptions
throw new TechnicalException("DB_ERROR", "Database connection failed", cause);

// Resource not found
throw new NotFoundException("Loan", loanId);
```

### Idempotency

```java
// Annotation-based
@Idempotent(keyExpression = "#command.applicationId + ':disburse'")
public DisbursementResult disburse(DisbursementCommand command) {
    // Executed at-most-once
}

// Programmatic
idempotencyStore.executeIdempotent(key, () -> {
    // Your operation
});
```

### Resilience

Configuration via `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

## Configuration

Required environment variables:

- `REDIS_HOST`: Redis server host (default: localhost)
- `REDIS_PORT`: Redis server port (default: 6379)
- `KEYCLOAK_AUTH_SERVER_URL`: Keycloak server URL
- `KEYCLOAK_REALM`: Keycloak realm name
- `JAEGER_ENDPOINT`: Jaeger collector endpoint for tracing

## Features

### Security
- OAuth 2.0 / OIDC via Keycloak
- JWT token validation
- Role-based access control (RBAC)
- Multi-tenant context propagation
- Thread-safe tenant isolation

### Observability
- Distributed tracing with OpenTelemetry + Jaeger
- Structured JSON logging (ELK-compatible)
- Correlation ID propagation
- MDC context enrichment

### Resilience
- Circuit breaker pattern
- Retry with exponential backoff
- Bulkhead isolation
- Rate limiting

### Idempotency
- Redis-backed idempotent execution
- Annotation-based (@Idempotent)
- SpEL expression support for key generation
- Configurable TTL

## Testing

Run tests with Testcontainers:

```bash
mvn test
```

Tests include:
- Redis idempotency store integration tests
- Security configuration tests
- Exception handler tests

## Architecture Alignment

This SDK implements the principles from:
- [Architecture Principles](../../docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md)
- [Security & Data Residency](../../docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md)
- [Observability](../../docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md)
- [SDK Ecosystem](../../docs/islamic-financing/master-blueprint/18_SDK_ECOSYSTEM.md)

## Build

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Install to local repository
mvn clean install
```

## Version

**1.0.0-SNAPSHOT**

## License

Proprietary - KSA Islamic Financing Platform
