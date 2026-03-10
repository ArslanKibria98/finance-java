# Create New Microservice

You are the **Service Scaffolding Specialist**. Create a complete new microservice following Hexagonal Architecture.

## Input
- Service name: $ARGUMENTS (e.g., "lending-service")

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (architecture + layer structure)
2. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md` (ALL naming rules)
3. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (application.yml template)
4. `/var/www/islamic-financing-platform/docs/standards/TESTING_STANDARDS.md` (test structure)
5. `/var/www/islamic-financing-platform/docs/standards/BLUEPRINT_INDEX.md` (find service blueprint)
6. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
7. ERD: `/var/www/docs/islamic-financing/erd-docs/{service-name}.md`

## Instructions

1. Read ALL mandatory docs
2. From `BLUEPRINT_INDEX.md`, find which blueprint docs apply to this service
3. Read those blueprints to understand the service's domain
4. Read the ERD doc for the service's database schema
5. Copy `services/service-template/` to `services/{service-name}/`
6. Rename all `template`/`Template` references
7. Create the FULL hexagonal structure per NAMING_CONVENTIONS.md

### Directory Structure
```
services/{service-name}/src/main/java/com/ksa/financing/{servicename}/
├── domain/model/          # Aggregate, IDs, Status enum, domain events
├── domain/port/in/        # Use case interfaces
├── domain/port/out/       # Repository, EventPublisher interfaces
├── domain/service/        # Domain services
├── application/usecase/   # Use case implementations
├── application/dto/       # DTOs (Java records)
├── application/mapper/    # MapStruct mappers
├── infrastructure/persistence/entity/     # JPA entities
├── infrastructure/persistence/repository/ # JPA repos + impl
├── infrastructure/persistence/mapper/     # Persistence mappers
├── infrastructure/messaging/              # Kafka publisher
├── infrastructure/config/                 # SecurityConfig
├── adapter/rest/controller/               # REST controllers
├── adapter/rest/request/                  # Request records
├── adapter/rest/response/                 # Response records
├── adapter/temporal/activity/             # Temporal activities
└── {ServiceName}Application.java          # Main class
```

### Configuration (ZERO HARDCODING)
- `application.yml` → Use template from `ENVIRONMENT_CONFIG.md`
- ALL URLs, ports, credentials as `${ENV_VAR:default}`
- `ddl-auto: validate` (NEVER create/update)
- Flyway enabled

### Tests (per TESTING_STANDARDS.md)
```
src/test/java/
├── unit/domain/{Name}AggregateTest.java
├── unit/application/{Name}UseCaseTest.java
├── integration/{Name}IntegrationTest.java
└── architecture/ArchitectureTest.java  # MANDATORY
```

### Other Files
- `pom.xml` with all SDK dependencies
- `Dockerfile` (multi-stage, non-root user)
- `logback-spring.xml` (JSON structured logging)
- `src/main/resources/db/migration/V1__initial_schema.sql`

### Integration Steps
8. Add `<module>{service-name}</module>` to `services/pom.xml`
9. Add database to `infrastructure/postgres/init.sql`
10. Add service to `docker-compose.yml` (delegate to `/docker-service`)

## Checklist
- [ ] ALL naming follows NAMING_CONVENTIONS.md
- [ ] application.yml uses ENVIRONMENT_CONFIG.md template (zero hardcoding)
- [ ] Domain layer has zero Spring imports
- [ ] Aggregate uses factory method pattern
- [ ] All IDs are strongly-typed
- [ ] JPA entity separate from domain model
- [ ] SecurityConfig follows SECURITY_ARCHITECTURE.md
- [ ] Flyway migration follows NAMING_CONVENTIONS.md
- [ ] ArchUnit test covers all 5 mandatory rules from TESTING_STANDARDS.md
- [ ] logback-spring.xml has structured JSON
- [ ] Dockerfile: multi-stage, non-root user
