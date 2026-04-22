# TRANSACTION GET APIs Structure - Current Operations Dashboard

> **Purpose**: Sabhi in-flight transactions/operations ko real-time track karna
> **Data Source**: Lending Service, Collections Service, Wallet Service, Risk Service
> **Use Case**: Dashboard mein current activity dikhana

---

## 🎯 **HIGH-LEVEL ARCHITECTURE**

```
┌─────────────────────────────────────────────┐
│     TRANSACTION DASHBOARD (Frontend)        │
│  • Live transactions                        │
│  • In-progress operations                   │
│  • Recent activity feed                     │
│  • Real-time counters                       │
└─────────────────────────────────────────────┘
                      ↓
        ┌─────────────────────────────────┐
        │   TRANSACTION GET APIs          │
        │   (New endpoints)               │
        └─────────────────────────────────┘
                      ↓
    ┌──────────────────────────────────────────┐
    │ Ledger Service                           │
    │ Collections Service                      │
    │ Wallet Service                           │
    │ Risk Service                             │
    │ Lending Service                          │
    └──────────────────────────────────────────┘
```

---

## 📊 **TRANSACTION TYPES (In-Flight Operations)**

```yaml
# ════════════════════════════════════════════════════════
# 0. GL TRANSACTIONS (Fineract Journal Entries) ⭐ NEW
# ════════════════════════════════════════════════════════
Status: SUBMITTED → POSTED / FAILED / PENDING
Data tracked:
  - Journal Entry ID (Fineract)
  - Reference Number (JE-2026-001)
  - Entry Date, Post Date
  - GL Accounts (Debit/Credit)
  - Amount, Currency (SAR)
  - Status: SUBMITTED, POSTED, FAILED
  - Related transaction (Loan ID, Payment ID, Disbursement ID)
  - Approver, Approval date
  - GL Balance before/after

# ════════════════════════════════════════════════════════
# 1. LOAN APPLICATION TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: APPLY → PENDING → APPROVED → DISBURSED
Data tracked:
  - Customer name, national ID
  - Loan amount, product type
  - Current status + stage
  - Timeline (apply date, approval date, disburse date)
  - Officer assigned, latest action
  - Next pending action

# ════════════════════════════════════════════════════════
# 2. DISBURSEMENT TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: INITIATED → SUBMITTED → APPROVED → PROCESSING → COMPLETED
Data tracked:
  - Loan ID, loan number
  - Customer name
  - Disbursement amount
  - Current status
  - Disbursal date, actual payment date
  - Payment method (bank transfer, SADAD, etc.)
  - Reference number
  - Fineract transaction ID (if synced)

# ════════════════════════════════════════════════════════
# 3. PAYMENT/REPAYMENT TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: INITIATED → PENDING_PROVIDER → COMPLETED / FAILED
Data tracked:
  - Payment ID
  - Loan ID, customer name
  - Payment amount, date
  - Type: FULL / PARTIAL
  - Method: SADAD, Card, Bank Transfer
  - Status: SUBMITTED, POSTED, FAILED
  - Outstanding balance after payment
  - Reference number

# ════════════════════════════════════════════════════════
# 4. RESCHEDULING TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: SUBMITTED → PENDING_APPROVAL → APPROVED / REJECTED → APPLIED
Data tracked:
  - Reschedule ID
  - Loan ID, customer name
  - Type: SKIP_PAYMENT / TENURE_EXTENSION / RESTRUCTURING
  - Current status
  - Requested by (customer / officer)
  - Approver (if needed), approval status
  - New terms (if applicable)

# ════════════════════════════════════════════════════════
# 5. EARLY SETTLEMENT TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: QUOTE_GENERATED → SETTLEMENT_INITIATED → COMPLETED / CANCELLED
Data tracked:
  - Settlement ID
  - Loan ID, customer name
  - Settlement amount
  - Quote validity period
  - Status
  - Profit waiver amount (if any)
  - Settlement date

# ════════════════════════════════════════════════════════
# 6. WALLET TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: INITIATED → COMPLETED / FAILED
Data tracked:
  - Transaction ID
  - Customer name, wallet ID
  - Amount, currency (SAR)
  - Type: TOP_UP / DEBIT / REVERSAL
  - Status
  - Timestamp
  - Reference number

# ════════════════════════════════════════════════════════
# 7. RISK ASSESSMENT TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: SUBMITTED → IN_PROGRESS → COMPLETED
Data tracked:
  - Assessment ID
  - Customer name, entity ID
  - Risk score
  - Status (pending, in-review, completed)
  - Reviewer assigned
  - Assessment results

# ════════════════════════════════════════════════════════
# 8. KYC/ONBOARDING TRANSACTIONS
# ════════════════════════════════════════════════════════
Status: INITIATED → PENDING_VERIFICATION → COMPLETED / REJECTED
Data tracked:
  - Onboarding ID
  - Customer name, national ID
  - Current step (Basic Info → KYC → Risk → Approval)
  - Status at each step
  - Missing documents/info
  - Timeline
```

---

## 🔗 **GET API ENDPOINTS STRUCTURE**

### **1️⃣ LOAN APPLICATION TRANSACTIONS**

```
BASE: /api/v1/transactions/loan-applications

1. GET /api/v1/transactions/loan-applications
   Description: List all loan applications (in-flight + recent)
   Auth: @SecuredEndpoint(obj = "transactions", act = "read")
   Query Params:
     - status: APPLY,PENDING,APPROVED,DISBURSED,REJECTED,CANCELLED
     - productId: filter by product
     - from: start date
     - to: end date
     - limit: 50 (default), max 500
     - offset: 0
     - sortBy: createdAt (default), updatedAt, status
   
   Response: {
     "applications": [
       {
         "id": "uuid",
         "applicationNumber": "APP-2026-00001",
         "customerName": "Ahmed Al-Saud",
         "nationalId": "1234567890",
         "loanAmount": 100000.00,
         "productType": "MURABAHA",
         "productName": "Personal Murabaha",
         "currentStatus": "APPROVED",
         "currentStage": "PENDING_DISBURSAL",  # Stepper stage
         "appliedOn": "2026-03-15T10:30:00Z",
         "approvedOn": "2026-03-28T14:20:00Z",
         "nextAction": "DISBURSE_LOAN",
         "assignedTo": "Officer Name",
         "daysInStage": 2,
         "riskScore": 45,
         "riskLevel": "LOW"
       },
       ...
     ],
     "totalCount": 250,
     "limit": 50,
     "offset": 0
   }

2. GET /api/v1/transactions/loan-applications/{applicationId}
   Description: Get detailed application transaction
   
   Response: {
     "application": {
       "id": "uuid",
       "applicationNumber": "APP-2026-00001",
       "customerId": "uuid",
       "customerName": "Ahmed Al-Saud",
       "... (same as above)",
       "timeline": [
         {
           "stage": "APPLICATION",
           "status": "COMPLETED",
           "completedOn": "2026-03-15T10:30:00Z",
           "completedBy": "System"
         },
         {
           "stage": "BASIC_INFO",
           "status": "COMPLETED",
           "completedOn": "2026-03-15T11:00:00Z"
         },
         {
           "stage": "BANK_ACCOUNT",
           "status": "COMPLETED",
           "completedOn": "2026-03-16T09:30:00Z"
         },
         {
           "stage": "KYC_VERIFICATION",
           "status": "COMPLETED",
           "completedOn": "2026-03-20T14:20:00Z"
         },
         {
           "stage": "RISK_ASSESSMENT",
           "status": "COMPLETED",
           "riskScore": 45,
           "completedOn": "2026-03-21T10:00:00Z"
         },
         {
           "stage": "APPROVAL",
           "status": "COMPLETED",
           "completedOn": "2026-03-28T14:20:00Z",
           "approvedBy": "Underwriter Name"
         },
         {
           "stage": "CONTRACT_SIGNING",
           "status": "PENDING",
           "daysWaiting": 2
         },
         {
           "stage": "DISBURSAL",
           "status": "PENDING",
           "expectedDate": "2026-04-10"
         }
       ]
     }
   }

3. GET /api/v1/transactions/loan-applications/status/{status}
   Description: Filter by specific status
   
   Example: /api/v1/transactions/loan-applications/status/PENDING
   Response: (same as #1)

4. GET /api/v1/transactions/loan-applications/pending-actions
   Description: Show applications waiting for officer action
   
   Response: {
     "pendingActions": [
       {
         "id": "uuid",
         "applicationNumber": "APP-2026-00001",
         "actionRequired": "APPROVE_DISBURSEMENT",
         "actionSince": "2026-03-30T00:00:00Z",
         "daysPending": 2,
         "assignedTo": "Officer Name"
       },
       ...
     ]
   }
```

---

### **2️⃣ DISBURSEMENT TRANSACTIONS**

```
BASE: /api/v1/transactions/disbursements

1. GET /api/v1/transactions/disbursements
   Description: List all disbursements (in-flight + recent)
   Query Params: same as loan applications + paymentMethod
   
   Response: {
     "disbursements": [
       {
         "id": "uuid",
         "disbursementNumber": "DISB-2026-00001",
         "loanId": "uuid",
         "loanNumber": "LOAN-2026-00001",
         "customerName": "Ahmed Al-Saud",
         "amount": 100000.00,
         "currency": "SAR",
         "status": "PROCESSING",  # INITIATED, SUBMITTED, APPROVED, PROCESSING, COMPLETED, FAILED
         "paymentMethod": "BANK_TRANSFER",
         "initiatedOn": "2026-03-30T10:00:00Z",
         "approvedOn": "2026-03-30T11:30:00Z",
         "expectedPaymentDate": "2026-03-31",
         "actualPaymentDate": null,
         "referenceNumber": "REF-ABC-123",
         "fineractTxnId": 5001,
         "paymentGatewayStatus": "IN_PROGRESS"
       },
       ...
     ],
     "totalCount": 100
   }

2. GET /api/v1/transactions/disbursements/{disbursementId}
   Description: Get detailed disbursement
   
   Response: {
     "disbursement": {
       "... (same as above)",
       "fundingSource": "INVESTOR_POOL",  # or PLATFORM_EQUITY
       "beneficiaryAccount": {
         "accountNumber": "1234567890",
         "accountHolder": "Ahmed Al-Saud",
         "bankCode": "RIAD",
         "bankName": "Riyadh Bank"
       },
       "logs": [
         {
           "timestamp": "2026-03-30T10:00:00Z",
           "action": "INITIATED",
           "actor": "System",
           "details": "Disbursement request created"
         },
         {
           "timestamp": "2026-03-30T10:15:00Z",
           "action": "SUBMITTED_FOR_APPROVAL",
           "actor": "Processing Team",
           "details": ""
         },
         {
           "timestamp": "2026-03-30T11:30:00Z",
           "action": "APPROVED",
           "actor": "Operations Head",
           "details": ""
         },
         {
           "timestamp": "2026-03-30T12:00:00Z",
           "action": "PAYMENT_INITIATED",
           "actor": "System",
           "details": "Sent to payment gateway"
         },
         {
           "timestamp": "2026-03-30T15:30:00Z",
           "action": "IN_PROGRESS",
           "actor": "Payment Gateway",
           "details": "Processing at bank"
         }
       ]
     }
   }

3. GET /api/v1/transactions/disbursements/status/{status}
   Example: /api/v1/transactions/disbursements/status/PROCESSING
   
4. GET /api/v1/transactions/disbursements/pending-approval
   Description: Disbursements waiting for approval
   
5. GET /api/v1/transactions/disbursements/failed
   Description: Failed disbursements with retry capability
```

---

### **3️⃣ PAYMENT/REPAYMENT TRANSACTIONS**

```
BASE: /api/v1/transactions/payments

1. GET /api/v1/transactions/payments
   Description: List all payments
   Query Params: same + paymentMethod, paymentType (FULL/PARTIAL)
   
   Response: {
     "payments": [
       {
         "id": "uuid",
         "paymentNumber": "PAY-2026-00001",
         "loanId": "uuid",
         "loanNumber": "LOAN-2026-00001",
         "customerName": "Ahmed Al-Saud",
         "paymentAmount": 5000.00,
         "paymentType": "PARTIAL",  # FULL or PARTIAL
         "status": "COMPLETED",  # INITIATED, PENDING, POSTED, FAILED, REVERSED
         "paymentMethod": "SADAD",
         "initiatedOn": "2026-03-30T10:00:00Z",
         "completedOn": "2026-03-30T15:30:00Z",
         "referenceNumber": "SADAD-XYZ-789",
         "providerTxnId": "TXN-1234567",
         "allocation": {
           "principalPaid": 3500.00,
           "profitPaid": 1200.00,
           "feePaid": 50.00,
           "waiverAmount": 0.00
         },
         "outstandingBalance": 96500.00
       },
       ...
     ],
     "totalCount": 500
   }

2. GET /api/v1/transactions/payments/{paymentId}
   Description: Get detailed payment with allocation details
   
   Response: {
     "payment": {
       "... (same as above)",
       "allocationDetails": {
         "installments": [
           {
             "installmentNumber": 1,
             "dueDate": "2026-05-01",
             "principalAllocated": 1667.00,
             "profitAllocated": 417.00,
             "feeAllocated": 50.00
           },
           {
             "installmentNumber": 2,
             "dueDate": "2026-06-01",
             "principalAllocated": 1833.00,
             "profitAllocated": 783.00,
             "feeAllocated": 0.00
           }
         ]
       },
       "timeline": [
         {
           "timestamp": "2026-03-30T10:00:00Z",
           "action": "INITIATED",
           "details": "Payment request created"
         },
         {
           "timestamp": "2026-03-30T10:10:00Z",
           "action": "PENDING_PROVIDER",
           "details": "Waiting for payment gateway"
         },
         {
           "timestamp": "2026-03-30T15:30:00Z",
           "action": "COMPLETED",
           "details": "Payment successful"
         },
         {
           "timestamp": "2026-03-30T16:00:00Z",
           "action": "POSTED_TO_LEDGER",
           "details": "GL entries created"
         }
       ]
     }
   }

3. GET /api/v1/transactions/payments/by-loan/{loanId}
   Description: Get all payments for a specific loan
   
   Response: {
     "loanId": "uuid",
     "loanNumber": "LOAN-2026-00001",
     "totalPaymentsMade": 10000.00,
     "paymentCount": 5,
     "lastPaymentDate": "2026-03-30",
     "payments": [...]
   }

4. GET /api/v1/transactions/payments/pending-posting
   Description: Payments completed but GL entries pending
   
5. GET /api/v1/transactions/payments/failed
   Description: Failed payments with retry capability
```

---

### **4️⃣ RESCHEDULING TRANSACTIONS**

```
BASE: /api/v1/transactions/reschedules

1. GET /api/v1/transactions/reschedules
   Description: List all reschedule requests
   Query Params: type, status, from, to
   
   Response: {
     "reschedules": [
       {
         "id": "uuid",
         "loanId": "uuid",
         "loanNumber": "LOAN-2026-00001",
         "customerName": "Ahmed Al-Saud",
         "type": "TENURE_EXTENSION",  # SKIP_PAYMENT, TENURE_EXTENSION, PAYMENT_HOLIDAY, RESTRUCTURING
         "status": "APPROVED",  # SUBMITTED, PENDING_APPROVAL, APPROVED, REJECTED, APPLIED
         "requestedOn": "2026-03-28T10:00:00Z",
         "requestedBy": "Customer / Officer",
         "approvedOn": "2026-03-29T14:30:00Z",
         "approvedBy": "Manager Name",
         "appliedOn": "2026-03-30T09:00:00Z",
         "newTerms": {
           "extensionMonths": 12,
           "newTenure": 72,
           "newInstallment": 1833.00,
           "newMaturityDate": "2028-12-01"
         }
       },
       ...
     ]
   }

2. GET /api/v1/transactions/reschedules/{rescheduleId}
   Description: Get detailed reschedule with full timeline
   
   Response: {
     "reschedule": {
       "... (same as above)",
       "timeline": [
         {
           "stage": "SUBMITTED",
           "status": "COMPLETED",
           "timestamp": "2026-03-28T10:00:00Z",
           "actor": "Customer"
         },
         {
           "stage": "ELIGIBILITY_CHECK",
           "status": "COMPLETED",
           "timestamp": "2026-03-28T10:15:00Z",
           "reason": "Loan age >= 3 months, no arrears"
         },
         {
           "stage": "AWAITING_APPROVAL",
           "status": "COMPLETED",
           "timestamp": "2026-03-28T10:30:00Z",
           "approvalRequired": true,
           "approverRole": "operations_head"
         },
         {
           "stage": "APPROVED",
           "status": "COMPLETED",
           "timestamp": "2026-03-29T14:30:00Z",
           "approvedBy": "Manager Name"
         },
         {
           "stage": "SCHEDULE_GENERATION",
           "status": "COMPLETED",
           "timestamp": "2026-03-29T15:00:00Z"
         },
         {
           "stage": "FINERACT_SYNC",
           "status": "COMPLETED",
           "timestamp": "2026-03-29T16:00:00Z",
           "fineractTxnId": 5002
         },
         {
           "stage": "APPLIED",
           "status": "COMPLETED",
           "timestamp": "2026-03-30T09:00:00Z"
         }
       ]
     }
   }

3. GET /api/v1/transactions/reschedules/pending-approval
   Description: Reschedules waiting for manager decision
   
4. GET /api/v1/transactions/reschedules/by-loan/{loanId}
   Description: Get all reschedules for a loan
```

---

### **5️⃣ EARLY SETTLEMENT TRANSACTIONS**

```
BASE: /api/v1/transactions/settlements

1. GET /api/v1/transactions/settlements
   Description: List all settlement requests
   
   Response: {
     "settlements": [
       {
         "id": "uuid",
         "loanId": "uuid",
         "customerName": "Ahmed Al-Saud",
         "status": "COMPLETED",  # QUOTE_GENERATED, SETTLEMENT_INITIATED, COMPLETED, CANCELLED
         "quotedAmount": 98500.00,
         "settlementAmount": 98500.00,
         "quoteGeneratedOn": "2026-03-29T10:00:00Z",
         "quoteValidTill": "2026-03-30T10:00:00Z",  # 24 hours validity
         "settlementCompletedOn": "2026-03-30T14:30:00Z",
         "breakdown": {
           "principalOutstanding": 96500.00,
           "accruedProfit": 2000.00,
           "unearnedProfitWaived": 500.00,
           "feeOutstanding": 0.00
         }
       },
       ...
     ]
   }

2. GET /api/v1/transactions/settlements/{settlementId}
   Description: Get detailed settlement
   
3. GET /api/v1/transactions/settlements/pending-payment
   Description: Settlements with payment pending
```

---

### **6️⃣ WALLET TRANSACTIONS**

```
BASE: /api/v1/transactions/wallet

1. GET /api/v1/transactions/wallet/transfers
   Description: List all wallet transactions
   Query Params: customerId, type (TOP_UP/DEBIT/REVERSAL), status, from, to
   
   Response: {
     "transactions": [
       {
         "id": "uuid",
         "customerId": "uuid",
         "customerName": "Ahmed Al-Saud",
         "walletId": "uuid",
         "amount": 5000.00,
         "currency": "SAR",
         "type": "TOP_UP",  # TOP_UP, DEBIT, REVERSAL
         "status": "COMPLETED",  # INITIATED, PENDING, COMPLETED, FAILED
         "timestamp": "2026-03-30T10:00:00Z",
         "referenceNumber": "WALLET-ABC-123",
         "balanceAfter": 95000.00,
         "description": "Loan disbursement credit"
       },
       ...
     ]
   }

2. GET /api/v1/transactions/wallet/{customerId}/balance
   Description: Get current wallet balance
   
   Response: {
     "customerId": "uuid",
     "walletId": "uuid",
     "currentBalance": 95000.00,
     "currency": "SAR",
     "lastTransaction": {
       "timestamp": "2026-03-30T10:00:00Z",
       "type": "TOP_UP",
       "amount": 5000.00
     }
   }

3. GET /api/v1/transactions/wallet/pending-completion
   Description: Wallet transactions pending processing
```

---

### **7️⃣ RISK ASSESSMENT TRANSACTIONS**

```
BASE: /api/v1/transactions/risk-assessments

1. GET /api/v1/transactions/risk-assessments
   Description: List all risk assessments in progress
   Query Params: status, entityType (CUSTOMER/LOAN), from, to
   
   Response: {
     "assessments": [
       {
         "id": "uuid",
         "entityId": "uuid",
         "entityType": "CUSTOMER",
         "entityName": "Ahmed Al-Saud",
         "riskScore": 45,
         "riskLevel": "LOW",  # LOW, MEDIUM, HIGH, CRITICAL
         "status": "COMPLETED",  # SUBMITTED, IN_PROGRESS, COMPLETED
         "assessedOn": "2026-03-30T14:30:00Z",
         "assessedBy": "Risk Officer",
         "reasons": [
           "New customer profile",
           "Salary income verified",
           "No previous defaults"
         ],
         "flags": []
       },
       ...
     ]
   }

2. GET /api/v1/transactions/risk-assessments/{assessmentId}
   Description: Get detailed assessment
   
   Response: {
     "assessment": {
       "... (same as above)",
       "detailedScoring": {
         "watchlistScore": 0,
         "velocityScore": 10,
         "newCustomerScore": 20,
         "multiNidScore": 15,
         "totalScore": 45
       },
       "rules_triggered": [
         "VELOCITY_IP_CHECK",
         "VELOCITY_DEVICE_CHECK"
       ]
     }
   }

3. GET /api/v1/transactions/risk-assessments/pending-review
   Description: Assessments waiting for manual review
```

---

### **8️⃣ KYC/ONBOARDING TRANSACTIONS**

```
BASE: /api/v1/transactions/onboarding

1. GET /api/v1/transactions/onboarding
   Description: List all onboarding in progress
   Query Params: status (INITIATED,IN_PROGRESS,COMPLETED,REJECTED), from, to
   
   Response: {
     "onboardings": [
       {
         "id": "uuid",
         "customerId": "uuid",
         "customerName": "Ahmed Al-Saud",
         "nationalId": "1234567890",
         "mobileNumber": "0512345678",
         "currentStep": "RISK_ASSESSMENT",  # BASIC_INFO → BANK_ACCOUNT → KYC → RISK → APPROVAL
         "completedSteps": ["BASIC_INFO", "BANK_ACCOUNT", "KYC"],
         "pendingSteps": ["RISK_ASSESSMENT", "APPROVAL"],
         "status": "IN_PROGRESS",
         "initiatedOn": "2026-03-28T10:00:00Z",
         "daysInProcess": 2,
         "completionEstimate": "2026-03-31",
         "missingDocuments": [],
         "nextAction": "Risk assessment in progress"
       },
       ...
     ]
   }

2. GET /api/v1/transactions/onboarding/{onboardingId}
   Description: Get detailed onboarding progress
   
   Response: {
     "onboarding": {
       "... (same as above)",
       "stepTimeline": [
         {
           "step": "BASIC_INFO",
           "status": "COMPLETED",
           "completedOn": "2026-03-28T11:00:00Z"
         },
         {
           "step": "BANK_ACCOUNT",
           "status": "COMPLETED",
           "completedOn": "2026-03-29T09:30:00Z"
         },
         {
           "step": "KYC_VERIFICATION",
           "status": "COMPLETED",
           "completedOn": "2026-03-29T15:00:00Z",
           "kycProvider": "NAFATH",
           "verificationStatus": "SUCCESS"
         },
         {
           "step": "RISK_ASSESSMENT",
           "status": "IN_PROGRESS",
           "startedOn": "2026-03-30T10:00:00Z",
           "riskScore": 45,
           "riskLevel": "LOW"
         },
         {
           "step": "APPROVAL",
           "status": "PENDING",
           "estimatedStartDate": "2026-03-31"
         }
       ]
     }
   }

3. GET /api/v1/transactions/onboarding/pending-actions
   Description: Onboarding requests waiting for officer action
   
4. GET /api/v1/transactions/onboarding/stuck
   Description: Onboarding in process > 3 days
```

---

## 📊 **SUMMARY API ENDPOINT (Dashboard Cards)**

```
GET /api/v1/transactions/summary?from=2026-01-01&to=2026-03-30

Response: {
  "summary": {
    "loanApplications": {
      "total": 150,
      "inProgress": 25,
      "pendingApproval": 8,
      "approvedAwaitingDisbursal": 5
    },
    "disbursements": {
      "total": 140,
      "inProgress": 3,
      "completed": 135,
      "failed": 2,
      "totalAmount": 14000000.00
    },
    "payments": {
      "total": 500,
      "completed": 490,
      "failed": 10,
      "pendingPosting": 2,
      "totalAmount": 2500000.00
    },
    "reschedules": {
      "total": 50,
      "applied": 45,
      "pending": 3,
      "rejected": 2
    },
    "settlements": {
      "total": 20,
      "completed": 18,
      "pending": 2,
      "totalAmount": 1960000.00
    },
    "walletTransactions": {
      "total": 300,
      "topups": 150,
      "debits": 140,
      "reversals": 10,
      "totalAmount": 3500000.00
    },
    "riskAssessments": {
      "total": 155,
      "completed": 150,
      "pending": 5,
      "flagged": 3
    },
    "onboardingInProgress": {
      "total": 15,
      "step1": 2,
      "step2": 3,
      "step3": 4,
      "step4": 4,
      "step5": 2
    }
  },
  "lastUpdated": "2026-03-30T20:00:00Z"
}
```

---

## 🏗️ **SERVICE STRUCTURE**

```
services/

├── lending-service/
│   └── adapter/rest/controller/
│       ├── LoanApplicationTransactionController.java (NEW)
│       └── DisbursementTransactionController.java (NEW)
│
├── collections-service/
│   └── adapter/rest/controller/
│       ├── PaymentTransactionController.java (NEW)
│       ├── RescheduleTransactionController.java (NEW)
│       └── SettlementTransactionController.java (NEW)
│
├── wallet-service/
│   └── adapter/rest/controller/
│       └── WalletTransactionController.java (NEW)
│
├── risk-service/
│   └── adapter/rest/controller/
│       └── RiskAssessmentTransactionController.java (NEW)
│
└── onboarding-workflow-service/
    └── adapter/rest/controller/
        └── OnboardingTransactionController.java (NEW)
```

---

## 🔒 **SECURITY (Authorization)**

```yaml
All endpoints require:
  Auth: @SecuredEndpoint(obj = "transactions", act = "read")
  Roles: admin, operations_head, accountant, customer (self-only)

Role-based visibility:
  admin: All transactions
  operations_head: Applications, Disbursements, Reschedules
  accountant: Payments, Settlements, GL posting
  customer: Own transactions only
  risk_officer: Risk assessments only
```

---

## 📈 **PERFORMANCE CONSIDERATIONS**

```yaml
Pagination:
  - Default limit: 50
  - Max limit: 500
  - Implement offset-based pagination

Caching:
  - Transaction summary: Cache 5 minutes
  - Transaction list: Cache 1 minute
  - Individual transaction: Cache 10 minutes (non-sensitive data)

Filtering:
  - Index on: tenant_id, status, createdAt, updatedAt
  - Support: date range, status, type filters

Real-time updates:
  - WebSocket for status changes (optional)
  - Polling: 30 seconds for active transactions
```

---

## 📝 **IMPLEMENTATION PHASES**

```
Phase 1: Lending Service (Application + Disbursement)
Phase 2: Collections Service (Payment + Reschedule + Settlement)
Phase 3: Wallet Service (Wallet Transactions)
Phase 4: Risk Service (Risk Assessments)
Phase 5: Onboarding Service (KYC Progress)
Phase 6: Dashboard Integration + Real-time Updates
Phase 7: Export/Report functionality
```

---

Yeh pura structure ready hai! Ab kaunsa service se shuru karte hain? 🚀
