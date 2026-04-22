# HyperPay Payment Flow - Implementation Guide (Updated)

**Status**: Collections Service Updated ✅  
**Last Updated**: April 16, 2026  
**Kong Route**: `/lending-service`  
**Collections Service Port**: 8097 (Docker) / 8000 (Kong at 46.62.226.94:8000/lending-service)

---

## Critical Changes (REQUIRED FOR PAYMENT FLOW TO WORK)

### ✅ Database Schema Updated
- **V2 Migration**: Added `installment_id` and `invoice_id` columns to `payments` table
- **Unique Constraint**: `(tenant_id, invoice_id)` prevents duplicate invoices
- **Foreign Key**: `installment_id` links to `installments` table

### ✅ API Request/Response Updated
Added **TWO CRITICAL FIELDS** to payment initiation:

| Field | Type | Source | Purpose |
|-------|------|--------|---------|
| **installmentId** | UUID | GET `/api/v1/loans/{id}/installments` response | CRITICAL: Identifies which installment is being paid |
| **invoiceId** | String | Mobile app generates (unique per payment) | CRITICAL: Audit trail & duplicate prevention |

### ✅ Domain Model Updated
- `PaymentAggregate`: Added `installmentId` and `invoiceId` fields
- `InitiatePaymentCommand`: Added parameters for both fields
- `PaymentInitiated` domain event: Includes both fields for Kafka publishing
- Validation: Both fields are **mandatory** (cannot be null)

### ✅ API Response Updated
Payment responses now include:
```json
{
  "id": "uuid",
  "loanId": "uuid",
  "installmentId": "inst-2026-05-15-001",
  "invoiceId": "INV-54F5B2DD-001",
  "amount": 100.30,
  "paymentMethod": "HYPERPAY_MADA",
  "status": "PENDING",
  "idempotencyKey": "...",
  "createdAt": "2026-05-15T12:00:00Z"
}
```

---

## Kong API Gateway Routing

### Local Development
```
POST http://localhost:8000/lending-service/api/v1/payments
```

### Production (46.62.226.94)
```
POST http://46.62.226.94:8000/lending-service/api/v1/payments
```

### How Kong Routes Work
- **Route Name**: `lending-routes`
- **Route Path**: `/lending-service`
- **Kong Service**: `lending-service` (port 8097)
- **Behavior**: `/lending-service/*` → `http://lending-service:8097/*`

**IMPORTANT**: Do NOT include service name in path. Kong handles routing via route path `/lending-service`, which maps to the service automatically.

---

## Updated Payment Flow (5 Steps)

```
┌────────────────────────────────────────────────────────────────┐
│ STEP 1: Mobile GET /api/v1/loans/{id}/installments            │
│ ────────────────────────────────────────────────────────────── │
│ Response includes:                                              │
│   - installmentId: "inst-2026-05-15-001"  ← SAVE THIS          │
│   - invoiceId: "INV-54F5B2DD-001"         ← SAVE THIS          │
│   - dueDate, totalOutstanding, status...                       │
│ Mobile shows: "Installment #1, Due: May 15, Amount: 100.30"   │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ STEP 2: Mobile POST /api/v1/payments (CRITICAL FIELDS)         │
│ ────────────────────────────────────────────────────────────── │
│ {                                                               │
│   "loanId": "770e8400-e29b-41d4-a716-446655440000",           │
│   "installmentId": "inst-2026-05-15-001",    ← CRITICAL!      │
│   "invoiceId": "INV-54F5B2DD-001",           ← CRITICAL!      │
│   "amount": 100.302973,                                       │
│   "paymentMethod": "HYPERPAY_MADA",                           │
│   "idempotencyKey": "uuid-12345...",                          │
│   "mobileNumber": "966501234567"                              │
│ }                                                               │
│                                                                 │
│ Response (202 Accepted):                                       │
│ {                                                               │
│   "id": "payment-abc123",                                      │
│   "installmentId": "inst-2026-05-15-001",                     │
│   "invoiceId": "INV-54F5B2DD-001",                            │
│   "status": "PENDING",                                         │
│   "workflowId": "payment-abc123",                             │
│   "checkoutUrl": "https://checkout.hyperpay.com/..."         │
│ }                                                               │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ STEP 3: Mobile Opens Checkout URL (HyperPay)                  │
│ ────────────────────────────────────────────────────────────── │
│ - User enters card details                                     │
│ - User confirms OTP                                            │
│ - HyperPay processes transaction                               │
│ - HyperPay calls webhook: POST /webhooks/hyperpay-callback    │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ STEP 4: Backend Temporal Workflow Triggered (Server-to-Server) │
│ ────────────────────────────────────────────────────────────── │
│ LoanRepaymentWorkflow:                                          │
│   1. Validate payment amount vs installment outstanding       │
│   2. Debit customer wallet                                    │
│   3. Apply WATERFALL allocation:                              │
│      - Fee: 10 SAR                                            │
│      - Profit: 50 SAR                                         │
│      - Principal: 40.30 SAR                                   │
│   4. Update installment status: DUE → PAID                    │
│   5. Post GL entries (double-entry accounting)                │
│   6. Update payments table with installmentId & invoiceId     │
│   7. Send SMS: "Payment of 100.30 SAR received for..."        │
│                                                                 │
│ ** installmentId & invoiceId are now used to find             │
│    which installment was paid and link all audit trails **    │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ STEP 5: Mobile Polls Status                                   │
│ ────────────────────────────────────────────────────────────── │
│ GET /api/v1/payments/payment-abc123                           │
│ Poll every 2 seconds until status = COMPLETED or FAILED       │
│                                                                 │
│ Response (COMPLETED):                                          │
│ {                                                               │
│   "id": "payment-abc123",                                      │
│   "installmentId": "inst-2026-05-15-001",                     │
│   "invoiceId": "INV-54F5B2DD-001",                            │
│   "status": "COMPLETED",                                       │
│   "amount": 100.30,                                           │
│   "providerTransactionId": "TXN-999",                         │
│   "appliedAllocations": {                                     │
│     "fees": 10,                                               │
│     "profit": 50,                                             │
│     "principal": 40.30                                        │
│   },                                                           │
│   "completedAt": "2026-05-15T12:05:45Z"                       │
│ }                                                               │
│                                                                 │
│ Mobile shows: ✅ Payment Successful                            │
└────────────────────────────────────────────────────────────────┘
```

---

## Code Changes Summary

### 1. Database (V2 Migration)
```sql
ALTER TABLE payments
ADD COLUMN installment_id UUID REFERENCES installments(id),
ADD COLUMN invoice_id VARCHAR(100);

ALTER TABLE payments
ADD CONSTRAINT uk_payments_tenant_invoice_id UNIQUE (tenant_id, invoice_id);

CREATE INDEX idx_payments_installment ON payments(tenant_id, installment_id);
CREATE INDEX idx_payments_invoice_id ON payments(tenant_id, invoice_id);
```

### 2. API Request (ProcessPaymentRequest)
```java
public record ProcessPaymentRequest(
    @NotNull UUID loanId,
    @NotNull UUID installmentId,          // ← NEW
    @NotBlank String invoiceId,           // ← NEW
    @NotNull @Positive BigDecimal amount,
    @NotNull PaymentMethod paymentMethod,
    @NotBlank String idempotencyKey,
    String providerReference,
    String notes
) {}
```

### 3. API Response (PaymentResponse)
```java
public record PaymentResponse(
    UUID id,
    UUID loanId,
    UUID installmentId,                   // ← NEW
    String invoiceId,                     // ← NEW
    BigDecimal amount,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    String idempotencyKey,
    String providerReference,
    String providerTransactionId,
    String failureCode,
    String failureMessage,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}
```

### 4. Domain Command (InitiatePaymentCommand)
```java
record InitiatePaymentCommand(
    UUID tenantId,
    UUID loanId,
    UUID installmentId,                   // ← NEW
    UUID customerId,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    String invoiceId,                     // ← NEW
    LocalDate valueDate,
    UUID sourceWalletId,
    String idempotencyKey
) {}
```

### 5. Domain Aggregate (PaymentAggregate)
- Added `private final UUID installmentId;`
- Added `private final String invoiceId;`
- Updated factory method `initiate()` to require both
- Updated `PaymentInitiated` event to include both

### 6. JPA Entity (PaymentJpaEntity)
```java
@Column(name = "installment_id")
private UUID installmentId;

@Column(name = "invoice_id", length = 100)
private String invoiceId;
```

### 7. Persistence Mapper (PaymentPersistenceMapper)
- Updated `toJpa()` to map these fields
- Updated `toDomain()` to pass them to `reconstitute()`

---

## Postman Collection Usage

### Updated Collection
File: `/docs/postman/HyperPay-Payment-Flow-Updated.postman_collection.json`

### Environment Variables (Update in Postman)
```
kong_base_url        = http://46.62.226.94:8000/lending-service
jwt_token            = <your_keycloak_jwt>
loan_id              = <loan_uuid>
installment_id       = <from_installments_response>
invoice_id           = INV-<date>-<number>
payment_amount       = <from_installments_response>
idempotency_key      = {{$randomUUID}}
mobile_number        = 966501234567
```

### Workflow
1. **GET Installments** → Copy `installmentId` and `invoiceId`
2. **POST Payment** → Paste them into request body + installmentId header
3. **Poll Status** → Check until COMPLETED
4. **Complete Payment** → Confirm success

---

## Database Schema Update

### Before
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    loan_id UUID,
    amount NUMERIC,
    status VARCHAR,
    ...
);
```

### After (V2)
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    loan_id UUID,
    installment_id UUID REFERENCES installments(id),  -- ← NEW
    invoice_id VARCHAR(100) UNIQUE,                   -- ← NEW
    amount NUMERIC,
    status VARCHAR,
    ...
);

-- Unique constraint: same tenant can't have duplicate invoiceId
CREATE UNIQUE INDEX uk_payments_tenant_invoice_id 
ON payments(tenant_id, invoice_id);
```

---

## Validation Rules

### installmentId Validation
- MUST NOT be null
- MUST exist in `installments` table
- MUST belong to the same `loan_id`
- MUST be linked to an active repayment schedule

### invoiceId Validation
- MUST NOT be blank
- MUST be unique per tenant (no duplicate payments with same invoiceId)
- Recommended format: `INV-{date}-{number}` or `INV-{uuid}`
- Used for audit trail and idempotency detection

---

## Error Scenarios

### Scenario 1: Missing installmentId
**Request**:
```json
{
  "loanId": "loan-123",
  "amount": 100,
  "idempotencyKey": "key-1"
}
```
**Response** (400 Bad Request):
```json
{
  "error": "installmentId is required",
  "message": "Cannot process payment without installment ID"
}
```

### Scenario 2: Duplicate invoiceId
**Request 1**:
```json
{
  "loanId": "loan-123",
  "installmentId": "inst-001",
  "invoiceId": "INV-2026-05-15-001",
  "amount": 100
}
```
**Response**: 202 Accepted, payment created

**Request 2** (same invoiceId):
```json
{
  "loanId": "loan-123",
  "installmentId": "inst-001",
  "invoiceId": "INV-2026-05-15-001",
  "amount": 100
}
```
**Response** (422 Unprocessable Entity):
```json
{
  "error": "COLLECTIONS.PAYMENT.DUPLICATE_INVOICE",
  "message": "Invoice INV-2026-05-15-001 already exists for this tenant"
}
```

### Scenario 3: Installment not found
**Request**:
```json
{
  "loanId": "loan-123",
  "installmentId": "inst-999",  -- doesn't exist
  "invoiceId": "INV-2026-05-15-001",
  "amount": 100
}
```
**Response** (404 Not Found):
```json
{
  "error": "COLLECTIONS.INSTALLMENT.NOT_FOUND",
  "message": "Installment inst-999 not found"
}
```

---

## Database Audit Trail

### Example: Payment for Installment #1

**payments table**:
```
id          | payment-abc123
tenant_id   | 550e8400-e29b-41d4-a716-446655440000
loan_id     | 770e8400-e29b-41d4-a716-446655440000
installment_id | inst-2026-05-15-001          ← TRACKS WHICH INSTALLMENT
invoice_id  | INV-54F5B2DD-001                ← AUDIT TRAIL
amount      | 100.302973
status      | COMPLETED
created_at  | 2026-05-15T12:00:00Z
updated_at  | 2026-05-15T12:05:45Z
```

**payment_allocations table** (Waterfall):
```
payment_id         | payment-abc123
installment_id     | inst-2026-05-15-001
fee_allocated      | 10.00
profit_allocated   | 50.30
principal_allocated| 40.00
total_allocated    | 100.30
```

**installments table** (After payment):
```
id          | inst-2026-05-15-001
status      | PAID (was DUE)
outstanding_amount | 0 (was 100.30)
paid_total  | 100.30
paid_date   | 2026-05-15
```

---

## Summary: Why These Fields Matter

| Field | Impact |
|-------|--------|
| **installmentId** | **CRITICAL**: System MUST know which installment is being paid. Without it, can't apply payment to correct place in schedule, can't calculate DPD, can't update installment status. |
| **invoiceId** | **CRITICAL**: Audit trail & fraud detection. Prevents duplicate charges if request is retried. Links payment to mobile app's intent. Used for reconciliation with payment provider. |

Without these fields, the payment flow **CANNOT WORK** because:
1. System doesn't know which installment received the payment
2. Multiple installments could exist, payments would be applied randomly
3. No audit trail linking payment to the original invoice
4. Duplicate payments can't be detected
5. Reconciliation becomes impossible

---

## Files Modified/Created

| File | Change |
|------|--------|
| `services/collections-service/src/main/resources/db/migration/V2__add_installment_and_invoice_tracking.sql` | NEW: Migration to add columns |
| `services/collections-service/src/main/java/.../domain/port/in/ProcessPaymentUseCase.java` | UPDATED: InitiatePaymentCommand |
| `services/collections-service/src/main/java/.../adapter/rest/request/ProcessPaymentRequest.java` | UPDATED: Added fields |
| `services/collections-service/src/main/java/.../domain/model/PaymentAggregate.java` | UPDATED: Added fields & validation |
| `services/collections-service/src/main/java/.../infrastructure/persistence/entity/PaymentJpaEntity.java` | UPDATED: Added columns |
| `services/collections-service/src/main/java/.../infrastructure/persistence/mapper/PaymentPersistenceMapper.java` | UPDATED: Mapping logic |
| `services/collections-service/src/main/java/.../application/usecase/ProcessPaymentUseCaseImpl.java` | UPDATED: Pass new fields |
| `services/collections-service/src/main/java/.../adapter/rest/response/PaymentResponse.java` | UPDATED: Response fields |
| `services/collections-service/src/main/java/.../adapter/rest/controller/PaymentController.java` | UPDATED: Request mapping |
| `/docs/postman/HyperPay-Payment-Flow-Updated.postman_collection.json` | NEW: Updated collection |

---

## Next Steps

1. **Run Migration**: Flyway will auto-execute V2 on next service startup
2. **Deploy Code**: Deploy updated collections-service
3. **Update Mobile App**: Pass installmentId + invoiceId in payment request
4. **Test Payment Flow**: Use updated Postman collection
5. **Verify Audit Trail**: Check payments + payment_allocations + installments tables

---

**Created**: April 16, 2026  
**Status**: Implementation Complete ✅  
**Testing**: Ready for QA
