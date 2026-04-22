# Invoice & Installment ID Mapping

**Critical Reference for Payment Processing**

---

## 📋 Overview

When a customer makes a payment on mobile, the system MUST know **EXACTLY which installment** is being paid. This is tracked via:

- **`installmentId`** - Links payment to specific installment in repayment schedule
- **`invoiceId`** - Unique invoice number for audit trail & duplicate prevention

---

## 🎯 Real-World Example

### Loan Details
```
Loan ID: loan-12345
Product: Murabaha
Principal: 50,000 SAR
Tenure: 12 months (12 installments)
Monthly Installment: ~4,500 SAR
```

### Repayment Schedule

```
┌────────────┬──────────────┬──────────┬─────────┬──────────┐
│ Install #  │ Due Date     │ Status   │ Amount  │ ID       │
├────────────┼──────────────┼──────────┼─────────┼──────────┤
│ 1          │ 2026-05-15   │ DUE      │ 4500    │ inst-001 │
│ 2          │ 2026-06-15   │ SCHEDULED│ 4500    │ inst-002 │
│ 3          │ 2026-07-15   │ SCHEDULED│ 4500    │ inst-003 │
│ ...        │ ...          │ ...      │ ...     │ ...      │
│ 12         │ 2026-04-15   │ SCHEDULED│ 4500    │ inst-012 │
└────────────┴──────────────┴──────────┴─────────┴──────────┘
```

---

## 🔄 Payment Flow with IDs

### Step 1: Mobile Shows Installments to Customer

```
GET /api/v1/loans/loan-12345/installments
Authorization: Bearer {{jwt}}

Response:
{
  "loanId": "loan-12345",
  "installments": [
    {
      "installmentId": "inst-001",          ← Store this
      "installmentNumber": 1,
      "dueDate": "2026-05-15",
      "status": "DUE",
      "outstandingFee": 100,
      "outstandingProfit": 1500,
      "outstandingPrincipal": 2900,
      "totalOutstanding": 4500,
      "dpd": 0
    },
    {
      "installmentId": "inst-002",
      "installmentNumber": 2,
      "dueDate": "2026-06-15",
      "status": "SCHEDULED",
      "totalOutstanding": 4500,
      ...
    },
    ...
  ]
}
```

### Step 2: Customer Clicks "Pay" on Installment #1

Mobile app knows:
- Which installment: `inst-001`
- Which loan: `loan-12345`
- Amount: `4500` SAR
- Payment method: `HYPERPAY_MADA`

### Step 3: Mobile Generates Unique Invoice ID

```typescript
// Mobile generates invoice ID (unique per payment attempt)
const invoiceId = `inv-${loanId}-${installmentId}-${Date.now()}`;
// Example: inv-loan-12345-inst-001-1715765400000

// OR use server timestamp
const invoiceId = `inv-2026-05-15-${uniqueNumber}`;
// Example: inv-2026-05-15-001
```

### Step 4: Mobile Sends Payment Request

```bash
POST /api/v1/payments
Authorization: Bearer {{jwt}}
X-Idempotency-Key: {{unique_key}}

{
  "loanId": "loan-12345",
  "installmentId": "inst-001",
  "invoiceId": "inv-2026-05-15-001",
  "amount": 4500,
  "paymentMethod": "HYPERPAY_MADA",
  "idempotencyKey": "{{unique_key}}",
  "mobileNumber": "966501234567"
}
```

### Step 5: Backend Validates

```java
// Collections-Service validates:
1. Loan exists: SELECT * FROM loans WHERE id = 'loan-12345'
2. Installment exists: SELECT * FROM installments WHERE id = 'inst-001'
3. Installment belongs to loan: ...WHERE id = 'inst-001' AND loanId = 'loan-12345'
4. Amount matches: 4500 == outstanding amount? ✅
5. Invoice unique: SELECT * FROM invoices WHERE invoiceId = 'inv-2026-05-15-001'
   → Must be first time (not duplicate)
```

### Step 6: Temporal Workflow Processes

```
Workflow Input:
{
  paymentId: "payment-abc123",
  loanId: "loan-12345",
  installmentId: "inst-001",
  invoiceId: "inv-2026-05-15-001",
  amount: 4500,
  ...
}

Processing:
1. Debit wallet
2. Apply waterfall allocation to installment inst-001:
   ├─ Fee: 100 SAR
   ├─ Profit: 1500 SAR
   └─ Principal: 2900 SAR
3. Update installment status: SCHEDULED → PAID
4. Post GL entries:
   ├─ DEBIT: Customer Receivable (loan account)
   └─ CREDIT: Loan Revenue Account
5. Send SMS to customer
6. Mark invoice as paid
```

### Step 7: Installment Updated

```sql
-- Before payment
SELECT * FROM installments WHERE id = 'inst-001';
┌──────────────────────────────────────────────┐
│ id: inst-001                                 │
│ status: DUE                                  │
│ outstanding_fee: 100                         │
│ outstanding_profit: 1500                     │
│ outstanding_principal: 2900                  │
│ outstanding_total: 4500                      │
└──────────────────────────────────────────────┘

-- After payment
SELECT * FROM installments WHERE id = 'inst-001';
┌──────────────────────────────────────────────┐
│ id: inst-001                                 │
│ status: PAID ✅                              │
│ outstanding_fee: 0                           │
│ outstanding_profit: 0                        │
│ outstanding_principal: 0                     │
│ outstanding_total: 0                         │
│ paid_fee: 100                                │
│ paid_profit: 1500                            │
│ paid_principal: 2900                         │
│ paid_total: 4500                             │
└──────────────────────────────────────────────┘
```

---

## 🚨 Common Mistakes

### ❌ Mistake 1: NOT Sending Installment ID

```bash
# WRONG - System doesn't know which installment!
POST /api/v1/payments
{
  "loanId": "loan-12345",
  "amount": 4500,
  "paymentMethod": "HYPERPAY_MADA"
}
```

**Problems**:
- System could apply payment to wrong installment
- If multiple installments due, no way to know which one
- Schedule gets corrupted

### ❌ Mistake 2: Hardcoding Invoice ID

```typescript
// WRONG - Same ID every time!
const invoiceId = "inv-2026-05-15-001";
```

**Problems**:
- Second payment attempt gets rejected (duplicate)
- Can't track multiple payments for same installment
- Audit trail breaks

### ❌ Mistake 3: Using Wrong Amount

```bash
# WRONG - Doesn't match installment outstanding!
POST /api/v1/payments
{
  "loanId": "loan-12345",
  "installmentId": "inst-001",
  "amount": 5000,  # But installment is 4500!
  ...
}
```

**Problems**:
- Payment gets rejected (amount mismatch)
- If overpayment allowed: extra amount goes to next installment
- Confusion in accounting

---

## ✅ CORRECT Implementation

### Mobile App Checklist

```typescript
// 1. Fetch installments
const installments = await fetchInstallments(loanId);

// 2. Show to customer
// Display: Installment #1, Due: May 15, Amount: 4500 SAR, Status: DUE

// 3. User clicks "Pay"
const selectedInstallment = installments[0];

// 4. Generate UNIQUE invoice ID
const invoiceId = `inv-${new Date().toISOString().split('T')[0]}-${uuid()}`;
// inv-2026-05-15-a1b2c3d4...

// 5. Send payment
const response = await fetch('https://api.kfs.com/api/v1/payments', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'X-Idempotency-Key': uuid()
  },
  body: JSON.stringify({
    loanId: loanId,
    installmentId: selectedInstallment.installmentId,  // ✅ Critical!
    invoiceId: invoiceId,                              // ✅ Unique!
    amount: selectedInstallment.totalOutstanding,      // ✅ Exact amount
    paymentMethod: 'HYPERPAY_MADA'
  })
});

// 6. Handle response
const data = await response.json();
const workflowId = data.workflowId;

// 7. Redirect to HyperPay
openCheckout(data.checkoutUrl);

// 8. Poll status
startPolling(workflowId);
```

---

## 📊 Database Schema

### invoices Table

```sql
CREATE TABLE invoices (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL,
  loan_id UUID NOT NULL,
  installment_id UUID NOT NULL,
  invoice_id VARCHAR(100) NOT NULL UNIQUE,  -- inv-2026-05-15-001
  
  amount NUMERIC(18,6) NOT NULL,
  status INVOICE_STATUS NOT NULL,  -- PENDING, PAID, CANCELLED
  
  payment_id UUID,  -- Link to payment
  paid_at TIMESTAMPTZ,
  
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  FOREIGN KEY (loan_id) REFERENCES loans(id),
  FOREIGN KEY (installment_id) REFERENCES installments(id),
  FOREIGN KEY (payment_id) REFERENCES payments(id),
  
  CONSTRAINT unique_invoice_per_installment 
    UNIQUE(tenant_id, loan_id, installment_id)
);

CREATE INDEX idx_invoices_loan_id ON invoices(tenant_id, loan_id);
CREATE INDEX idx_invoices_installment_id ON invoices(installment_id);
CREATE INDEX idx_invoices_invoice_id ON invoices(invoice_id);
```

### payments Table (Updated)

```sql
CREATE TABLE payments (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL,
  
  loan_id UUID NOT NULL,
  installment_id UUID NOT NULL,  -- ✅ Now includes installment
  invoice_id VARCHAR(100) NOT NULL,   -- ✅ Now includes invoice
  
  payment_number VARCHAR(50) NOT NULL,
  amount NUMERIC(18,6) NOT NULL,
  status PAYMENT_STATUS NOT NULL,
  
  payment_method PAYMENT_METHOD NOT NULL,
  provider_transaction_id VARCHAR(100),
  
  idempotency_key VARCHAR(100) UNIQUE NOT NULL,
  
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  FOREIGN KEY (loan_id) REFERENCES loans(id),
  FOREIGN KEY (installment_id) REFERENCES installments(id),
  FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
);
```

---

## 🔍 Audit Trail Example

```
Customer pays Installment #1 of Loan ABC (May 15, 2026)

Database Records:
┌─────────────────────────────────────────────────────────────┐
│ invoices table                                              │
├─────────────────────────────────────────────────────────────┤
│ invoice_id: inv-2026-05-15-001                              │
│ loan_id: loan-12345                                         │
│ installment_id: inst-001                                    │
│ amount: 4500 SAR                                            │
│ status: PAID                                                │
│ payment_id: payment-abc123                                  │
│ paid_at: 2026-05-15T12:05:45Z                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ payments table                                              │
├─────────────────────────────────────────────────────────────┤
│ id: payment-abc123                                          │
│ invoice_id: inv-2026-05-15-001  ← Links to invoice         │
│ installment_id: inst-001        ← Links to installment     │
│ amount: 4500 SAR                                            │
│ status: COMPLETED                                           │
│ provider_transaction_id: TXN-999                            │
│ created_at: 2026-05-15T12:00:00Z                            │
│ completed_at: 2026-05-15T12:05:45Z                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ installments table (after allocation)                       │
├─────────────────────────────────────────────────────────────┤
│ id: inst-001                                                │
│ status: PAID ✅                                             │
│ outstanding_fee: 0                                          │
│ outstanding_profit: 0                                       │
│ outstanding_principal: 0                                    │
│ paid_total: 4500                                            │
│ paid_at: 2026-05-15T12:05:45Z                               │
└─────────────────────────────────────────────────────────────┘

Complete Audit Trail:
2026-05-15T12:00:00Z → Customer initiates payment
                       invoice_id: inv-2026-05-15-001
2026-05-15T12:03:30Z → HyperPay gateway processes
2026-05-15T12:05:45Z → Payment completed
                       TXN-999
                       Allocation applied to inst-001
```

---

## 🎯 Summary

| Aspect | Details |
|--------|---------|
| **When to use installmentId** | ALWAYS - identifies which installment to pay |
| **When to use invoiceId** | ALWAYS - creates audit trail & prevents duplicates |
| **invoiceId Format** | inv-{date}-{unique_suffix} or inv-{uuid} |
| **installmentId Source** | From GET /api/v1/loans/{id}/installments response |
| **Uniqueness** | One invoiceId per payment attempt (regenerate if retry) |
| **Database Impact** | Creates records in invoices + payments + installments tables |
| **Audit Trail** | invoiceId → paymentId → installmentId (full linkage) |

---

## 💻 API Update Required

**Collections-Service API must be updated** to accept these fields:

```java
public record InitiatePaymentCommand(
    UUID tenantId,
    UUID loanId,
    UUID installmentId,        // ✅ ADD THIS
    String invoiceId,          // ✅ ADD THIS
    UUID customerId,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    LocalDate valueDate,
    String idempotencyKey
) {}
```

---

**Remember**: Without installmentId and invoiceId, the system has **no way to know which installment is being paid**. Always include both!
