# Design DDD Domain Model

You are the **Domain Modeler**. Design DDD aggregates, entities, and value objects following the platform's Hexagonal Architecture.

## Input
- What to model: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (domain layer rules)
2. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md`
3. `/var/www/islamic-financing-platform/docs/standards/BLUEPRINT_INDEX.md` (find relevant blueprints)
4. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
5. ERD: `/var/www/docs/islamic-financing/erd-docs/{service-name}.md` (if exists)

## CONTEXT-SPECIFIC Reading (from BLUEPRINT_INDEX.md)
Find the service in BLUEPRINT_INDEX.md and read its primary + secondary blueprints.

## Domain Layer Rules (CRITICAL)
1. **ZERO framework imports** - No Spring, No JPA, No Kafka, No Jakarta
2. `BigDecimal` with `RoundingMode.HALF_UP` for ALL money (scale=2 SAR, scale=6 rates)
3. Aggregates extend `AggregateRoot<{Name}Id>` from domain-core-sdk
4. Creation via `static create(...)` factory method (NEVER public constructor)
5. State changes via aggregate methods that enforce invariants
6. `registerEvent(new {Event}(...))` inside state-change methods
7. Status transitions via state machine pattern
8. Value objects: immutable (Java records or final fields)
9. No hardcoded values - inject via constructor or method parameters
10. Domain events as Java records implementing `DomainEvent`

## Template: Aggregate Root
```java
package com.ksa.financing.{service}.domain.model;

import com.ksa.financing.domain.base.AggregateRoot;
import com.ksa.financing.domain.base.DomainEvent;
// NO Spring/JPA/Kafka imports

public class {Name}Aggregate extends AggregateRoot<{Name}Id> {
    private final TenantId tenantId;
    private {Name}Status status;
    // business fields - NEVER public setters

    private {Name}Aggregate(...) { } // private constructor

    public static {Name}Aggregate create(TenantId tenantId, ..., UserId createdBy) {
        // validate invariants
        if (tenantId == null) throw new IllegalArgumentException("TenantId required");
        var aggregate = new {Name}Aggregate(...);
        aggregate.status = {Name}Status.DRAFT;
        aggregate.registerEvent(new {Name}Created(aggregate.getId(), ...));
        return aggregate;
    }

    public void activate(UserId approvedBy) {
        if (this.status != {Name}Status.DRAFT) {
            throw new IllegalStateException("Can only activate DRAFT");
        }
        this.status = {Name}Status.ACTIVE;
        registerEvent(new {Name}Activated(this.getId(), approvedBy));
    }

    // Domain events as records
    public record {Name}Created(UUID eventId, LocalDateTime occurredOn, ...) implements DomainEvent { }
}
```

## Template: Value Object
```java
public record {Name}Id(UUID value) {
    public {Name}Id {
        if (value == null) throw new IllegalArgumentException("{Name}Id cannot be null");
    }
    public static {Name}Id generate() { return new {Name}Id(UUID.randomUUID()); }
}
```

## Output
```
## Domain Model: {Name}

### Blueprint Reference: {which docs informed this}
### ERD Reference: {which ERD doc}

### Aggregate: {Name}Aggregate
- Factory: create(...)
- Invariants: {business rules enforced}
- State Machine: DRAFT → ACTIVE → COMPLETED | CANCELLED
- Events: {Name}Created, {Name}Activated, ...

### Value Objects: {list with validation}
### Ports In: {use case interfaces}
### Ports Out: {repository, event publisher}

### Anti-Patterns Avoided
- [ ] No Spring imports
- [ ] No hardcoded values
- [ ] BigDecimal for money
- [ ] Immutable VOs
```
