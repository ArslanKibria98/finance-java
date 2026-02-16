# Service Template - Hexagonal Architecture Microservice

## Overview

This is a **production-ready microservice template** for the KSA Islamic Financing Platform, implementing **Hexagonal Architecture (Ports & Adapters)** with Domain-Driven Design principles. Use this template as the foundation for all 20 microservices in the platform.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      DRIVING ADAPTERS                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   REST   │  │ Temporal │  │  Kafka   │  │   CLI    │   │
│  │Controller│  │ Activity │  │ Consumer │  │  Admin   │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
└───────┼─────────────┼──────────────┼─────────────┼─────────┘
        │             │              │             │
        ▼             ▼              ▼             ▼
┌─────────────────────────────────────────────────────────────┐
│                      INPUT PORTS                            │
│                  (Use Case Interfaces)                      │
├─────────────────────────────────────────────────────────────┤
│                   APPLICATION LAYER                         │
│                    (Use Cases, DTOs)                        │
├─────────────────────────────────────────────────────────────┤
│                     DOMAIN LAYER                            │
│         (Entities, Aggregates, Services, Events)           │
├─────────────────────────────────────────────────────────────┤
│                      OUTPUT PORTS                           │
│              (Repository, Event Publisher)                  │
└─────────────────────────────────────────────────────────────┘
        │             │              │             │
        ▼             ▼              ▼             ▼
┌───────┼─────────────┼──────────────┼─────────────┼─────────┐
│  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐   │
│  │   JPA    │  │  Kafka   │  │ Fineract │  │   Redis  │   │
│  │Repository│  │Publisher │  │  Adapter │  │   Cache  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│                      DRIVEN ADAPTERS                        │
└─────────────────────────────────────────────────────────────┘
```

## Key Features

### 🎯 Core Architecture Patterns
- **Hexagonal Architecture** - Complete isolation of business logic from infrastructure
- **Domain-Driven Design** - Rich domain models with aggregates and value objects
- **CQRS** - Separated read/write models for scalability
- **Event Sourcing** - Domain events for audit trail and integration

### 🔐 Security & Compliance
- **OAuth2/OIDC** with Keycloak integration
- **Multi-tenancy** support via JWT claims
- **Row-level security** in database
- **Audit logging** for SAMA compliance

### 📊 Observability
- **OpenTelemetry** tracing
- **Prometheus** metrics
- **Structured logging** with correlation IDs
- **Health checks** and readiness probes

### 🚀 Production Ready
- **Spring Boot 4.0.2** with Java 21
- **Virtual Threads** (Project Loom) support
- **GraalVM Native Image** compatible
- **Testcontainers** for integration testing
- **Docker** multi-stage builds
- **Kubernetes** manifests with HPA

## Project Structure

```
service-template/
├── src/
│   ├── main/
│   │   ├── java/com/ksa/financing/service/template/
│   │   │   ├── domain/                 # Core business logic
│   │   │   │   ├── model/             # Aggregates, Entities, VOs
│   │   │   │   ├── service/           # Domain services
│   │   │   │   └── port/              # Port interfaces
│   │   │   │       ├── in/            # Input ports (use cases)
│   │   │   │       └── out/           # Output ports (SPI)
│   │   │   ├── application/           # Application services
│   │   │   │   ├── usecase/          # Use case implementations
│   │   │   │   ├── dto/              # Data transfer objects
│   │   │   │   └── mapper/           # DTO mappers
│   │   │   ├── infrastructure/        # Technical concerns
│   │   │   │   ├── persistence/      # JPA repositories
│   │   │   │   ├── messaging/        # Kafka integration
│   │   │   │   └── config/           # Spring configuration
│   │   │   └── adapter/               # External interfaces
│   │   │       ├── rest/             # REST controllers
│   │   │       ├── temporal/         # Temporal activities
│   │   │       └── client/           # External API clients
│   │   └── resources/
│   │       ├── application.yml        # Main configuration
│   │       ├── application-dev.yml    # Development profile
│   │       ├── application-prod.yml   # Production profile
│   │       └── db/migration/          # Flyway migrations
│   └── test/                           # Test suite
├── k8s/                                # Kubernetes manifests
├── Dockerfile                          # Multi-stage Docker build
└── pom.xml                            # Maven configuration
```

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16
- Apache Kafka
- Temporal.io (optional)

### Local Development

1. **Clone and navigate:**
```bash
cd /var/www/islamic-financing-platform/services/service-template
```

2. **Start infrastructure:**
```bash
docker-compose -f ../../docker-compose.yml up -d postgres kafka
```

3. **Configure environment:**
```bash
cp ../../.env.example .env
# Edit .env with your local settings
```

4. **Build the service:**
```bash
mvn clean compile
```

5. **Run tests:**
```bash
mvn test
```

6. **Run the application:**
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

7. **Access endpoints:**
- API: http://localhost:8081/api/v1/examples
- Swagger UI: http://localhost:8081/swagger-ui.html
- Health: http://localhost:8081/actuator/health

## Usage Examples

### Create an Example Aggregate
```bash
curl -X POST http://localhost:8081/api/v1/examples \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -d '{
    "name": "Test Example",
    "description": "This is a test example"
  }'
```

### Activate an Example
```bash
curl -X POST http://localhost:8081/api/v1/examples/{id}/activate \
  -H "Authorization: Bearer ${JWT_TOKEN}"
```

### Add Entity to Example
```bash
curl -X POST http://localhost:8081/api/v1/examples/{id}/entities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -d '{
    "name": "Entity Name",
    "value": "Entity Value"
  }'
```

## Creating a New Service from Template

### Step 1: Copy Template
```bash
cp -r service-template your-service-name
cd your-service-name
```

### Step 2: Rename Packages
```bash
# Update package names
find . -type f -name "*.java" -exec sed -i 's/service\.template/your\.service/g' {} +
find . -type f -name "*.yml" -exec sed -i 's/service-template/your-service/g' {} +
find . -type f -name "*.xml" -exec sed -i 's/service-template/your-service/g' {} +
```

### Step 3: Update Configuration
1. Edit `pom.xml`:
   - Change `artifactId` to your service name
   - Update `name` and `description`

2. Edit `application.yml`:
   - Change `spring.application.name`
   - Update database name
   - Configure service-specific settings

3. Edit Kubernetes manifests in `k8s/`:
   - Update service name
   - Adjust resource limits
   - Configure environment variables

### Step 4: Implement Domain Model
1. Replace `ExampleAggregate` with your domain aggregate
2. Define your domain entities and value objects
3. Implement domain services for business logic
4. Create domain events for state changes

### Step 5: Define Ports
1. Create input ports (use case interfaces) in `domain/port/in/`
2. Create output ports (SPI) in `domain/port/out/`
3. Keep ports focused and cohesive

### Step 6: Implement Use Cases
1. Implement use cases in `application/usecase/`
2. Orchestrate domain operations
3. Handle transactions and events
4. Map between domain and DTOs

### Step 7: Add Infrastructure Adapters
1. Implement JPA repositories
2. Configure Kafka topics and consumers
3. Add external API clients
4. Implement caching if needed

### Step 8: Create REST Controllers
1. Define REST endpoints
2. Add OpenAPI documentation
3. Implement request validation
4. Handle errors appropriately

## Testing Strategy

### Unit Tests
- Test domain logic in isolation
- Mock external dependencies
- Focus on business rules and invariants

### Integration Tests
- Use Testcontainers for real databases
- Test repository implementations
- Verify transaction boundaries

### API Tests
- Test REST controllers with MockMvc
- Verify request/response contracts
- Test error scenarios

### End-to-End Tests
- Test complete workflows
- Include Temporal activities
- Verify event publishing

## Deployment

### Build Docker Image
```bash
docker build -t ksa-financing/your-service:latest .
```

### Deploy to Kubernetes
```bash
kubectl apply -f k8s/
```

### Configure Istio Service Mesh
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: your-service
spec:
  hosts:
  - your-service
  http:
  - route:
    - destination:
        host: your-service
        subset: v1
```

## Monitoring & Observability

### Metrics
- Exposed at `/actuator/prometheus`
- Custom metrics via Micrometer
- Business metrics in domain services

### Tracing
- OpenTelemetry integration
- Correlation ID propagation
- Distributed trace visualization in Jaeger

### Logging
- Structured JSON logging in production
- MDC for context propagation
- Log aggregation with Loki

## Best Practices

### Domain Layer
✅ Keep domain free of framework dependencies
✅ Use factory methods for aggregate creation
✅ Validate invariants in domain methods
✅ Emit domain events for state changes
✅ Use value objects for concepts with rules

### Application Layer
✅ Keep use cases focused on single responsibility
✅ Handle transactions at use case boundary
✅ Map between domain models and DTOs
✅ Orchestrate domain services

### Infrastructure Layer
✅ Implement ports as Spring components
✅ Handle technical concerns (retry, caching)
✅ Map between domain and persistence models
✅ Configure external integrations

### Testing
✅ Test domain logic without Spring context
✅ Use Testcontainers for integration tests
✅ Mock external services in unit tests
✅ Test error scenarios and edge cases

## Common Customizations

### Add a New Aggregate
1. Create aggregate class in `domain/model/`
2. Define repository port in `domain/port/out/`
3. Implement JPA repository in `infrastructure/persistence/`
4. Create use cases in `application/usecase/`
5. Add REST endpoints in `adapter/rest/`

### Integrate with External API
1. Define client port in `domain/port/out/`
2. Implement client in `infrastructure/client/`
3. Add configuration in `application.yml`
4. Handle errors and retries
5. Add circuit breaker if needed

### Add Kafka Consumer
1. Define event handler in `infrastructure/messaging/`
2. Configure topic in `application.yml`
3. Implement deserialization
4. Handle processing errors
5. Add idempotency checks

### Implement Temporal Workflow
1. Define workflow interface
2. Implement workflow logic
3. Create activities in `adapter/temporal/`
4. Register with worker
5. Add tests with Temporal test framework

## Troubleshooting

### Build Issues
```bash
# Clear Maven cache
mvn clean
rm -rf ~/.m2/repository/com/ksa/financing

# Rebuild with debug output
mvn clean compile -X
```

### Database Connection
```bash
# Check PostgreSQL connection
psql -h localhost -U postgres -d service_template

# Run migrations manually
mvn flyway:migrate
```

### Kafka Issues
```bash
# List topics
kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## Support

For questions and issues:
- Check existing documentation in `/docs`
- Review architecture principles in `/docs/islamic-financing/master-blueprint/`
- Contact the platform team

## License

Copyright © 2024 KSA Islamic Financing Platform. All rights reserved.