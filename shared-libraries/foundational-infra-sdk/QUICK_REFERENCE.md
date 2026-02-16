# Foundational Infrastructure SDK - Quick Reference

## Maven Dependency

```xml
<dependency>
    <groupId>com.ksa.financing</groupId>
    <artifactId>foundational-infra-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Component Scanning

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.ksa.financing.infra",
    "com.your.service.package"
})
public class YourApplication { }
```

## 1. Security

### Get Tenant Context

```java
String tenantId = TenantContextHolder.getTenantId();
String userId = TenantContextHolder.getUserId();
```

### Secure Endpoint

```java
@PreAuthorize("hasRole('UNDERWRITER')")
@PostMapping("/loans/{id}/approve")
public Response approveLoan(@PathVariable String id) { }
```

## 2. Logging

### Structured Logging

```java
import com.ksa.financing.infra.logging.StructuredLogger;

private static final StructuredLogger log =
    StructuredLogger.getLogger(MyService.class);

// Log with structured fields
log.info("Loan created",
    "loanId", loanId,
    "amount", amount,
    "customerId", customerId);

log.error("Failed to process", exception,
    "loanId", loanId,
    "reason", reason);
```

### Correlation ID (Automatic)

Automatically added to:
- Response headers: `X-Correlation-ID`
- MDC: `correlationId`
- Logs: included in JSON output

## 3. Exception Handling

### Throw Exceptions

```java
// Business logic errors (422)
throw new BusinessException("LOAN_001", "Invalid loan amount");

// Resource not found (404)
throw new NotFoundException("Loan", loanId);

// Technical errors (500)
throw new TechnicalException("DB_001", "Database error", cause);
```

### Error Response Format

```json
{
  "timestamp": "2026-02-12T10:30:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "LOAN_001",
  "message": "Invalid loan amount",
  "path": "/api/v1/loans",
  "traceId": "abc123..."
}
```

## 4. Idempotency

### Annotation-Based

```java
@Idempotent(
    keyExpression = "#command.loanId + ':disburse'",
    ttlSeconds = 86400
)
public DisbursementResult disburse(DisbursementCommand command) {
    // This will execute at-most-once for the same key
    return performDisbursement(command);
}
```

### Programmatic

```java
@Autowired
private IdempotencyStore idempotencyStore;

public Result process(String id) {
    String key = id + ":process";
    return idempotencyStore.executeIdempotent(key, () -> {
        return doProcessing(id);
    });
}
```

## 5. Resilience

### Circuit Breaker

```java
@CircuitBreaker(name = "externalApi")
public Response callExternalApi() {
    return externalClient.call();
}
```

### Retry

```java
@Retry(name = "simahApi")
public SimahReport getSimahReport(String nationalId) {
    return simahClient.getReport(nationalId);
}
```

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      externalApi:
        failureRateThreshold: 50
        waitDurationInOpenState: 10s

  retry:
    instances:
      simahApi:
        maxAttempts: 3
        waitDuration: 500ms
```

## Configuration Template

```yaml
spring:
  application:
    name: my-service

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

keycloak:
  auth-server-url: ${KEYCLOAK_AUTH_SERVER_URL:http://keycloak:8080}
  realm: ${KEYCLOAK_REALM:ksa-financing}
  resource: ${SERVICE_NAME:my-service}

opentelemetry:
  jaeger:
    endpoint: ${JAEGER_ENDPOINT:http://jaeger:14250}
```

## Common Patterns

### Service with All Features

```java
@Service
@RequiredArgsConstructor
public class LoanService {

    private static final StructuredLogger log =
        StructuredLogger.getLogger(LoanService.class);

    private final IdempotencyStore idempotencyStore;
    private final ExternalClient externalClient;

    @Idempotent(keyExpression = "#loanId + ':approve'")
    @CircuitBreaker(name = "loanApproval")
    @Retry(name = "loanRetry")
    public LoanApprovalResult approveLoan(String loanId) {
        String tenantId = TenantContextHolder.getTenantId();

        log.info("Approving loan",
            "loanId", loanId,
            "tenantId", tenantId);

        try {
            LoanApprovalResult result = performApproval(loanId);

            log.info("Loan approved",
                "loanId", loanId,
                "status", result.getStatus());

            return result;

        } catch (Exception e) {
            log.error("Loan approval failed", e,
                "loanId", loanId);
            throw new BusinessException("APPROVAL_FAILED",
                "Failed to approve loan", e);
        }
    }
}
```

## Troubleshooting

### Issue: Tenant context is null

**Solution**: Ensure JWT contains `tenant_id` claim or pass `X-Tenant-ID` header

### Issue: Idempotency not working

**Solution**:
1. Check Redis connection
2. Verify SpEL expression in `@Idempotent`
3. Ensure AOP is enabled

### Issue: Tracing not appearing in Jaeger

**Solution**:
1. Verify Jaeger endpoint configuration
2. Check network connectivity
3. Ensure OpenTelemetry autoconfiguration is enabled

### Issue: Circuit breaker not triggering

**Solution**:
1. Check Resilience4j configuration
2. Verify minimum number of calls threshold
3. Review failure rate threshold

## Best Practices

1. **Always use structured logging** instead of string concatenation
2. **Add idempotency keys** to all mutation operations
3. **Use BusinessException** for domain errors, not RuntimeException
4. **Configure circuit breakers** for all external API calls
5. **Include correlation IDs** in all outbound requests
6. **Set appropriate TTLs** for idempotency keys based on operation type

## Health Checks

Built-in health indicators:

- `/actuator/health/redis` - Redis connectivity
- `/actuator/health/circuitbreakers` - Circuit breaker status

## Metrics

Available metrics:

- `resilience4j.circuitbreaker.state` - Circuit breaker states
- `resilience4j.retry.calls` - Retry attempts
- `idempotency.cache.hits` - Idempotent cache hits
- `security.tenant.context.sets` - Tenant context operations

## Support

For issues or questions:
1. Check logs with correlation ID
2. Review distributed traces in Jaeger
3. Check Redis for idempotency key values
4. Verify JWT token claims

---

**Last Updated**: February 12, 2026
**SDK Version**: 1.0.0-SNAPSHOT
