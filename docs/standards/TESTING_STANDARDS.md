# Testing Standards - Single Source of Truth

> ALL agents writing tests MUST follow these standards. No exceptions.

## Test Pyramid

```
        ╱╲  Architecture (ArchUnit) - MANDATORY, 100% rules pass
       ╱──╲
      ╱ E2E ╲  Temporal workflow tests, full flow tests
     ╱────────╲
    ╱Integration╲  @SpringBootTest + Testcontainers
   ╱──────────────╲
  ╱   Unit Tests    ╲  Domain (pure Java) + Application (mocked ports)
 ╱────────────────────╲
```

## Coverage Targets

| Layer | Target | Blocker |
|-------|--------|---------|
| Domain (aggregates, VOs, services) | >= 90% | YES |
| Application (use cases) | >= 80% | YES |
| Infrastructure (repos, publishers) | >= 70% | NO |
| Adapter (controllers, activities) | >= 70% | NO |
| Overall service | >= 80% | YES |
| Architecture rules | 100% pass | YES (non-negotiable) |

## Test Directory Structure
```
src/test/java/com/ksa/financing/{service}/
├── unit/
│   ├── domain/
│   │   ├── {Name}AggregateTest.java       # Factory, invariants, state transitions, events
│   │   ├── {Name}DomainServiceTest.java    # Cross-aggregate logic
│   │   └── {Name}ValueObjectTest.java      # VO validation, equality
│   └── application/
│       └── {Verb}{Name}UseCaseTest.java    # Mocked ports, use case logic
├── integration/
│   ├── persistence/
│   │   └── {Name}RepositoryTest.java       # @DataJpaTest + Testcontainers
│   ├── rest/
│   │   └── {Name}ControllerTest.java       # @WebMvcTest + MockMvc
│   ├── messaging/
│   │   └── {Name}KafkaTest.java            # Embedded Kafka
│   └── temporal/
│       └── {Name}WorkflowTest.java         # Temporal test environment
└── architecture/
    └── ArchitectureTest.java               # ArchUnit (MANDATORY)
```

## Test Naming Convention
```
should_{expected_behavior}_when_{condition}
```
Examples:
- `should_create_loan_when_valid_data_provided`
- `should_reject_loan_when_amount_exceeds_limit`
- `should_emit_disbursed_event_when_loan_disbursed`
- `should_throw_not_found_when_loan_does_not_exist`

## ArchUnit Rules (MANDATORY per service)
```java
@AnalyzeClasses(packages = "com.ksa.financing.{service}")
class ArchitectureTest {
    // Rule 1: Domain MUST NOT depend on infrastructure
    @ArchTest static final ArchRule domain_not_depend_on_infra = ...
    // Rule 2: Domain MUST NOT use Spring
    @ArchTest static final ArchRule domain_not_use_spring = ...
    // Rule 3: Domain MUST NOT use JPA
    @ArchTest static final ArchRule domain_not_use_jpa = ...
    // Rule 4: Controllers MUST NOT access repositories directly
    @ArchTest static final ArchRule controllers_not_access_repos = ...
    // Rule 5: Application layer MUST NOT depend on adapters
    @ArchTest static final ArchRule application_not_depend_on_adapters = ...
}
```

## Financial Calculation Test Rules
```
1. ALL money calculations use BigDecimal (assert no double/float)
2. ALL rounding uses RoundingMode.HALF_UP
3. Scale = 2 for SAR amounts, scale = 6 for profit rates
4. Sum of installments == total selling price (±0.01 SAR tolerance for rounding)
5. Murabaha profit never exceeds configured SAMA cap
6. Ibra waiver correctly zeroes remaining profit
7. Late payment penalty routes to charity fund (not income)
```

## Testing Libraries
| Library | Version | SDK | Usage |
|---------|---------|-----|-------|
| JUnit 5 | 5.11.4 | `test-harness-sdk` | Test framework |
| Mockito | 5.15.2 | `test-harness-sdk` | Mocking |
| AssertJ | 3.27.3 | `test-harness-sdk` | Fluent assertions (ALWAYS use this, not JUnit) |
| TestContainers | 1.20.4 | `test-harness-sdk` | PostgreSQL, Kafka, Redis containers |
| ArchUnit | 1.2.1 | `test-harness-sdk` | Architecture enforcement |
| Rest Assured | Latest | `test-harness-sdk` | REST API testing |
| TemporalTestEnvironment | Custom | `test-harness-sdk` | Temporal workflow testing |
