# GL TRANSACTION FLOW & GET APIs

> **Purpose**: Track GL journal entries being posted from our services to Fineract via ledger-service
> **Data Flow**: Lending/Collections Service → Ledger Service → Fineract GL

---

## 📊 **CURRENT GL ENTRIES BEING POSTED TO FINERACT**

```
1️⃣ DISBURSEMENT ENTRY
   Trigger: Loan disbursement completed
   GL Entry:
     Dr. Loans Receivable (1200)        100,000 SAR
     Cr. Bank Account (1010)                        100,000 SAR
   
   Data tracked:
     - Loan ID, Loan Number
     - Amount, Currency
     - Reference Number (DISB-2026-00001)
     - Status: SUBMITTED → POSTED
     - Fineract Journal Entry ID
     - Created by, Created date

2️⃣ REPAYMENT ENTRY
   Trigger: Payment/Repayment received
   GL Entry:
     Dr. Bank Account (1010)             5,000 SAR
     Cr. Loans Receivable (1200)  3,500 SAR
     Cr. Profit Receivable (1300)  1,500 SAR
   
   Data tracked:
     - Loan ID, Payment ID
     - Amount breakdown (principal, profit, fee)
     - Reference Number (PAY-2026-00001)
     - Status: SUBMITTED → POSTED
     - Fineract Journal Entry ID
     - Created by, Created date

3️⃣ EARLY SETTLEMENT ENTRY
   Trigger: Early settlement completed
   GL Entry:
     Dr. Bank Account (1010)         98,500 SAR
     Cr. Loans Receivable (1200)                    96,500 SAR
     Cr. Profit Receivable (1300)                    2,000 SAR
     
     Dr. Unearned Profit (2200)         500 SAR
     Cr. Profit Receivable (1300)                      500 SAR
   
   Data tracked:
     - Loan ID, Settlement ID
     - Amount breakdown
     - Profit waiver amount
     - Reference Number
     - Status
     - Fineract Journal Entry ID

4️⃣ RESTRUCTURING WRITE-OFF ENTRY
   Trigger: Loan restructuring with write-off
   GL Entry:
     Dr. Provision for Bad Debts (3100)  5,000 SAR
     Cr. Loans Receivable (1200)                    5,000 SAR
     
     Dr. Unearned Profit (2200)          1,000 SAR
     Cr. Profit Receivable (1300)                   1,000 SAR
   
   Data tracked:
     - Loan ID, Reschedule ID
     - Write-off amount
     - Profit waiver amount
     - Reference Number
     - Status
     - Fineract Journal Entry ID
```

---

## 🔄 **COMPLETE FLOW DIAGRAM**

```
┌─────────────────────────────────────┐
│  LENDING/COLLECTIONS SERVICE        │
│  (Loan App, Disbursement, Payment)  │
└─────────────────┬───────────────────┘
                  │
                  │ Event Triggered:
                  │ - Disbursement approved
                  │ - Payment received
                  │ - Settlement completed
                  │ - Restructuring approved
                  │
                  ▼
┌──────────────────────────────────────────────────────┐
│  TEMPORAL WORKFLOW (LoanApplicationWorkflowImpl)      │
│                                                      │
│  Calls: LedgerActivityImpl.postDisbursementGlEntry() │
│         LedgerActivityImpl.postRepaymentGlEntry()    │
│         LedgerActivityImpl.postRestructuringEntry()  │
└──────────────────┬───────────────────────────────────┘
                   │
                   │ Temporal Activity
                   │
                   ▼
┌──────────────────────────────────────────────────┐
│  LEDGER SERVICE                                  │
│  (GL Bridge - Single entry point to Fineract)   │
│                                                  │
│  Endpoints:                                      │
│  POST /api/v1/gl/entries/disbursement           │
│  POST /api/v1/gl/entries/repayment              │
│  POST /api/v1/gl/entries/settlement             │
│  POST /api/v1/gl/entries/restructuring          │
└──────────────────┬───────────────────────────────┘
                   │
                   │ Creates GL entry in DB
                   │ Then syncs to Fineract
                   │
                   ▼
┌──────────────────────────────────────────────────┐
│  FINERACT                                        │
│  (GL Accounting System)                          │
│                                                  │
│  POST /api/v1/journalentries                   │
│  Status: SUBMITTED → POSTED                     │
└──────────────────────────────────────────────────┘
```

---

## 🎯 **GET APIs NEEDED (To Track GL Entries)**

### **1. List All GL Entries Posted**

```
GET /api/v1/transactions/gl-entries

Auth: @SecuredEndpoint(obj = "gl.entries", act = "read")
Roles: accountant, head_of_accounts, admin

Query Parameters:
  - from: "2026-01-01"        (Date filter)
  - to: "2026-03-30"          (Date filter)
  - status: "POSTED,SUBMITTED,FAILED"  (Filter by status)
  - type: "DISBURSEMENT,REPAYMENT,SETTLEMENT,RESTRUCTURING"
  - loanId: "uuid"            (Filter by loan)
  - limit: 100 (default)
  - offset: 0
  - sortBy: "createdAt" (desc)

Response:
{
  "glEntries": [
    {
      "id": "uuid",                        # Our GL entry ID
      "fineractJournalEntryId": 5001,     # Fineract JE ID
      "referenceNumber": "DISB-2026-00001", # Our reference
      "entryType": "DISBURSEMENT",        # Type of entry
      "entryDate": "2026-03-30",
      
      "loanId": "uuid",
      "loanNumber": "LOAN-2026-00001",
      "customerName": "Ahmed Al-Saud",
      
      "lines": [
        {
          "accountCode": "1200",
          "accountName": "Loans Receivable",
          "debitAmount": 100000.00,
          "creditAmount": 0.00
        },
        {
          "accountCode": "1010",
          "accountName": "Bank Account",
          "debitAmount": 0.00,
          "creditAmount": 100000.00
        }
      ],
      
      "totalDebit": 100000.00,
      "totalCredit": 100000.00,
      "balanced": true,
      
      "status": "POSTED",                 # SUBMITTED, POSTED, FAILED
      "submittedAt": "2026-03-30T10:00:00Z",
      "postedAt": "2026-03-30T10:15:00Z",
      
      "createdBy": "system",
      "idempotencyKey": "DISB-2026-00001-001",
      
      "relatedTransaction": {
        "type": "DISBURSEMENT",
        "id": "uuid",
        "referenceNumber": "DISB-2026-00001"
      },
      
      "fineractStatus": "POSTED",
      "fineractPostedDate": "2026-03-30",
      "syncedAt": "2026-03-30T10:15:30Z"
    },
    {
      "id": "uuid",
      "fineractJournalEntryId": 5002,
      "referenceNumber": "PAY-2026-00001",
      "entryType": "REPAYMENT",
      "entryDate": "2026-03-30",
      
      "loanId": "uuid",
      "loanNumber": "LOAN-2026-00001",
      "customerName": "Ahmed Al-Saud",
      
      "lines": [
        {
          "accountCode": "1010",
          "accountName": "Bank Account",
          "debitAmount": 5000.00,
          "creditAmount": 0.00
        },
        {
          "accountCode": "1200",
          "accountName": "Loans Receivable",
          "debitAmount": 0.00,
          "creditAmount": 3500.00
        },
        {
          "accountCode": "1300",
          "accountName": "Profit Receivable",
          "debitAmount": 0.00,
          "creditAmount": 1500.00
        }
      ],
      
      "status": "POSTED",
      "submittedAt": "2026-03-30T15:00:00Z",
      "postedAt": "2026-03-30T15:05:00Z",
      
      "relatedTransaction": {
        "type": "PAYMENT",
        "id": "uuid",
        "referenceNumber": "PAY-2026-00001"
      }
    },
    ...
  ],
  
  "totalCount": 500,
  "limit": 100,
  "offset": 0,
  "summary": {
    "totalDebit": 5000000.00,
    "totalCredit": 5000000.00,
    "balanced": true,
    "submitted": 10,
    "posted": 485,
    "failed": 5
  }
}
```

---

### **2. Get Single GL Entry Details**

```
GET /api/v1/transactions/gl-entries/{glEntryId}

Response:
{
  "id": "uuid",
  "fineractJournalEntryId": 5001,
  "referenceNumber": "DISB-2026-00001",
  
  ... (same as above, single entry)
  
  "fullTimeline": [
    {
      "timestamp": "2026-03-30T10:00:00Z",
      "action": "CREATED",
      "actor": "System",
      "details": "GL entry created in DB"
    },
    {
      "timestamp": "2026-03-30T10:05:00Z",
      "action": "SUBMITTED_TO_FINERACT",
      "actor": "Ledger Service",
      "fineractRequest": {...},
      "details": "Posted to Fineract API"
    },
    {
      "timestamp": "2026-03-30T10:10:00Z",
      "action": "FINERACT_POSTED",
      "actor": "Fineract",
      "fineractResponse": {...},
      "details": "Successfully posted in Fineract"
    }
  ]
}
```

---

### **3. Filter by Entry Type**

```
GET /api/v1/transactions/gl-entries?type=DISBURSEMENT

GET /api/v1/transactions/gl-entries?type=REPAYMENT

GET /api/v1/transactions/gl-entries?type=SETTLEMENT

GET /api/v1/transactions/gl-entries?type=RESTRUCTURING

GET /api/v1/transactions/gl-entries?type=ACCRUAL  (future)
```

---

### **4. Filter by Status**

```
GET /api/v1/transactions/gl-entries?status=POSTED
   Returns: Successfully posted entries (normal)

GET /api/v1/transactions/gl-entries?status=SUBMITTED
   Returns: Entries waiting for Fineract response

GET /api/v1/transactions/gl-entries?status=FAILED
   Returns: Failed entries (need reconciliation)

GET /api/v1/transactions/gl-entries?status=PENDING_APPROVAL
   Returns: Entries pending manual review (if any)
```

---

### **5. GL Entry Verification & Reconciliation**

```
GET /api/v1/transactions/gl-entries/reconciliation?date=2026-03-30

Response:
{
  "date": "2026-03-30",
  "totalEntries": 100,
  
  "byStatus": {
    "POSTED": 95,
    "SUBMITTED": 3,
    "FAILED": 2
  },
  
  "totalDebits": 5000000.00,
  "totalCredits": 5000000.00,
  "balanced": true,
  
  "byType": {
    "DISBURSEMENT": {
      "count": 50,
      "totalAmount": 3000000.00,
      "allPosted": true
    },
    "REPAYMENT": {
      "count": 40,
      "totalAmount": 1800000.00,
      "allPosted": true
    },
    "SETTLEMENT": {
      "count": 8,
      "totalAmount": 800000.00,
      "allPosted": true
    },
    "RESTRUCTURING": {
      "count": 2,
      "totalAmount": 400000.00,
      "allPosted": true
    }
  },
  
  "failedEntries": [
    {
      "id": "uuid",
      "referenceNumber": "DISB-2026-00050",
      "failureReason": "Fineract API timeout",
      "retryCount": 2,
      "nextRetryAt": "2026-03-30T20:00:00Z"
    }
  ]
}
```

---

### **6. GL Entries by Loan**

```
GET /api/v1/transactions/gl-entries?loanId={loanId}

Response:
{
  "loanId": "uuid",
  "loanNumber": "LOAN-2026-00001",
  "customerName": "Ahmed Al-Saud",
  
  "entries": [
    {
      "referenceNumber": "DISB-2026-00001",
      "entryType": "DISBURSEMENT",
      "amount": 100000.00,
      "status": "POSTED",
      "date": "2026-03-30"
    },
    {
      "referenceNumber": "PAY-2026-00001",
      "entryType": "REPAYMENT",
      "amount": 5000.00,
      "status": "POSTED",
      "date": "2026-03-30"
    }
  ],
  
  "totalAmountPosted": 105000.00,
  "loanBalance": 95000.00
}
```

---

### **7. Failed GL Entries (With Retry Capability)**

```
GET /api/v1/transactions/gl-entries/failed?limit=50

Response:
{
  "failedEntries": [
    {
      "id": "uuid",
      "referenceNumber": "DISB-2026-00050",
      "entryType": "DISBURSEMENT",
      "loanNumber": "LOAN-2026-00050",
      
      "status": "FAILED",
      "failureReason": "Connection timeout to Fineract",
      "failedAt": "2026-03-30T10:05:00Z",
      
      "retryCount": 2,
      "maxRetries": 5,
      "nextRetryAt": "2026-03-30T20:00:00Z",
      
      "totalAmount": 50000.00
    }
  ],
  
  "totalFailed": 5,
  "retryableCount": 5,
  "requiresManualReview": 0
}

POST /api/v1/transactions/gl-entries/{glEntryId}/retry
   Force immediate retry
   
Response:
{
  "message": "Retry initiated",
  "newAttempt": 3,
  "nextRetryAt": "2026-03-30T21:00:00Z"
}
```

---

### **8. GL Daily Reconciliation Report**

```
GET /api/v1/transactions/gl-entries/daily-report?date=2026-03-30

Response:
{
  "reportDate": "2026-03-30",
  "preparedAt": "2026-03-30T23:59:00Z",
  
  "summary": {
    "totalEntriesCreated": 105,
    "totalEntriesPosted": 100,
    "totalEntriesFailed": 5,
    "totalEntriesPending": 0,
    
    "totalDayDebits": 5000000.00,
    "totalDayCredits": 5000000.00,
    "balanced": true
  },
  
  "byEntryType": {
    "DISBURSEMENT": {
      "created": 50,
      "posted": 50,
      "failed": 0,
      "totalAmount": 3000000.00
    },
    "REPAYMENT": {
      "created": 40,
      "posted": 38,
      "failed": 2,
      "totalAmount": 1800000.00
    },
    "SETTLEMENT": {
      "created": 8,
      "posted": 8,
      "failed": 0,
      "totalAmount": 800000.00
    },
    "RESTRUCTURING": {
      "created": 7,
      "posted": 4,
      "failed": 3,
      "totalAmount": 400000.00
    }
  },
  
  "accountBalances": {
    "1010": {
      "name": "Bank Account",
      "openingBalance": 1000000.00,
      "totalDebits": 4600000.00,
      "totalCredits": 4400000.00,
      "closingBalance": 1200000.00
    },
    "1200": {
      "name": "Loans Receivable",
      "openingBalance": 5000000.00,
      "totalDebits": 100000.00,
      "totalCredits": 4500000.00,
      "closingBalance": 600000.00
    },
    "1300": {
      "name": "Profit Receivable",
      "openingBalance": 500000.00,
      "totalDebits": 0,
      "totalCredits": 200000.00,
      "closingBalance": 300000.00
    }
  },
  
  "failedEntries": [
    {
      "referenceNumber": "PAY-2026-00020",
      "reason": "Fineract API timeout",
      "retryAt": "2026-03-30T20:00:00Z"
    },
    ...
  ]
}
```

---

## 🏗️ **SERVICE STRUCTURE (Ledger Service)**

```
services/ledger-service/
│
├── adapter/rest/controller/
│   └── GLTransactionController.java (NEW)
│       ├── GET /api/v1/transactions/gl-entries (list)
│       ├── GET /api/v1/transactions/gl-entries/{id} (detail)
│       ├── GET /api/v1/transactions/gl-entries/reconciliation (verify)
│       ├── GET /api/v1/transactions/gl-entries/failed (failed entries)
│       ├── GET /api/v1/transactions/gl-entries/daily-report (summary)
│       └── POST /api/v1/transactions/gl-entries/{id}/retry (retry failed)
│
├── application/usecase/
│   ├── FetchGLEntriesUseCaseImpl.java
│   ├── FetchGLEntryDetailUseCaseImpl.java
│   ├── GenerateGLDailyReportUseCaseImpl.java
│   └── RetryFailedGLEntryUseCaseImpl.java
│
├── domain/port/out/
│   └── GLEntryRepository.java
│       ├── findAll(filters)
│       ├── findById(id)
│       ├── findFailed(limit)
│       └── findByLoan(loanId)
│
└── infrastructure/persistence/
    ├── entity/
    │   ├── GLEntryJpaEntity.java
    │   └── GLEntryLineJpaEntity.java
    │
    └── repository/
        ├── JpaGLEntryRepository.java
        └── GLEntryRepositoryImpl.java
```

---

## 🔐 **SECURITY**

```yaml
All endpoints:
  - Require: @SecuredEndpoint(obj = "gl.entries", act = "read")
  - Roles: accountant, head_of_accounts, admin
  
Sensitive operations:
  - Retry: Only head_of_accounts or admin
  - Delete/Cancel: Not allowed (audit trail immutable)
  
Audit trail:
  - Every view logged with who, when, what filters
  - Failed entry retries logged
```

---

## 📊 **DATABASE SCHEMA**

```sql
CREATE TABLE gl_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    fineract_journal_entry_id BIGINT,
    
    reference_number VARCHAR(50) NOT NULL,
    entry_type VARCHAR(50) NOT NULL,  -- DISBURSEMENT, REPAYMENT, etc
    entry_date DATE NOT NULL,
    
    loan_id UUID,
    payment_id UUID,
    settlement_id UUID,
    reschedule_id UUID,
    
    related_transaction_type VARCHAR(50),
    related_transaction_id UUID,
    related_reference_number VARCHAR(50),
    
    total_debit NUMERIC(19,2) DEFAULT 0,
    total_credit NUMERIC(19,2) DEFAULT 0,
    balanced BOOLEAN DEFAULT false,
    
    status VARCHAR(20) DEFAULT 'SUBMITTED',  -- SUBMITTED, POSTED, FAILED
    fineract_status VARCHAR(50),
    
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    posted_at TIMESTAMPTZ,
    synced_at TIMESTAMPTZ,
    
    failure_reason TEXT,
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    
    created_by UUID,
    idempotency_key VARCHAR(100),
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    UNIQUE(tenant_id, idempotency_key),
    UNIQUE(tenant_id, fineract_journal_entry_id),
    FOREIGN KEY(loan_id) REFERENCES loans(id)
);

CREATE TABLE gl_entry_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL,
    
    account_code VARCHAR(20) NOT NULL,
    account_name VARCHAR(255),
    debit_amount NUMERIC(19,2) DEFAULT 0,
    credit_amount NUMERIC(19,2) DEFAULT 0,
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    FOREIGN KEY(entry_id) REFERENCES gl_entries(id) ON DELETE CASCADE
);

CREATE INDEX idx_gl_entries_tenant ON gl_entries(tenant_id);
CREATE INDEX idx_gl_entries_status ON gl_entries(status);
CREATE INDEX idx_gl_entries_date ON gl_entries(entry_date);
CREATE INDEX idx_gl_entries_loan ON gl_entries(loan_id);
CREATE INDEX idx_gl_entries_type ON gl_entries(entry_type);
```

---

Yeh complete structure hai! **Ab kya next?** 🚀

Implement karna start karten hain? Ya kuch changes chahiye?
