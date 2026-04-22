# GL TRANSACTION APIs & FINERACT REPORTS - Complete Implementation Structure

> **Status**: Controller stubs created with DTOs fully defined
> **Next Phase**: Implement repository methods and business logic for data retrieval

---

## 📋 OVERVIEW

All GL Transaction Query APIs and Fineract Financial Reports have been scaffolded with:
- ✅ Complete DTOs (request/response records)
- ✅ REST controller stubs with @SecuredEndpoint authorization
- ✅ Swagger/OpenAPI documentation
- ✅ Proper error handling and tenant isolation
- ⏳ Business logic implementation (TODO)

---

## 🔗 GL TRANSACTION QUERY APIs

**Base URL**: `/api/v1/transactions/gl-entries`  
**Authorization**: All endpoints require `@SecuredEndpoint(obj = "gl.entries", act = "read")`

### 1. List All GL Entries (Paginated & Filtered)

```
GET /api/v1/transactions/gl-entries

Query Parameters:
  from         (Date, optional)     "2026-01-01" - Start date filter
  to           (Date, optional)     "2026-03-30" - End date filter
  status       (String, optional)   "POSTED,SUBMITTED,FAILED"
  type         (String, optional)   "DISBURSEMENT,REPAYMENT,SETTLEMENT,RESTRUCTURING"
  loanId       (UUID, optional)     Loan ID filter
  limit        (int, default: 100)  Pagination limit
  offset       (int, default: 0)    Pagination offset

Response: GLEntriesListResponse
├── glEntries (List<GLEntryResponse>)
├── totalCount
├── limit, offset
└── summary (GLSummaryResponse)
    ├── totalDebit, totalCredit
    ├── balanced
    └── submitted, posted, failed (counts)

Status: 200 OK | 400 Bad Request
```

### 2. Get Single GL Entry Details

```
GET /api/v1/transactions/gl-entries/{glEntryId}

Response: GLEntryResponse
├── id, fineractJournalEntryId
├── referenceNumber, entryType, entryDate
├── loanId, loanNumber, customerName
├── lines (List<GLLineResponse>)
│  ├── accountCode, accountName
│  ├── debitAmount, creditAmount
│  └── description
├── totalDebit, totalCredit, balanced
├── status (POSTED|SUBMITTED|FAILED|PENDING_APPROVAL)
├── submittedAt, postedAt
├── relatedTransaction
└── fineractStatus, syncedAt

Status: 200 OK | 404 Not Found
```

### 3. Filter GL Entries by Type

```
GET /api/v1/transactions/gl-entries/filter/by-type?type=DISBURSEMENT

Supported Types:
  - DISBURSEMENT    (Dr. Loans Receivable, Cr. Bank)
  - REPAYMENT       (Dr. Bank, Cr. Loans Receivable, Cr. Profit)
  - SETTLEMENT      (Dr. Bank, Cr. LR, Cr. Profit, ± Waiver)
  - RESTRUCTURING   (Dr. Provision, Cr. LR, ± Profit Waiver)
  - ACCRUAL         (Future)

Response: GLEntriesListResponse (paginated)
Status: 200 OK | 400 Bad Request
```

### 4. Filter GL Entries by Status

```
GET /api/v1/transactions/gl-entries/filter/by-status?status=FAILED

Supported Statuses:
  - POSTED              ✅ Successfully in Fineract GL
  - SUBMITTED           ⏳ Waiting for Fineract response
  - FAILED              ❌ Failed to post, needs reconciliation
  - PENDING_APPROVAL    👤 Pending manual review (if configured)

Response: GLEntriesListResponse (paginated)
Status: 200 OK | 400 Bad Request
```

### 5. Filter GL Entries by Loan

```
GET /api/v1/transactions/gl-entries/filter/by-loan/{loanId}

Returns all GL entries (disbursements, repayments, settlements) related to a loan.

Response: GLEntriesListResponse
  ├── All GL entries for the loan
  └── Summary with total debits/credits for loan

Status: 200 OK | 404 Not Found
```

### 6. Daily GL Reconciliation Report

```
GET /api/v1/transactions/gl-entries/reconciliation?date=2026-03-30

Purpose: Verify GL entries are balanced and all statuses accounted for

Response: GLReconciliationResponse
├── date
├── totalEntries
├── byStatus (POSTED count, SUBMITTED count, FAILED count)
├── totalDebits, totalCredits, balanced
├── byType (breakdown by DISBURSEMENT, REPAYMENT, SETTLEMENT, RESTRUCTURING)
│  ├── count
│  ├── totalAmount
│  └── allPosted
└── failedEntries (List<FailedGLEntryResponse>)
   ├── id, referenceNumber
   ├── failureReason
   ├── retryCount
   └── nextRetryAt

Status: 200 OK
```

### 7. Get Failed GL Entries

```
GET /api/v1/transactions/gl-entries/failed

Purpose: List all failed entries needing manual reconciliation

Query Parameters:
  limit   (default: 100)
  offset  (default: 0)

Response: List<GLEntryResponse> (filtered for status = FAILED)
Status: 200 OK
```

### 8. Daily GL Summary

```
GET /api/v1/transactions/gl-entries/daily-summary?date=2026-03-30

Purpose: Summary statistics for a specific day

Response: GLSummaryResponse
├── totalDebit, totalCredit
├── balanced
└── submitted, posted, failed (counts)

Status: 200 OK
```

---

## 📊 FINERACT REPORTS APIs

**Base URL**: `/api/v1/reports`  
**Authorization**: Each report has specific `@SecuredEndpoint(obj, act)` authorization

### 1. Trial Balance Report

```
GET /api/v1/reports/trial-balance?date=2026-03-30

Purpose: All GL accounts with their balances (debits should equal credits)
Use Case: Daily end-of-day reconciliation

Auth: @SecuredEndpoint(obj = "reports.trial-balance", act = "read")

Response: TrialBalanceReportResponse
├── reportDate
├── accounts (List<TrialBalanceAccountResponse>)
│  ├── accountCode (1010, 1200, 1300, 2200, 3100...)
│  ├── accountName
│  ├── openingBalance
│  ├── debits, credits (period activity)
│  └── closingBalance
├── totalDebits, totalCredits
├── balanced
└── generatedAt

Status: 200 OK
```

### 2. Loan Portfolio Summary Report

```
GET /api/v1/reports/portfolio-summary?from=2026-01-01&to=2026-03-30

Purpose: Portfolio health overview
Use Case: Portfolio monitoring, investor reporting

Auth: @SecuredEndpoint(obj = "reports.portfolio", act = "read")

Response: PortfolioSummaryReportResponse
├── fromDate, toDate
├── totalLoans
├── totalDisbursed
├── outstandingPrincipal
├── collectionsReceived
├── activeLoans
├── delinquentDays1to30, delinquentDays31to60, defaultedLoans
├── portfolioHealth (% performing)
└── generatedAt

Status: 200 OK
```

### 3. DPD Bucket Report

```
GET /api/v1/reports/dpd-buckets?date=2026-03-30

Purpose: Delinquency analysis by days past due
Use Case: Collections management, early intervention

Auth: @SecuredEndpoint(obj = "reports.dpd", act = "read")

Response: DPDBucketReportResponse
├── reportDate
├── buckets (List<DPDBucketResponse>)
│  ├── bucketName (Current (DPD 0), Grace Period (DPD 1-30), etc.)
│  ├── dpqRange
│  ├── loanCount
│  └── totalAmount
├── totalLoans, totalOutstanding
├── trend (movement analysis from previous day)
│  ├── enteredGrace, exitedGrace
│  ├── enteredMild, enteredHigh
│  └── newDefaults
└── generatedAt

Status: 200 OK
```

### 4. Collections & Repayment Report

```
GET /api/v1/reports/collections?from=2026-03-01&to=2026-03-30

Purpose: Payment collections analysis
Use Case: Collections performance tracking

Auth: @SecuredEndpoint(obj = "reports.collections", act = "read")

Response: CollectionsReportResponse
├── fromDate, toDate
├── totalCollections
├── paymentCount, averagePaymentSize
├── byPaymentMethod (SADAD, Bank Transfer, Card, etc.)
│  ├── amount, percentage, count
├── performance (CollectionPerformanceResponse)
│  ├── expectedCollections, actualCollections
│  ├── collectionRate
│  ├── onTimePayments, latePayments
└── generatedAt

Status: 200 OK
```

### 5. Profit & Revenue Report

```
GET /api/v1/reports/profit-revenue?period=2026-03

Purpose: Profit earned vs collected
Use Case: Revenue recognition, profit quality analysis

Auth: @SecuredEndpoint(obj = "reports.profit", act = "read")

Response: ProfitRevenueReportResponse
├── period (YYYY-MM)
├── totalProfitEarned
├── profitCollected
├── unearnedProfit
├── byProductType (MURABAHA, IJARA, TAWARRUQ, etc.)
│  ├── productType
│  ├── profitEarned, profitCollected
│  └── activeContracts
├── profitQuality (collected / earned %)
└── generatedAt

Status: 200 OK
```

### 6. Write-off & Provisions Report

```
GET /api/v1/reports/write-off-provisions?period=2026-03

Purpose: Bad debt provisions and restructured loans
Use Case: Risk management, SAMA compliance

Auth: @SecuredEndpoint(obj = "reports.writeoff", act = "read")

Response: WriteOffProvisionReportResponse
├── period (YYYY-MM)
├── totalProvisions
├── totalWriteOffs
├── restructuredLoans, restructuredAmount
├── coverageRatio (provisions / risky assets)
└── generatedAt

Status: 200 OK
```

### 7. Cash Flow Report

```
GET /api/v1/reports/cash-flow?date=2026-03-30

Purpose: Bank account liquidity
Use Case: Daily liquidity monitoring

Auth: @SecuredEndpoint(obj = "reports.cashflow", act = "read")

Response: CashFlowReportResponse
├── reportDate
├── openingBalance
├── totalInflows, totalOutflows
├── closingBalance, netPosition
├── inflowsBySource (Collections, Capital, etc.)
├── outflowsByCategory (Disbursements, Operating, etc.)
└── generatedAt

Status: 200 OK
```

### 8. Investor/Portfolio Report

```
GET /api/v1/reports/investor-portfolio?period=2026-03

Purpose: AUM and returns for investors
Use Case: Investor reporting, syndication

Auth: @SecuredEndpoint(obj = "reports.investor", act = "read")

Response: InvestorPortfolioReportResponse
├── period
├── totalAUM
├── totalProfitEarned, totalReturnsPaid
├── averageROI
├── investorPerformance (List<InvestorPerformanceResponse>)
│  ├── investorId, investorName
│  ├── aum, profitEarned, returnsPaid
│  └── roi
└── generatedAt

Status: 200 OK
```

### 9. Reconciliation Report

```
GET /api/v1/reports/reconciliation-detail?date=2026-03-30

Purpose: Our GL entries vs Fineract GL verification
Use Case: System consistency check

Auth: @SecuredEndpoint(obj = "reports.reconciliation", act = "read")

Response: ReconciliationReportDetailResponse
├── reportDate
├── ourSystemEntries, fineractEntries
├── matchedEntries
├── unmatchedInOurSystem, unmatchedInFineract
├── pendingItems (List<PendingReconciliationResponse>)
│  ├── ourTransactionId, referenceNumber
│  ├── ourAmount, fineractAmount, variance
│  └── daysPending
├── totalVariance
└── generatedAt

Status: 200 OK
```

### 10. SAMA Regulatory Report

```
GET /api/v1/reports/sama-regulatory?period=2026-03

Purpose: Compliance metrics for SAMA
Use Case: Regulatory submission

Auth: @SecuredEndpoint(obj = "reports.sama", act = "read")

Response: SAMARegulatorReportResponse
├── period
├── capitalAdequacyRatio
├── delinquencyPercentage
├── nonPerformingLoanPercentage
├── profitQuality
├── shariProductPortfolio (MURABAHA %, IJARA %, etc.)
├── sectorConcentration (Retail %, SME %, Corporate %, etc.)
├── liquidityRatio
└── generatedAt

Status: 200 OK
```

---

## 📁 CODE STRUCTURE

```
services/ledger-service/src/main/java/com/ksa/financing/ledger/
├── application/dto/
│   ├── GLEntryResponse.java                    ✅ GL transaction response DTO
│   ├── GLEntriesListResponse.java              ✅ Paginated GL list
│   ├── GLReconciliationResponse.java           ✅ Reconciliation report DTO
│   └── FineractReportResponses.java            ✅ All 10 report DTOs
│
├── adapter/rest/controller/
│   ├── JournalEntryController.java             ✅ Existing (POST entries)
│   ├── GLTransactionQueryController.java       ✅ NEW - 8 GL query GET endpoints
│   └── FineractReportsController.java          ✅ NEW - 10 report GET endpoints
│
├── domain/port/out/
│   └── JournalEntryRepository.java             ⏳ Needs query methods (TODO)
│       ├── findByDateRange(tenantId, from, to)
│       ├── findByStatus(tenantId, status)
│       ├── findByType(tenantId, type)
│       ├── findByLoan(tenantId, loanId)
│       ├── dailyReconciliation(tenantId, date)
│       └── dailySummary(tenantId, date)
│
└── infrastructure/persistence/
    └── repository/
        ├── JpaJournalEntryRepository.java      ⏳ Needs JPA query methods (TODO)
        └── JournalEntryRepositoryImpl.java      ⏳ Needs implementation (TODO)
```

---

## 🎯 IMPLEMENTATION ROADMAP

### Phase 1: Core GL Transaction Queries (THIS PHASE)
- ✅ Create DTOs and controllers (DONE)
- ⏳ Implement JPA repository query methods
  - `findByDateRange()` with pagination
  - `findByStatus()` with filters
  - `findByType()` with filters
  - `findByLoan()` with filters
  - Daily reconciliation logic
  - Daily summary aggregation

**Effort**: 2-3 days  
**Priority**: HIGH (foundation for all reports)

### Phase 2: Core Financial Reports (Phase 2)
- Trial Balance Report (account balances from GL accounts table)
- Loan Portfolio Summary (loan counts and amounts from GL)
- DPD Bucket Report (loan delinquency from status/schedule)
- Collections Report (repayment GL entries analysis)
- Profit Revenue Report (profit GL entries analysis)

**Effort**: 3-4 days  
**Priority**: HIGH (daily operational need)

### Phase 3: Advanced Reports (Phase 3)
- Write-off Provisions (provisions GL account tracking)
- Cash Flow (bank account GL analysis)
- Investor Reports (if using syndication)
- Reconciliation (Fineract API comparison)
- SAMA Regulatory (compliance metrics aggregation)

**Effort**: 4-5 days  
**Priority**: MEDIUM (monthly/quarterly need)

---

## 🔒 SECURITY NOTES

All endpoints require:
1. **JWT Authentication** - Keycloak bearer token with `tenant_id` claim
2. **Casbin Authorization** - Tenant-specific obj+act policies from Redis
3. **Tenant Isolation** - All queries filtered by `tenant_id` from JWT

Example Policy Rules:
```
accountant, gl.entries, read, allow
head_of_accounts, gl.entries, read, allow
admin, gl.entries, read, allow

accountant, reports.trial-balance, read, allow
head_of_accounts, reports.*, read, allow
admin, reports.*, read, allow
```

---

## 📝 DATABASE CONSIDERATIONS

### Current GL Tables (from JPA entities)
- `journal_entry_jpa_entity` - Main GL entry
- `journal_line_jpa_entity` - GL entry lines (debit/credit)
- `account_jpa_entity` - Chart of accounts
- `idempotency_key_jpa_entity` - Idempotency tracking
- `journal_entry_sync_log_jpa_entity` - Fineract sync status
- `account_balance_jpa_entity` - Account balance snapshots

### New Indexes Needed for Query Performance
```sql
-- GL query optimization
CREATE INDEX idx_journal_entry_tenant_date ON journal_entry_jpa_entity(tenant_id, entry_date DESC);
CREATE INDEX idx_journal_entry_tenant_status ON journal_entry_jpa_entity(tenant_id, status);
CREATE INDEX idx_journal_entry_tenant_type ON journal_entry_jpa_entity(tenant_id, entry_type);
CREATE INDEX idx_journal_entry_tenant_loan ON journal_entry_jpa_entity(tenant_id, loan_id);
CREATE INDEX idx_journal_entry_tenant_reference ON journal_entry_jpa_entity(tenant_id, reference_id);

-- Account balance queries for reports
CREATE INDEX idx_account_balance_tenant_date ON account_balance_jpa_entity(tenant_id, balance_date DESC);
```

---

## 📋 TESTING STRATEGY

### Unit Tests
- GL Entry creation and validation
- GL Summary calculations
- DPD bucket logic

### Integration Tests
- GL query filtering by date range
- GL query filtering by status/type
- Daily reconciliation logic
- Report calculations from test data

### Contract Tests
- Fineract GL API consistency
- Report output structure validation

### Performance Tests
- Large date range queries (100k+ entries)
- Report generation for large portfolios

---

## 🚀 NEXT STEPS

1. **Implement Repository Query Methods**
   - Extend `JpaJournalEntryRepository` with filtering/pagination queries
   - Implement `JournalEntryRepositoryImpl` with actual database logic

2. **Implement Report Business Logic**
   - Create service layer classes for each report type
   - Aggregate GL data into report DTOs

3. **Add Report Caching**
   - Cache daily reports (regenerate at EOD)
   - Cache monthly reports (regenerate at month-end)

4. **Dashboard Integration**
   - Frontend components for GL transaction list
   - Dashboard cards for each report type

5. **SAMA Compliance**
   - Validate report data against SAMA requirements
   - Generate audit trail for regulatory submissions

---

## 📚 REFERENCES

- **GL Transaction Flow**: `/docs/GL_TRANSACTION_FLOW.md`
- **Fineract Reports**: `/docs/FINERACT_REPORTS_STRUCTURE.md`
- **Transaction APIs**: `/docs/TRANSACTION_GET_APIS_STRUCTURE.md`
- **Service Code**: `services/ledger-service/`
- **Error Codes**: `ErrorCodes` in `foundational-infra-sdk`
