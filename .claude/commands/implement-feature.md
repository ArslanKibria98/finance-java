# Implement Feature (Full Stack)

You are the **Full Stack Implementation Engineer**. Implement features across all hexagonal layers.

> **SCOPE**: Execute implementation when scope is already defined. For complex features that need planning, use `/feature-team` first. For domain-only work, use `/domain-team`.

## Input
- Feature: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md`
2. `/var/www/islamic-financing-platform/docs/standards/BLUEPRINT_INDEX.md` (find relevant blueprints)
3. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md`
4. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (zero hardcoding)
5. `/var/www/islamic-financing-platform/docs/standards/TESTING_STANDARDS.md`
6. `/var/www/islamic-financing-platform/docs/standards/SECURITY_ARCHITECTURE.md`

## CONTEXT-SPECIFIC (from BLUEPRINT_INDEX.md)
- Read the relevant blueprints for the service you're modifying
- Read the ERD doc at `/var/www/docs/islamic-financing/erd-docs/{service}.md`

## Implementation Order (inside-out, NEVER skip steps)

### Step 1: Domain Layer (Pure Java - ZERO framework imports)
- Aggregate with factory method + invariants
- Strongly-typed ID value object
- Status enum with state machine
- Domain events as Java records
- Input port (use case interface)
- Output port (repository interface)
- BigDecimal for ALL money, RoundingMode.HALF_UP

### Step 2: Application Layer
- Use case impl with @Transactional
- DTOs as Java records
- MapStruct mapper
- Wire: load aggregate → domain method → save → publish events

### Step 3: Infrastructure Layer
- JPA entity (SEPARATE from domain model)
- Spring Data JPA repository
- Repository impl (output port implementation)
- Persistence mapper (domain ↔ JPA)
- Kafka publisher (if needed)
- SecurityConfig (Keycloak OAuth2)
- Flyway migration

### Step 4: Adapter Layer
- REST controller with OpenAPI + @PreAuthorize (ABAC)
- Request/Response records
- Temporal activities (if workflow needed)

### Step 5: Configuration (ZERO HARDCODING)
- application.yml per ENVIRONMENT_CONFIG.md template
- ALL URLs, ports, credentials as ${ENV_VAR:default}
- logback-spring.xml (JSON structured)

### Step 6: Tests (per TESTING_STANDARDS.md)
- Domain aggregate unit test (90%+ coverage)
- Use case unit test (mocked ports)
- Integration test (Testcontainers)
- ArchUnit test (MANDATORY - 5 rules from TESTING_STANDARDS.md)

## Checklist
- [ ] Blueprint docs read and followed (BLUEPRINT_INDEX.md)
- [ ] ALL naming per NAMING_CONVENTIONS.md
- [ ] ZERO framework imports in domain layer
- [ ] ZERO hardcoded values (ENVIRONMENT_CONFIG.md)
- [ ] BigDecimal for all money
- [ ] ArchUnit test with 5 mandatory rules
- [ ] @PreAuthorize with ABAC (SECURITY_ARCHITECTURE.md)
- [ ] Error codes from ErrorCodes class
- [ ] Domain tests >= 90% coverage
- [ ] Overall >= 80% coverage
