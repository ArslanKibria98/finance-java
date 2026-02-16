# 🎯 Prompt 02: Domain Core SDK Implementation

**Objective**: Implement the `domain-core-sdk` containing pure domain logic for Islamic financing.

**Prerequisites**:
- ✅ Prompt 00: Initial setup complete
- ✅ Prompt 01: Foundational Infrastructure SDK complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding. They contain all the specifications and requirements.

### Master Blueprint Documents
- `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
  - Section: Domain-Driven Design (DDD) - Tactical Patterns
  - Section: Hexagonal Architecture - Domain Layer

- `/var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md`
  - Murabaha calculation formulas
  - Ijara calculation formulas
  - Tawarruq calculation formulas
  - Charity penalty rules (AAOIFI Standard No. 8)
  - Early settlement (Ibra) calculations

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
  - Domain boundaries
  - Aggregate definitions

### ERD Documentation
- `/var/www/docs/islamic-financing/erd-docs/lending-service.md` - Loan entity structure
- `/var/www/docs/islamic-financing/erd-docs/customer-service.md` - Customer entity structure
- `/var/www/docs/islamic-financing/erd-docs/product-service.md` - Product entity structure

### Product Specifications
- `/var/www/docs/islamic-financing/product-specifications/01_MURABAHA_PERSONAL_FINANCING.md`
- `/var/www/docs/islamic-financing/product-specifications/02_TAWARRUQ_CASH_FINANCING.md`
- `/var/www/docs/islamic-financing/product-specifications/03_IJARA_FINANCING.md`

---

## 🎯 Implementation Requirements

### Core Principles
1. **Pure Domain Logic**: NO Spring Framework, NO Jakarta EE, NO infrastructure dependencies
2. **Java 21 Features**: Use records, sealed classes, pattern matching
3. **Immutability**: All value objects must be immutable
4. **Validation**: Use Jakarta Validation API annotations
5. **Money Precision**: Use JSR 354 Money API (Moneta) for all monetary calculations

### What to Implement

#### 1. Value Objects (in `valueobject/` package)
Implement immutable value objects with validation:
- `Money` - Using JSR 354, always SAR currency, 2 decimal places
- `ProfitRate` - Annual percentage with 4 decimal precision, max 50%
- `ContractId`, `LoanId`, `CustomerId`, `ProductId`, `WalletId` - UUID-based identifiers
- `IBAN` - Saudi IBAN format (SA + 22 digits), with checksum validation
- `NationalId` - 10-digit Saudi ID, type indicator (1=citizen, 2=resident)
- `HijriDate` - Using Java's HijrahChronology
- `Tenure`, `Address`

#### 2. Domain Entities (Aggregates in `model/` package)
Implement these aggregate roots following DDD patterns:
- `Loan` - Main aggregate with lifecycle methods (create, approve, reject, disburse, markOverdue)
- `Customer` - Customer aggregate
- `Wallet` - Wallet aggregate
- `Product` - Product aggregate
- Each aggregate must:
  - Maintain business invariants
  - Publish domain events on state changes
  - Have factory methods for creation

#### 3. Sharia Calculation Engine (in `sharia/` package)
Implement calculators as per reference documents:
- `MurabahaCalculator` - Cost + profit calculations, amortization schedule, early settlement
- `IjarahCalculator` - Lease rental calculations
- `TawarruqCalculator` - Three-party commodity Murabaha
- `CharityPenaltyCalculator` - Late payment penalties (must go to charity, not lender profit)
- `EarlySettlementCalculator` - Ibra (rebate) calculations
- `AmortizationScheduleGenerator`

#### 4. Domain Events (in `event/` package)
Implement event records:
- `DomainEvent` - Base interface with eventId, occurredOn, eventType
- `LoanApplicationCreated`, `LoanApproved`, `LoanRejected`, `LoanDisbursed`
- `PaymentReceived`, `PaymentOverdue`
- `CustomerCreated`, `WalletCredited`

#### 5. Ports (Hexagonal Architecture in `port/` package)
- `port/in/` - Input ports (use case interfaces):
  - `CreateLoanUseCase`, `ApproveLoanUseCase`, `DisburseLoanUseCase`, `RecordPaymentUseCase`
  - `CreateCustomerUseCase`, `GetLoanDetailsQuery`

- `port/out/` - Output ports (SPI interfaces):
  - `LoanRepository`, `CustomerRepository`, `WalletRepository`, `ProductRepository`
  - `EventPublisher`, `IdempotencyChecker`

#### 6. Enumerations (in `enums/` package)
- `ShariaStructure` - MURABAHA, IJARA, TAWARRUQ, MUDARABA, DIMINISHING_MUSHARAKA
- `LoanStatus` - All lifecycle states
- `PaymentStatus`, `PaymentMethod`, `CustomerType`, `CustomerStatus`
- `EmploymentType`, `RepaymentFrequency`

---

## 🧪 Testing Requirements

Write comprehensive tests for:
1. **Murabaha Calculator** - Test all calculation scenarios from product specs
2. **Money Value Object** - Test arithmetic operations, precision, currency validation
3. **IBAN Validation** - Test valid/invalid Saudi IBANs with checksum
4. **Loan Aggregate** - Test state transitions and invariant enforcement
5. **Early Settlement** - Test Ibra calculations with different discount percentages

Use AssertJ for fluent assertions.

---

## ✅ Success Criteria

After implementation, verify:
- [ ] All value objects are immutable and validated
- [ ] Sharia calculations match formulas in reference documents
- [ ] Domain entities enforce all business rules
- [ ] NO infrastructure dependencies (Spring, Jakarta EE, databases)
- [ ] All tests pass: `mvn test -pl shared-libraries/domain-core-sdk`
- [ ] Build succeeds: `mvn clean install -pl shared-libraries/domain-core-sdk`
- [ ] Code coverage > 80%

---

## 🔄 Next Step

After completing this SDK, proceed to:
- **Prompt 03**: `messaging-event-sdk` implementation

---

**Note**: This is the core domain layer. Take time to understand Islamic finance principles from the reference documents before implementation.
