# 🧪 Prompt 08: Test Harness SDK Implementation

**Objective**: Implement the `test-harness-sdk` for testing utilities, fixtures, and test containers.

**Prerequisites**:
- ✅ Prompt 00-07 complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
  - Section: Testing tools

- `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
  - Section: Testability

---

## 🎯 Implementation Requirements

### Technologies
- **JUnit 5**: 5.11.4
- **Testcontainers**: 1.20.4
- **AssertJ**: 3.27.3
- **Mockito**: 5.15.2
- **WireMock**: 3.10.0
- **Temporal Test SDK**: 1.32.1

### What to Implement

#### 1. Test Containers (in `container/`)
Reusable Testcontainers wrappers:
- `PostgresTestContainer` - PostgreSQL 18.1
- `RedisTestContainer` - Redis 8.0
- `KafkaTestContainer` - Kafka 3.9.1 with Schema Registry
- `TemporalTestContainer` - Temporal server
- `FineractTestContainer` - Fineract mock server (WireMock)
- `KeycloakTestContainer` - Keycloak 26.5.2

Usage:
```java
@Testcontainers
class MyIntegrationTest {
    @Container
    static PostgresTestContainer postgres = new PostgresTestContainer();
}
```

#### 2. Test Fixtures (in `fixture/`)
Domain object builders:
- `LoanFixture` - Create test loans
  - `aLoan()` → Default loan
  - `anApprovedLoan()` → Approved loan
  - `anOverdueLoan()` → Overdue loan
- `CustomerFixture` - Create test customers
- `PaymentFixture` - Create test payments
- `MoneyFixture` - Money amounts

Use builder pattern:
```java
Loan loan = LoanFixture.aLoan()
    .withAmount(Money.sar(10000))
    .withTenure(12)
    .build();
```

#### 3. Temporal Test Utilities (in `temporal/`)
- `TemporalTestEnvironment` - In-memory Temporal for tests
- `WorkflowTestHelper` - Execute workflows synchronously
- `ActivityMockBuilder` - Mock activity implementations
- `WorkflowAssertions` - AssertJ-style workflow assertions

#### 4. Adapter Test Doubles (in `adapter/`)
Test doubles for external systems:
- `FineractMockServer` - WireMock-based Fineract API
- `ZatcaMockServer` - Mock ZATCA e-invoicing API
- `SimahMockServer` - Mock Simah credit bureau API
- `NafathMockServer` - Mock Nafath authentication

#### 5. Assertion Utilities (in `assertion/`)
Custom AssertJ assertions:
- `LoanAssert` - Fluent assertions for Loan
  - `assertThat(loan).isApproved()`
  - `assertThat(loan).hasAmount(Money.sar(10000))`
- `MoneyAssert` - Assertions for Money
- `DomainEventAssert` - Assertions for domain events

#### 6. Test Base Classes (in `base/`)
- `IntegrationTest` - Base class for integration tests
  - Starts all required containers
  - Configures Spring context
  - Cleans up after each test
- `UnitTest` - Base class for unit tests
- `TemporalWorkflowTest` - Base for workflow tests
- `RestControllerTest` - Base for REST API tests

---

## 🧪 Example Usage

```java
@IntegrationTest
class LoanServiceTest {

    @Autowired
    private LoanService loanService;

    @Test
    void shouldCreateLoan() {
        // Given
        Loan loan = LoanFixture.aLoan().build();

        // When
        LoanId loanId = loanService.createLoan(loan);

        // Then
        assertThat(loanService.findById(loanId))
            .isPresent()
            .get()
            .hasStatus(LoanStatus.PENDING_REVIEW);
    }
}
```

---

## ✅ Success Criteria

- [ ] All test containers start successfully
- [ ] Fixtures provide realistic test data
- [ ] Temporal test environment works for workflow tests
- [ ] Mock servers respond to API calls
- [ ] Custom assertions provide clear failure messages
- [ ] Integration tests use containers, not mocks
- [ ] Tests are fast (< 5 seconds for unit, < 30 seconds for integration)
- [ ] Tests pass: `mvn test -pl shared-libraries/test-harness-sdk`
- [ ] Build succeeds: `mvn clean install -pl shared-libraries/test-harness-sdk`

---

## 🔄 Next Step

After completing all 8 SDKs, proceed to:
- **Prompt 09**: Service template implementation
- Then individual microservice prompts

---

**This SDK enables comprehensive testing across the platform. Invest time in making tests easy to write.**
