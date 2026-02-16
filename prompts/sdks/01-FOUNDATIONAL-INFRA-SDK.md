# 🔧 Prompt 01: Foundational Infrastructure SDK

**Objective**: Build the complete foundational-infra-sdk with Security, Logging, Exception Handling, Resilience, and Idempotency components.

**Prerequisites**:
- Prompt 00 (Initial Setup) must be completed
- Directory structure exists at `shared-libraries/foundational-infra-sdk/`

**Reference Documents**:
- Architecture Principles: `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
- Technology Stack: `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
- Security & Data Residency: `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md`
- Observability: `/var/www/docs/islamic-financing/master-blueprint/09_OBSERVABILITY_OPERATIONS.md`
- SDK Ecosystem: `/var/www/docs/islamic-financing/master-blueprint/18_SDK_ECOSYSTEM.md`

---

## 📦 Complete POM Configuration

Create `shared-libraries/foundational-infra-sdk/pom.xml`:

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

    <artifactId>foundational-infra-sdk</artifactId>
    <name>Foundational Infrastructure SDK</name>
    <description>Core infrastructure: Security (Keycloak), Logging (Logback+Logstash), Exception Handling, Resilience (Resilience4j), Idempotency (Redis)</description>

    <dependencies>
        <!-- Spring Boot Core -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Keycloak -->
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-spring-boot-starter</artifactId>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
        </dependency>

        <!-- OpenTelemetry -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-jaeger</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry.instrumentation</groupId>
            <artifactId>opentelemetry-spring-boot-starter</artifactId>
            <version>${opentelemetry-instrumentation.version}</version>
        </dependency>

        <!-- Resilience4j -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-circuitbreaker</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-retry</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-bulkhead</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-ratelimiter</artifactId>
        </dependency>

        <!-- Micrometer for metrics -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-core</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Apache Commons -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 🔒 Security Package Implementation

### 1. Keycloak Configuration

**File**: `src/main/java/com/ksa/financing/infra/security/KeycloakSecurityConfig.java`

```java
package com.ksa.financing.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class KeycloakSecurityConfig {

    private final TenantContextFilter tenantContextFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/public/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
}
```

### 2. Keycloak Role Converter

**File**: `src/main/java/com/ksa/financing/infra/security/KeycloakRoleConverter.java`

```java
package com.ksa.financing.infra.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            authorities.addAll(realmRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList()));
        }

        // Extract resource roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(resource -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> resourceMap = (Map<String, Object>) resource;
                if (resourceMap.containsKey("roles")) {
                    @SuppressWarnings("unchecked")
                    List<String> resourceRoles = (List<String>) resourceMap.get("roles");
                    authorities.addAll(resourceRoles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList()));
                }
            });
        }

        return authorities;
    }
}
```

### 3. Tenant Context Holder

**File**: `src/main/java/com/ksa/financing/infra/security/TenantContextHolder.java`

```java
package com.ksa.financing.infra.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TenantContextHolder {

    private static final ThreadLocal<String> TENANT_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new InheritableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
        log.trace("Tenant context set: {}", tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void setUserId(String userId) {
        USER_ID.set(userId);
        log.trace("User context set: {}", userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        log.trace("Tenant and user context cleared");
    }
}
```

### 4. Tenant Context Filter

**File**: `src/main/java/com/ksa/financing/infra/security/TenantContextFilter.java`

```java
package com.ksa.financing.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String USER_ID_CLAIM = "sub";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String tenantId = extractTenantId(request);
            String userId = extractUserId();

            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            }

            if (userId != null) {
                TenantContextHolder.setUserId(userId);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String extractTenantId(HttpServletRequest request) {
        // First try JWT claim
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String tenantId = jwt.getClaim(TENANT_ID_CLAIM);
            if (tenantId != null) {
                return tenantId;
            }
        }

        // Fallback to header
        return request.getHeader(TENANT_HEADER);
    }

    private String extractUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaim(USER_ID_CLAIM);
        }
        return null;
    }
}
```

---

## 📝 Logging Package Implementation

### 1. Structured Logger

**File**: `src/main/java/com/ksa/financing/infra/logging/StructuredLogger.java`

```java
package com.ksa.financing.infra.logging;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

public class StructuredLogger {

    private final Logger logger;

    private StructuredLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static StructuredLogger getLogger(Class<?> clazz) {
        return new StructuredLogger(clazz);
    }

    public void info(String message, Object... keyValues) {
        if (logger.isInfoEnabled()) {
            logger.info(message, StructuredArguments.entries(asMap(keyValues)));
        }
    }

    public void warn(String message, Object... keyValues) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, StructuredArguments.entries(asMap(keyValues)));
        }
    }

    public void error(String message, Throwable throwable, Object... keyValues) {
        if (logger.isErrorEnabled()) {
            logger.error(message, StructuredArguments.entries(asMap(keyValues)), throwable);
        }
    }

    public void debug(String message, Object... keyValues) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, StructuredArguments.entries(asMap(keyValues)));
        }
    }

    public void withContext(Map<String, String> contextData, Runnable action) {
        contextData.forEach(MDC::put);
        try {
            action.run();
        } finally {
            contextData.keySet().forEach(MDC::remove);
        }
    }

    private Map<String, Object> asMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even");
        }

        Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return map;
    }
}
```

### 2. Correlation ID Filter

**File**: `src/main/java/com/ksa/financing/infra/logging/CorrelationIdFilter.java`

```java
package com.ksa.financing.infra.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
```

### 3. OpenTelemetry Configuration

**File**: `src/main/java/com/ksa/financing/infra/logging/OpenTelemetryConfig.java`

```java
package com.ksa.financing.infra.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Value("${spring.application.name:ksa-financing}")
    private String serviceName;

    @Value("${opentelemetry.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, serviceName,
                ResourceAttributes.SERVICE_VERSION, "1.0.0"
            )));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(
                JaegerGrpcSpanExporter.builder()
                    .setEndpoint(jaegerEndpoint)
                    .build())
                .build())
            .setResource(resource)
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName);
    }
}
```

### 4. MDC Aspect for Tracing

**File**: `src/main/java/com/ksa/financing/infra/logging/TracingMdcAspect.java`

```java
package com.ksa.financing.infra.logging;

import com.ksa.financing.infra.security.TenantContextHolder;
import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TracingMdcAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object addTracingContext(ProceedingJoinPoint joinPoint) throws Throwable {
        Span currentSpan = Span.current();

        try {
            MDC.put("traceId", currentSpan.getSpanContext().getTraceId());
            MDC.put("spanId", currentSpan.getSpanContext().getSpanId());

            String tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                MDC.put("tenantId", tenantId);
            }

            String userId = TenantContextHolder.getUserId();
            if (userId != null) {
                MDC.put("userId", userId);
            }

            return joinPoint.proceed();
        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }
}
```

---

## ⚠️ Exception Handling Package

### 1. Global Exception Handler

**File**: `src/main/java/com/ksa/financing/infra/exception/GlobalExceptionHandler.java`

```java
package com.ksa.financing.infra.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponse> handleTechnicalException(TechnicalException ex, WebRequest request) {
        log.error("Technical error: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .code(ex.getErrorCode())
            .message("An internal error occurred. Please try again later.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error(HttpStatus.FORBIDDEN.getReasonPhrase())
            .code("ACCESS_DENIED")
            .message("You do not have permission to access this resource")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .code("INVALID_CREDENTIALS")
            .message("Invalid username or password")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .code("VALIDATION_ERROR")
            .message("Validation failed")
            .path(request.getDescription(false).replace("uri=", ""))
            .details(validationErrors)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .code("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### 2. Business Exception

**File**: `src/main/java/com/ksa/financing/infra/exception/BusinessException.java`

```java
package com.ksa.financing.infra.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public BusinessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }
}
```

### 3. Technical Exception

**File**: `src/main/java/com/ksa/financing/infra/exception/TechnicalException.java`

```java
package com.ksa.financing.infra.exception;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {

    private final String errorCode;

    public TechnicalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
```

### 4. Not Found Exception

**File**: `src/main/java/com/ksa/financing/infra/exception/NotFoundException.java`

```java
package com.ksa.financing.infra.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

    private final String errorCode;

    public NotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NotFoundException(String entityType, String identifier) {
        super(String.format("%s not found with identifier: %s", entityType, identifier));
        this.errorCode = "NOT_FOUND";
    }
}
```

### 5. Error Response DTO

**File**: `src/main/java/com/ksa/financing/infra/exception/ErrorResponse.java`

```java
package com.ksa.financing.infra.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private String traceId;
    private Map<String, String> details;

    public static class ErrorResponseBuilder {
        public ErrorResponse build() {
            this.traceId = MDC.get("traceId");
            return new ErrorResponse(timestamp, status, error, code, message, path, traceId, details);
        }
    }
}
```

---

## 🔄 Resilience Package Implementation

### 1. Circuit Breaker Configuration

**File**: `src/main/java/com/ksa/financing/infra/resilience/CircuitBreakerConfig.java`

```java
package com.ksa.financing.infra.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slowCallRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(10000))
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .permittedNumberOfCallsInHalfOpenState(3)
            .minimumNumberOfCalls(5)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .build();

        return CircuitBreakerRegistry.of(config);
    }
}
```

### 2. Retry Configuration

**File**: `src/main/java/com/ksa/financing/infra/resilience/RetryConfig.java`

```java
package com.ksa.financing.infra/resilience;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RetryConfiguration {

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(Exception.class)
            .build();

        return RetryRegistry.of(config);
    }
}
```

---

## 🔑 Idempotency Package Implementation

### 1. Idempotency Store Interface

**File**: `src/main/java/com/ksa/financing/infra/idempotency/IdempotencyStore.java`

```java
package com.ksa.financing.infra.idempotency;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface IdempotencyStore {

    <T> T executeIdempotent(String key, Supplier<T> operation);

    <T> T executeIdempotent(String key, Supplier<T> operation, Duration ttl);

    Optional<Object> get(String key);

    void store(String key, Object value);

    void store(String key, Object value, Duration ttl);

    void delete(String key);
}
```

### 2. Redis Idempotency Store Implementation

**File**: `src/main/java/com/ksa/financing/infra/idempotency/RedisIdempotencyStore.java`

```java
package com.ksa.financing.infra.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> T executeIdempotent(String key, Supplier<T> operation) {
        return executeIdempotent(key, operation, DEFAULT_TTL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeIdempotent(String key, Supplier<T> operation, Duration ttl) {
        String fullKey = KEY_PREFIX + key;

        // Check if result exists
        Object cachedResult = redisTemplate.opsForValue().get(fullKey);
        if (cachedResult != null) {
            log.debug("Idempotent operation result found in cache for key: {}", key);
            return (T) cachedResult;
        }

        // Execute operation and store result
        T result = operation.get();
        store(key, result, ttl);
        log.debug("Idempotent operation executed and cached for key: {}", key);

        return result;
    }

    @Override
    public Optional<Object> get(String key) {
        String fullKey = KEY_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(fullKey);
        return Optional.ofNullable(value);
    }

    @Override
    public void store(String key, Object value) {
        store(key, value, DEFAULT_TTL);
    }

    @Override
    public void store(String key, Object value, Duration ttl) {
        String fullKey = KEY_PREFIX + key;
        redisTemplate.opsForValue().set(fullKey, value, ttl);
    }

    @Override
    public void delete(String key) {
        String fullKey = KEY_PREFIX + key;
        redisTemplate.delete(fullKey);
    }
}
```

### 3. Idempotent Annotation

**File**: `src/main/java/com/ksa/financing/infra/idempotency/Idempotent.java`

```java
package com.ksa.financing.infra.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * SpEL expression to generate idempotency key
     */
    String keyExpression();

    /**
     * TTL in seconds (default 24 hours)
     */
    long ttlSeconds() default 86400;
}
```

### 4. Idempotency Aspect

**File**: `src/main/java/com/ksa/financing/infra/idempotency/IdempotencyAspect.java`

```java
package com.ksa.financing.infra.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyStore idempotencyStore;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = generateKey(joinPoint, idempotent.keyExpression());
        Duration ttl = Duration.ofSeconds(idempotent.ttlSeconds());

        return idempotencyStore.executeIdempotent(key, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException("Error executing idempotent operation", e);
            }
        }, ttl);
    }

    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}
```

---

## 📋 Resources Configuration

### 1. Logback Configuration

**File**: `src/main/resources/logback-base.xml`

(Already provided in Prompt 00)

### 2. Application Properties Template

**File**: `src/main/resources/application.yml`

```yaml
# Foundational Infrastructure SDK Configuration Template

spring:
  application:
    name: ${SERVICE_NAME:ksa-financing-service}

  # Redis Configuration
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
      jedis:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

# Keycloak Configuration
keycloak:
  auth-server-url: ${KEYCLOAK_AUTH_SERVER_URL:http://localhost:8080}
  realm: ${KEYCLOAK_REALM:ksa-financing}
  resource: ${SERVICE_NAME:ksa-financing-service}
  bearer-only: true

# OpenTelemetry Configuration
opentelemetry:
  jaeger:
    endpoint: ${JAEGER_ENDPOINT:http://localhost:14250}

# Resilience4j Configuration
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10

  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2

  bulkhead:
    configs:
      default:
        maxConcurrentCalls: 25
        maxWaitDuration: 0

# Logging Configuration
logging:
  level:
    com.ksa.financing: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

---

## 🧪 Test Classes

### 1. Idempotency Store Test

**File**: `src/test/java/com/ksa/financing/infra/idempotency/RedisIdempotencyStoreTest.java`

```java
package com.ksa.financing.infra.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RedisIdempotencyStoreTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.0"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private IdempotencyStore idempotencyStore;

    @Test
    void shouldExecuteOperationOnce() {
        AtomicInteger counter = new AtomicInteger(0);
        String key = "test-key-1";

        String result1 = idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        });

        String result2 = idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        });

        assertThat(result1).isEqualTo("result");
        assertThat(result2).isEqualTo("result");
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldExpireAfterTTL() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        String key = "test-key-2";

        idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        }, Duration.ofSeconds(1));

        Thread.sleep(1100);

        idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        }, Duration.ofSeconds(1));

        assertThat(counter.get()).isEqualTo(2);
    }
}
```

---

## ✅ Success Criteria

After completing this prompt:

- [ ] All security classes compile successfully
- [ ] All logging classes compile successfully
- [ ] All exception handling classes compile successfully
- [ ] All resilience classes compile successfully
- [ ] All idempotency classes compile successfully
- [ ] Unit tests pass
- [ ] Maven install succeeds: `mvn clean install`
- [ ] Artifact exists in local Maven repo: `~/.m2/repository/com/ksa/financing/foundational-infra-sdk/1.0.0-SNAPSHOT/`

---

## 🔄 Next Step

After successful completion, proceed to:
- **Prompt 02**: domain-core-sdk implementation

---

**Execute all code in this prompt to complete foundational-infra-sdk.**
