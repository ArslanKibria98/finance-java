# Invoice Details API - Complete Flow

**Get Invoice Details + Make Payment**

---

## 📱 Complete Mobile Flow

```
┌─────────────────────────────────────────────────────┐
│ STEP 1: Get Loan Installments                       │
├─────────────────────────────────────────────────────┤
│ GET /api/v1/loans/{loanId}/installments             │
│                                                     │
│ Response:                                           │
│ ├─ invoiceId: "INV-54F5B2DD-001"  ← Use this      │
│ ├─ installmentNumber: 1            ← Use this      │
│ ├─ dueDate: "2026-05-10"                           │
│ ├─ installmentAmount: 100.30                       │
│ ├─ outstandingBalance: 906.70                      │
│ └─ paymentStatus: "PENDING"                        │
└──────────┬────────────────────────────────────────┘
           ⬇️
┌─────────────────────────────────────────────────────┐
│ STEP 2: Get Invoice Details (NEW!)                  │
├─────────────────────────────────────────────────────┤
│ GET /api/v1/invoices/{invoiceId}                    │
│                                                     │
│ Where invoiceId = "INV-54F5B2DD-001"                │
│                                                     │
│ Response: Complete invoice details                  │
│ ├─ invoiceId                                        │
│ ├─ loanId                                           │
│ ├─ customerId                                       │
│ ├─ installmentNumber                                │
│ ├─ dueDate                                          │
│ ├─ installmentAmount                                │
│ ├─ principalComponent                               │
│ ├─ profitComponent                                  │
│ ├─ outstandingBalance                               │
│ ├─ paymentStatus                                    │
│ ├─ lateFeesApplied                                  │
│ ├─ totalDueAmount (with late fees if any)           │
│ └─ receiptAvailable                                 │
└──────────┬────────────────────────────────────────┘
           ⬇️
┌─────────────────────────────────────────────────────┐
│ STEP 3: Pay Invoice                                 │
├─────────────────────────────────────────────────────┤
│ POST /api/v1/payments                               │
│                                                     │
│ {                                                   │
│   "invoiceId": "INV-54F5B2DD-001",  ← From Step 1  │
│   "loanId": "loan-uuid",                            │
│   "amount": 100.30,                                 │
│   "paymentMethod": "HYPERPAY_MADA"                  │
│ }                                                   │
└──────────┬────────────────────────────────────────┘
           ⬇️
┌─────────────────────────────────────────────────────┐
│ STEP 4: Payment Completed                           │
│ ✅ Invoice marked as PAID                           │
│ ✅ Allocation applied (fees → profit → principal)   │
└─────────────────────────────────────────────────────┘
```

---

## 🔗 API Endpoints

### 1. GET /api/v1/loans/{loanId}/installments

**Purpose**: Get all installments for a loan

```bash
GET /api/v1/loans/loan-uuid/installments
Authorization: Bearer {{jwt_token}}
```

**Response**:
```json
{
  "data": [
    {
      "invoiceId": "INV-54F5B2DD-001",
      "installmentNumber": 1,
      "dueDate": "2026-05-10",
      "installmentAmount": 100.302973,
      "principalComponent": 95.00,
      "profitComponent": 5.70,
      "outstandingBalance": 906.697027,
      "paymentStatus": "PENDING",
      "paidDate": null,
      "paidAmount": null,
      "receiptAvailable": false
    },
    {
      "invoiceId": "INV-54F5B2DD-002",
      "installmentNumber": 2,
      ...
    }
  ],
  "message": "success",
  "timestamp": "2026-04-16T07:27:00Z"
}
```

---

### 2. GET /api/v1/invoices/{invoiceId} (NEW!)

**Purpose**: Get detailed information about a specific invoice

```bash
GET /api/v1/invoices/INV-54F5B2DD-001
Authorization: Bearer {{jwt_token}}
```

**Response**:
```json
{
  "data": {
    "invoiceId": "INV-54F5B2DD-001",
    "loanId": "loan-uuid",
    "customerId": "customer-uuid",
    "tenantId": "tenant-uuid",
    
    "installmentNumber": 1,
    "dueDate": "2026-05-10",
    "gracePeriodDays": 10,
    "gracePeriodEndDate": "2026-05-20",
    
    "installmentAmount": 100.302973,
    "principalComponent": 95.00,
    "profitComponent": 5.70,
    "lateFeesApplied": 0.00,
    "totalDueAmount": 100.302973,
    
    "outstandingBalance": 906.697027,
    "outstandingPrincipal": 95.00,
    "outstandingProfit": 5.70,
    "outstandingFees": 0.00,
    
    "paymentStatus": "PENDING",
    "daysOverdue": 0,
    "isOverdue": false,
    
    "paidDate": null,
    "paidAmount": null,
    "paymentMethod": null,
    "providerTransactionId": null,
    
    "receiptAvailable": false,
    "receiptUrl": null,
    
    "createdAt": "2026-04-16T00:00:00Z",
    "updatedAt": "2026-04-16T00:00:00Z",
    
    "loanDetails": {
      "loanNumber": "LOAN-2026-00001",
      "productType": "MURABAHA",
      "tenor": 10,
      "principalAmount": 1000.00,
      "profitRate": 5.70,
      "totalProfit": 57.00
    },
    
    "customerDetails": {
      "customerId": "customer-uuid",
      "name": "Ahmed Al-Saudhi",
      "iban": "SA12345678901234567890",
      "mobileNumber": "966501234567"
    }
  },
  "message": "success",
  "timestamp": "2026-04-16T07:27:00Z"
}
```

**Key Fields Explained**:

| Field | Description |
|-------|-------------|
| `invoiceId` | Unique invoice ID (use for payment) |
| `installmentNumber` | Which installment this is (1st, 2nd, etc) |
| `dueDate` | When payment is due |
| `gracePeriodDays` | Grace period after due date |
| `gracePeriodEndDate` | Last day before late fees apply |
| `installmentAmount` | Total amount for this installment |
| `principalComponent` | Principal portion of installment |
| `profitComponent` | Islamic profit (Murabaha) portion |
| `lateFeesApplied` | Any accumulated late fees |
| `totalDueAmount` | Total amount to pay (including fees) |
| `outstandingBalance` | Remaining balance after this installment |
| `paymentStatus` | PENDING, PAID, OVERDUE, CANCELLED |
| `daysOverdue` | If overdue, how many days past due |
| `isOverdue` | Boolean flag for overdue |

---

### 3. POST /api/v1/payments

**Purpose**: Initiate payment for an invoice

```bash
POST /api/v1/payments
Authorization: Bearer {{jwt_token}}
X-Idempotency-Key: {{unique_key}}

{
  "invoiceId": "INV-54F5B2DD-001",
  "loanId": "loan-uuid",
  "amount": 100.302973,
  "paymentMethod": "HYPERPAY_MADA",
  "idempotencyKey": "{{unique_key}}",
  "mobileNumber": "966501234567"
}
```

**Response** (202):
```json
{
  "workflowId": "payment-abc123",
  "paymentId": "payment-xyz",
  "invoiceId": "INV-54F5B2DD-001",
  "checkoutUrl": "https://checkout.hyperpay.com/session/CPR-...",
  "status": "INITIATED"
}
```

---

## 📱 Mobile Implementation

### Step 1: Fetch Installments

```typescript
async function getInstallments(loanId: string) {
  const response = await fetch(
    `https://api.kfs.com/api/v1/loans/${loanId}/installments`,
    {
      headers: { 'Authorization': `Bearer ${jwtToken}` }
    }
  );
  
  const data = await response.json();
  return data.data;  // Array of installments
}

// Display to customer
const installments = await getInstallments(loanId);
// Show: Installment #1 - Due: May 10, Amount: 100.30 SAR, Status: PENDING
```

### Step 2: User Selects Installment

```typescript
const selectedInstallment = installments[0];
// {
//   invoiceId: "INV-54F5B2DD-001",
//   installmentNumber: 1,
//   dueDate: "2026-05-10",
//   installmentAmount: 100.302973,
//   outstandingBalance: 906.697027,
//   paymentStatus: "PENDING"
// }
```

### Step 3: Get Invoice Details (Optional but Recommended)

```typescript
async function getInvoiceDetails(invoiceId: string) {
  const response = await fetch(
    `https://api.kfs.com/api/v1/invoices/${invoiceId}`,
    {
      headers: { 'Authorization': `Bearer ${jwtToken}` }
    }
  );
  
  const data = await response.json();
  return data.data;  // Invoice details object
}

// Get full details
const invoice = await getInvoiceDetails(selectedInstallment.invoiceId);

// Display to customer before payment
console.log(`
  Invoice: ${invoice.invoiceId}
  Installment #${invoice.installmentNumber}
  Due Date: ${invoice.dueDate}
  
  Amount Breakdown:
  ├─ Principal: ${invoice.principalComponent} SAR
  ├─ Profit: ${invoice.profitComponent} SAR
  └─ Total: ${invoice.installmentAmount} SAR
  
  Days Overdue: ${invoice.daysOverdue || "0"}
  Late Fees: ${invoice.lateFeesApplied} SAR
  Total Due: ${invoice.totalDueAmount} SAR
`);
```

### Step 4: Initiate Payment

```typescript
async function initiatePayment(invoice: InvoiceDetails) {
  const response = await fetch(
    'https://api.kfs.com/api/v1/payments',
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${jwtToken}`,
        'X-Idempotency-Key': generateUUID(),
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        invoiceId: invoice.invoiceId,        // ← From Step 1
        loanId: invoice.loanId,              // ← From Step 2
        amount: invoice.totalDueAmount,      // ← From Step 3
        paymentMethod: 'HYPERPAY_MADA',
        idempotencyKey: generateUUID()
      })
    }
  );
  
  const data = await response.json();
  return data;
}

const paymentResponse = await initiatePayment(invoice);
// Redirect to HyperPay
navigateToWebView(paymentResponse.checkoutUrl);
```

---

## 🎯 Real Example Flow

### Customer Loan Details
```
Loan ID: loan-uuid
Product: Murabaha
Principal: 1,000 SAR
Tenor: 10 months
Monthly Payment: 100.30 SAR (includes 5.7% profit)
```

### Step 1: Get Installments
```bash
GET /api/v1/loans/loan-uuid/installments

Response:
[
  {
    "invoiceId": "INV-54F5B2DD-001",    ← Use this
    "installmentNumber": 1,
    "dueDate": "2026-05-10",
    "installmentAmount": 100.302973,
    "outstandingBalance": 906.697027,
    "paymentStatus": "PENDING"
  },
  ...more installments
]
```

### Step 2: Display to Customer
```
Mobile Screen:
┌─────────────────────────────────┐
│ Loan Installments               │
├─────────────────────────────────┤
│                                 │
│ ✓ Installment #1                │
│   Due: May 10, 2026             │
│   Amount: 100.30 SAR            │
│   Status: PENDING 🔴            │
│   [PAY NOW] ← User clicks        │
│                                 │
│ ○ Installment #2                │
│   Due: June 10, 2026            │
│   Amount: 100.30 SAR            │
│   Status: PENDING 🔴            │
│   [PAY NOW]                      │
│                                 │
└─────────────────────────────────┘
```

### Step 3: Get Invoice Details
```bash
GET /api/v1/invoices/INV-54F5B2DD-001

Response:
{
  "invoiceId": "INV-54F5B2DD-001",
  "installmentNumber": 1,
  "dueDate": "2026-05-10",
  "installmentAmount": 100.302973,
  "principalComponent": 95.00,
  "profitComponent": 5.70,
  "lateFeesApplied": 0.00,
  "totalDueAmount": 100.302973,
  "outstandingBalance": 906.697027,
  "paymentStatus": "PENDING",
  "daysOverdue": 0,
  "isOverdue": false
}
```

### Step 4: Show Payment Details
```
Mobile Screen:
┌─────────────────────────────────┐
│ Payment Confirmation            │
├─────────────────────────────────┤
│                                 │
│ Invoice: INV-54F5B2DD-001       │
│ Installment #1 of 10            │
│                                 │
│ Amount Breakdown:               │
│ ├─ Principal: 95.00 SAR         │
│ ├─ Profit: 5.70 SAR             │
│ └─ Late Fees: 0.00 SAR          │
│                                 │
│ Total: 100.30 SAR               │
│                                 │
│ Due Date: May 10, 2026          │
│ Days Overdue: 0                 │
│                                 │
│ [CONFIRM & PAY]                 │
│                                 │
└─────────────────────────────────┘
```

### Step 5: Initiate Payment
```bash
POST /api/v1/payments

{
  "invoiceId": "INV-54F5B2DD-001",
  "loanId": "loan-uuid",
  "amount": 100.302973,
  "paymentMethod": "HYPERPAY_MADA",
  "idempotencyKey": "unique-payment-key"
}

Response:
{
  "workflowId": "payment-abc123",
  "checkoutUrl": "https://checkout.hyperpay.com/session/CPR-...",
  "status": "INITIATED"
}
```

### Step 6: Redirect to HyperPay
```
Mobile: Opens WebView → HyperPay checkout page
Customer: Enters card details + OTP
HyperPay: Processes payment
```

### Step 7: Poll Status
```bash
GET /api/v1/payments/payment-abc123/status

Response (after completion):
{
  "status": "COMPLETED",
  "appliedAllocations": {
    "fees": 0.00,
    "profit": 5.70,
    "principal": 95.00
  }
}
```

### Step 8: Mobile Shows Success
```
Mobile Screen:
┌─────────────────────────────────┐
│ ✅ Payment Successful!          │
├─────────────────────────────────┤
│                                 │
│ Invoice: INV-54F5B2DD-001       │
│ Amount: 100.30 SAR              │
│ Status: PAID                    │
│                                 │
│ Next Due:                       │
│ Installment #2                  │
│ Due: June 10, 2026              │
│ Amount: 100.30 SAR              │
│                                 │
│ [VIEW RECEIPT] [HOME]           │
│                                 │
└─────────────────────────────────┘
```

---

## 🔄 Error Scenarios

### Invoice Not Found
```bash
GET /api/v1/invoices/INVALID-ID

Response (404):
{
  "status": 404,
  "error": "Not Found",
  "code": "INVOICE.NOT_FOUND",
  "message": "Invoice not found: INVALID-ID"
}
```

### Invoice Already Paid
```bash
POST /api/v1/payments

{
  "invoiceId": "INV-54F5B2DD-001"  // Already paid
}

Response (422):
{
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "INVOICE.ALREADY_PAID",
  "message": "Invoice INV-54F5B2DD-001 is already paid"
}
```

### Invoice Overdue with Late Fees
```bash
GET /api/v1/invoices/INV-54F5B2DD-001

Response:
{
  "invoiceId": "INV-54F5B2DD-001",
  "dueDate": "2026-05-10",
  "daysOverdue": 6,
  "isOverdue": true,
  "lateFeesApplied": 5.00,
  "installmentAmount": 100.30,
  "totalDueAmount": 105.30,  // ← Includes late fees
  ...
}
```

---

## 📋 Database Schema

### invoices Table

```sql
CREATE TABLE invoices (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL,
  loan_id UUID NOT NULL,
  customer_id UUID NOT NULL,
  
  invoice_id VARCHAR(50) NOT NULL UNIQUE,  -- INV-54F5B2DD-001
  installment_number INT NOT NULL,
  
  due_date DATE NOT NULL,
  grace_period_days INT DEFAULT 10,
  
  principal_component NUMERIC(18,6) NOT NULL,
  profit_component NUMERIC(18,6) NOT NULL,
  installment_amount NUMERIC(18,6) NOT NULL,
  late_fees_applied NUMERIC(18,6) DEFAULT 0,
  total_due_amount NUMERIC(18,6) NOT NULL,
  
  outstanding_balance NUMERIC(18,6) NOT NULL,
  outstanding_principal NUMERIC(18,6) NOT NULL,
  outstanding_profit NUMERIC(18,6) NOT NULL,
  outstanding_fees NUMERIC(18,6) DEFAULT 0,
  
  payment_status VARCHAR(20) NOT NULL,  -- PENDING, PAID, OVERDUE
  days_overdue INT DEFAULT 0,
  
  paid_date TIMESTAMPTZ,
  paid_amount NUMERIC(18,6),
  payment_method VARCHAR(50),
  provider_transaction_id VARCHAR(100),
  
  receipt_available BOOLEAN DEFAULT false,
  receipt_url VARCHAR(500),
  
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  FOREIGN KEY (loan_id) REFERENCES loans(id),
  FOREIGN KEY (customer_id) REFERENCES customers(id),
  UNIQUE(tenant_id, loan_id, installment_number)
);

CREATE INDEX idx_invoices_loan_id ON invoices(tenant_id, loan_id);
CREATE INDEX idx_invoices_invoice_id ON invoices(invoice_id);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
```

---

## ✅ Summary

| Action | API | Method | Use invoiceId? |
|--------|-----|--------|----------------|
| Get all installments | `/loans/{id}/installments` | GET | ← Returns it |
| Get invoice details | `/invoices/{invoiceId}` | GET | ✅ Required |
| Initiate payment | `/payments` | POST | ✅ Required |
| Check payment status | `/payments/{workflowId}/status` | GET | - |

**Remember**: Use `invoiceId` from installments API directly - no need to create custom IDs!

---

**Version**: 2.0 (with Invoice Details API)  
**Last Updated**: 2026-04-16
