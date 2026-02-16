# LMS Adapter SDK

## Overview

The LMS Adapter SDK provides a pluggable abstraction layer for Core Banking System (Apache Fineract) integration. It implements the Hexagonal Architecture pattern to ensure the domain layer remains independent of infrastructure concerns.

## Key Features

- **Intent-based abstraction**: Services express WHAT they want (intent), adapter determines HOW (Fineract API calls)
- **Pluggable architecture**: Easily swap Fineract for another LMS without changing domain code
- **Islamic finance support**: Built-in support for Tawarruq, Murabaha, and Ibra calculations
- **Idempotency**: Prevents duplicate transactions with distributed locking
- **Resilience**: Circuit breakers, retries, and timeout handling
- **Audit trail**: Complete transaction history and journal entries

## Architecture

```
Domain Layer (Intents) → Port Interface → Adapter Implementation → Fineract API
```

### Key Components

1. **Port Interfaces**
   - `LmsPort`: Main interface for loan management operations
   - `LedgerPort`: Interface for general ledger operations

2. **Intent Objects**
   - `LoanIntent`: Domain request for loan creation
   - `ApprovalIntent`: Loan approval decision
   - `DisbursementIntent`: Disbursement details
   - `RepaymentIntent`: Payment recording
   - `SettlementIntent`: Early settlement with Ibra

3. **Fineract Adapter**
   - `FineractLmsAdapter`: Main adapter implementation
   - `FineractClient`: REST client for Fineract API
   - `FineractMapper`: Maps domain ↔ Fineract DTOs
   - `IdempotencyStore`: Ensures idempotent operations

## Integration Points

### Fineract API Endpoints

- `POST /loans` - Create loan
- `POST /loans/{id}?command=approve` - Approve loan
- `POST /loans/{id}?command=disburse` - Disburse loan
- `POST /loans/{id}/transactions?command=repayment` - Record payment
- `GET /loans/{id}` - Get loan details
- `GET /loans/{id}?associations=repaymentSchedule` - Get schedule

## Configuration

```yaml
fineract:
  baseUrl: https://localhost:8443/fineract-provider/api/v1
  username: mifos
  password: password
  tenantId: default
  connectTimeoutMs: 5000
  readTimeoutMs: 30000
```

## Usage Example

```java
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LmsPort lmsPort;

    public void createLoan(LoanApplication application) {
        // Create intent from domain objects
        LoanIntent intent = LoanIntent.builder()
            .customerId(application.getCustomerId())
            .principalAmount(application.getAmount())
            .profitAmount(calculateProfit(application))
            .shariaStructure("TAWARRUQ")
            .commodityId(selectCommodity())
            .build();

        // Adapter handles Fineract integration
        LoanAccountId accountId = lmsPort.createLoanAccount(intent);
    }
}
```

## Islamic Finance Features

### Sharia Structures
- **Tawarruq**: Commodity-based financing with purchase and sale
- **Murabaha**: Cost-plus financing with disclosed profit margin
- **Ibra**: Profit waiver for early settlement

### Compliance
- Profit calculation separate from Fineract interest
- Late fees directed to charity account
- Commodity tracking for Tawarruq transactions
- Sharia board approval tracking

## Testing

The SDK includes comprehensive tests using WireMock to simulate Fineract responses:

```bash
mvn test -pl shared-libraries/lms-adapter-sdk
```

## Dependencies

- Spring Boot 3.2.2
- Apache Fineract 1.13.0 (via REST API)
- Resilience4j for circuit breaking
- Redisson for distributed locking
- WireMock for testing

## Build

```bash
mvn clean install -pl shared-libraries/lms-adapter-sdk
```

## Success Criteria

- ✅ All intents successfully map to Fineract API calls
- ✅ Loan creation, approval, disbursement, repayment work end-to-end
- ✅ Adapter is pluggable (can swap Fineract for another LMS)
- ✅ NO Fineract-specific objects leak to domain layer
- ✅ Error handling converts Fineract errors to domain exceptions
- ✅ Tests pass with WireMock mocking
- ✅ Build succeeds