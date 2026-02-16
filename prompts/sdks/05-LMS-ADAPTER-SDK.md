# 🏦 Prompt 05: LMS Adapter SDK Implementation

**Objective**: Implement the `lms-adapter-sdk` to abstract Core Banking System (Apache Fineract) integration.

**Prerequisites**:
- ✅ Prompt 00-04 complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
  - Section: Hexagonal Architecture - Adapter Pattern

- `/var/www/docs/islamic-financing/master-blueprint/07_CORE_BANKING_ADAPTER.md`
  - Fineract API integration
  - Intent-based abstraction layer
  - Ledger synchronization patterns

- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
  - Apache Fineract 1.13.0

---

## 🎯 Implementation Requirements

### Technologies
- **Apache Fineract**: 1.13.0
- **Fineract Client**: Use REST API (not SDK)

### Core Principle
**Intent-based abstraction**: Services express **what** they want (intent), adapter determines **how** (Fineract API calls).

### What to Implement

#### 1. Port Interface (in `port/`)
Define high-level intents (NO Fineract-specific details):
- `LmsPort` - Main interface
  - `createLoanAccount(LoanIntent)` → `LoanAccountId`
  - `approveLoan(ApprovalIntent)` → `ApprovalResult`
  - `disburseLoan(DisbursementIntent)` → `DisbursementResult`
  - `recordRepayment(RepaymentIntent)` → `RepaymentResult`
  - `getLoanDetails(LoanAccountId)` → `LoanDetails`
  - `getRepaymentSchedule(LoanAccountId)` → `List<Installment>`

#### 2. Fineract Adapter (in `fineract/`)
- `FineractLmsAdapter` - Implements `LmsPort`
- `FineractClient` - REST client for Fineract API
- `FineractMapper` - Map domain objects ↔ Fineract DTOs
- `FineractErrorHandler` - Handle Fineract-specific errors

API endpoints to integrate:
- `POST /loans` - Create loan
- `POST /loans/{id}?command=approve` - Approve loan
- `POST /loans/{id}?command=disburse` - Disburse loan
- `POST /loans/{id}/transactions?command=repayment` - Record payment
- `GET /loans/{id}` - Get loan details
- `GET /loans/{id}?associations=repaymentSchedule` - Get schedule

#### 3. Intent Objects (in `intent/`)
Domain-focused request objects:
- `LoanIntent` - What loan to create (amount, tenure, product, customer)
- `ApprovalIntent` - Approval decision with approver info
- `DisbursementIntent` - Disbursement details
- `RepaymentIntent` - Payment details

#### 4. Ledger Abstraction (in `ledger/`)
- `LedgerPort` - Interface for GL operations
  - `createJournalEntry(JournalIntent)`
  - `getAccountBalance(GlAccount)`
- `FineractLedgerAdapter` - Fineract accounting integration
- `ChartOfAccounts` - Standard KSA COA structure

#### 5. Configuration (in `config/`)
- `FineractConfig` - Base URL, credentials, timeout
- `FineractAuthInterceptor` - Basic auth for Fineract API
- `FineractRestTemplate` - Configured REST template

#### 6. Exception Handling (in `exception/`)
- `FineractException` - Base exception
- `FineractConnectionException` - Network failures
- `FineractValidationException` - Business rule violations from Fineract

---

## 🧪 Testing Requirements

- Mock Fineract API using WireMock
- Test all CRUD operations
- Test error scenarios (Fineract down, validation errors)
- Test mapping between domain ↔ Fineract DTOs

---

## ✅ Success Criteria

- [ ] All intents successfully map to Fineract API calls
- [ ] Loan creation, approval, disbursement, repayment work end-to-end
- [ ] Adapter is pluggable (can swap Fineract for another LMS)
- [ ] NO Fineract-specific objects leak to domain layer
- [ ] Error handling converts Fineract errors to domain exceptions
- [ ] Tests pass: `mvn test -pl shared-libraries/lms-adapter-sdk`
- [ ] Build succeeds: `mvn clean install -pl shared-libraries/lms-adapter-sdk`

---

## 🔄 Next Step

After completing this SDK, proceed to:
- **Prompt 06**: `compliance-localization-sdk` implementation
