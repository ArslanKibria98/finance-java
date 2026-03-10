# Write Tests

You are the **Test Engineer**. Write tests following the platform's testing standards.

## Input
- What to test: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (testing section)
2. `/var/www/islamic-financing-platform/docs/standards/TESTING_STANDARDS.md` (SINGLE SOURCE for all test rules)
3. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md` (test naming)

## Follow TESTING_STANDARDS.md for EVERYTHING:
- Test directory structure
- Coverage targets (domain 90%, application 80%, overall 80%)
- ArchUnit mandatory rules (5 rules)
- Test naming: `should_{expected}_when_{condition}`
- Financial calculation test rules
- Library usage (AssertJ, Mockito, Testcontainers)

## Quick Templates

### Domain Aggregate Test
```java
class {Name}AggregateTest {
    @Test void should_create_with_valid_data() {
        var agg = {Name}Aggregate.create(tenantId, ...);
        assertThat(agg.getId()).isNotNull();
        assertThat(agg.getStatus()).isEqualTo({Name}Status.DRAFT);
        assertThat(agg.getUncommittedEvents()).hasSize(1);
    }

    @Test void should_reject_null_tenant() {
        assertThatThrownBy(() -> {Name}Aggregate.create(null, ...))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void should_enforce_state_transitions() {
        var agg = createDraft();
        assertThatThrownBy(() -> agg.complete(userId))
            .isInstanceOf(IllegalStateException.class);
    }
}
```

### Use Case Test (Mocked)
```java
@ExtendWith(MockitoExtension.class)
class {Name}UseCaseTest {
    @Mock private {Name}Repository repository;
    @Mock private EventPublisher eventPublisher;
    @InjectMocks private {Verb}{Name}UseCaseImpl useCase;

    @Test void should_create_and_persist() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = useCase.create(command);
        verify(repository).save(any());
        verify(eventPublisher).publishAll(anyList());
    }
}
```

### ArchUnit Test (MANDATORY - from TESTING_STANDARDS.md)
```java
@AnalyzeClasses(packages = "com.ksa.financing.{service}")
class ArchitectureTest {
    @ArchTest static final ArchRule domain_not_depend_on_infra = ...
    @ArchTest static final ArchRule domain_not_use_spring = ...
    @ArchTest static final ArchRule domain_not_use_jpa = ...
    @ArchTest static final ArchRule controllers_not_access_repos = ...
    @ArchTest static final ArchRule application_not_depend_on_adapters = ...
}
```

### Integration Test
```java
@SpringBootTest
@Testcontainers
class {Name}IntegrationTest {
    @Container
    static PostgresContainer postgres = PostgresContainer.create("{service}_db");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

## Rules
- AssertJ for assertions (NEVER JUnit assertions)
- Mockito for mocking
- Testcontainers for integration (real PostgreSQL)
- Test naming: `should_{expected}_when_{condition}`
- ArchUnit test is NON-NEGOTIABLE per service
- 90%+ domain coverage, 80%+ overall
