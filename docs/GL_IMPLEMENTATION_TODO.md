# GL APIs & Reports Implementation TODO

> **Status**: All 18 GET endpoints scaffolded with DTOs
> **Last Updated**: 2026-04-16

---

## ✅ COMPLETED

### Controllers & DTOs (All Ready)
- [x] GLTransactionQueryController (8 GET endpoints scaffolded)
- [x] FineractReportsController (10 GET endpoints scaffolded)
- [x] GLEntryResponse DTO with all fields
- [x] GLEntriesListResponse with pagination/summary
- [x] GLReconciliationResponse
- [x] All 10 Report DTOs:
  - TrialBalanceReportResponse
  - PortfolioSummaryReportResponse
  - DPDBucketReportResponse
  - CollectionsReportResponse
  - ProfitRevenueReportResponse
  - WriteOffProvisionReportResponse
  - CashFlowReportResponse
  - InvestorPortfolioReportResponse
  - ReconciliationReportDetailResponse
  - SAMARegulatorReportResponse
- [x] Swagger/OpenAPI documentation on all endpoints
- [x] @SecuredEndpoint authorization configured on all endpoints

---

## ⏳ TODO - High Priority (Phase 1: GL Transaction Queries)

### 1. Repository Layer - Query Methods
**Location**: `services/ledger-service/src/main/java/com/ksa/financing/ledger/domain/port/out/JournalEntryRepository.java`

```java
// ADD THESE METHOD SIGNATURES:
Page<JournalEntryAggregate> findByDateRange(
    UUID tenantId, LocalDate from, LocalDate to, Pageable pageable);

Page<JournalEntryAggregate> findByStatus(
    UUID tenantId, String status, Pageable pageable);

Page<JournalEntryAggregate> findByType(
    UUID tenantId, String type, Pageable pageable);

Page<JournalEntryAggregate> findByLoan(
    UUID tenantId, UUID loanId, Pageable pageable);

GLReconciliationData dailyReconciliation(UUID tenantId, LocalDate date);

GLSummaryData dailySummary(UUID tenantId, LocalDate date);
```

**Effort**: 1 day  
**Dependency**: None

### 2. JPA Repository - Native Queries
**Location**: `services/ledger-service/src/main/java/com/ksa/financing/ledger/infrastructure/persistence/repository/JpaJournalEntryRepository.java`

```java
// ADD THESE JPA QUERY METHODS:
@Query("SELECT j FROM JournalEntryJpaEntity j " +
       "WHERE j.tenantId = :tenantId AND j.entryDate BETWEEN :from AND :to " +
       "ORDER BY j.entryDate DESC")
Page<JournalEntryJpaEntity> findByDateRange(
    @Param("tenantId") UUID tenantId,
    @Param("from") LocalDate from,
    @Param("to") LocalDate to,
    Pageable pageable);

@Query("SELECT j FROM JournalEntryJpaEntity j " +
       "WHERE j.tenantId = :tenantId AND j.status = :status")
Page<JournalEntryJpaEntity> findByStatus(
    @Param("tenantId") UUID tenantId,
    @Param("status") String status,
    Pageable pageable);

// Similar for type, loan, etc.
```

**Effort**: 1 day  
**Dependency**: Above

### 3. Repository Implementation - Business Logic
**Location**: `services/ledger-service/src/main/java/com/ksa/financing/ledger/infrastructure/persistence/repository/JournalEntryRepositoryImpl.java`

```java
// IMPLEMENT THESE METHODS (delegate to JPA):
public Page<JournalEntryAggregate> findByDateRange(...) {
    // Call JPA method
    // Map JPA entities → Domain aggregates
    // Return Page<>
}

public GLReconciliationData dailyReconciliation(...) {
    // Count entries by status for the date
    // Sum debits and credits
    // Verify balanced
    // List failed entries
    // Return aggregated data
}

public GLSummaryData dailySummary(...) {
    // Similar to above but for summary only
}
```

**Effort**: 2 days  
**Dependency**: Above two tasks

### 4. Controller Logic - Wire Repository to Response
**Location**: `services/ledger-service/src/main/java/com/ksa/financing/ledger/adapter/rest/controller/GLTransactionQueryController.java`

Current state: Controllers have `TODO` comments  
Need to:

```java
// In listGLEntries():
Page<JournalEntryAggregate> entries = journalEntryRepository.findByDateRange(...);
List<GLEntryResponse> responses = entries.stream()
    .map(this::toGLEntryResponse)  // Add mapper method
    .toList();
// Return GLEntriesListResponse with entries, pagination, summary

// In getReconciliation():
GLReconciliationData data = journalEntryRepository.dailyReconciliation(...);
// Map to GLReconciliationResponse

// Similar for each endpoint
```

**Effort**: 1 day  
**Dependency**: Repository implementation complete

### 5. Add Mapper Methods
**Location**: Add to `JournalEntryMapper.java` or new file

```java
GLEntryResponse toGLEntryResponse(JournalEntryAggregate entry) {
    // Map aggregate fields
    // Convert domain JournalLine → GLLineResponse
    // Handle null checks
    // Return GLEntryResponse
}

GLSummaryResponse toSummary(GLSummaryData data) { ... }

GLReconciliationResponse toReconciliation(GLReconciliationData data) { ... }
```

**Effort**: 1 day  
**Dependency**: Entity structure understanding

---

## ⏳ TODO - Medium Priority (Phase 2: Core Reports)

### 6. Trial Balance Report Implementation
**Location**: Create `services/ledger-service/.../application/service/TrialBalanceReportService.java`

```java
@Service
@RequiredArgsConstructor
public class TrialBalanceReportService {
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    
    public TrialBalanceReportResponse generate(UUID tenantId, LocalDate date) {
        // Fetch all accounts for tenant
        List<AccountAggregate> accounts = accountRepository.findAllByTenant(tenantId);
        
        // For each account:
        //   - Get opening balance as of date
        //   - Get all debits/credits posted before date
        //   - Calculate closing balance
        
        // Verify total debits == total credits
        // Return TrialBalanceReportResponse
    }
}
```

**Effort**: 2 days  
**Dependency**: GL query methods working

### 7. Portfolio Summary Report Implementation
**Location**: `PortfolioSummaryReportService.java`

```java
// Use lending-service loan data + GL entries
// - Count loans by status
// - Sum disbursements from GL
// - Sum collections from GL
// - Calculate DPD buckets
// - Calculate portfolio health %
```

**Effort**: 2 days  
**Dependency**: Lending-service loan API available

### 8. DPD Bucket Report Implementation
**Location**: `DPDBucketReportService.java`

```java
// Get loan status data
// Bucket by last payment date - today (DPD calculation)
// Count loans in each bucket
// Compare to previous day for trend
```

**Effort**: 2 days  
**Dependency**: Loan schedule/payment data access

### 9. Collections Report Implementation
**Location**: `CollectionsReportService.java`

```java
// Query GL repayment entries (Dr. Bank, Cr. Loans Receivable)
// Group by payment method (from payment metadata)
// Calculate on-time vs late (compare to due date)
// Sum totals and averages
```

**Effort**: 1 day  
**Dependency**: Payment GL entries properly tagged with method

### 10. Profit & Revenue Report Implementation
**Location**: `ProfitRevenueReportService.java`

```java
// Query GL Profit Receivable account (1300)
// Sum by product type (Murabaha, Ijara, etc.)
// Calculate collected profit
// Calculate unearned profit from GL Unearned Profit account
```

**Effort**: 1 day  
**Dependency**: Profit GL entries properly structured

---

## ⏳ TODO - Lower Priority (Phase 3: Advanced Reports)

### 11. Write-off & Provisions Report
**Location**: `WriteOffProvisionReportService.java`

```java
// Query GL Provision account (3100)
// Query restructuring GL entries
// Track restructured loans from lending-service
```

**Effort**: 1 day

### 12. Cash Flow Report
**Location**: `CashFlowReportService.java`

```java
// Query GL Bank Account (1010)
// Sum all debits (inflows) and credits (outflows)
// Group by source/category
```

**Effort**: 1 day

### 13. Investor Report (if needed)
**Location**: `InvestorPortfolioReportService.java`

```java
// If using investor syndication:
// Track AUM per investor
// Distribute profit to investors
// Calculate individual ROI
```

**Effort**: 2 days

### 14. Reconciliation Report
**Location**: `ReconciliationReportService.java`

```java
// Call Fineract GL API for same date range
// Compare entry counts
// Match by reference number
// Report unmatched items
```

**Effort**: 1 day

### 15. SAMA Regulatory Report
**Location**: `SAMARegulatorReportService.java`

```java
// Aggregate metrics for SAMA submission:
// - Capital adequacy ratio (from wallet/capital tables)
// - Delinquency % (DPD buckets)
// - NPL % (defaults)
// - Profit quality (collections / earned)
// - Product mix (Murabaha %, Ijara %, etc.)
// - Sector concentration (retail, SME, corporate)
// - Liquidity ratio (cash / liabilities)
```

**Effort**: 2 days

---

## 🗂️ NEW TESTS NEEDED

### Unit Tests
```
services/ledger-service/src/test/java/.../
├── application/service/
│   ├── TrialBalanceReportServiceTest.java
│   ├── PortfolioSummaryReportServiceTest.java
│   ├── DPDBucketReportServiceTest.java
│   └── ... (1 test per report)
└── adapter/rest/controller/
    ├── GLTransactionQueryControllerTest.java
    └── FineractReportsControllerTest.java
```

**Test Coverage Target**: 90%+ for service layer

### Integration Tests
```
├── GLTransactionQueryIntegrationTest.java
│   - Test each GET endpoint with sample GL data
│   - Verify pagination/filtering works
│   - Verify authorization checks
└── FineractReportsIntegrationTest.java
    - Test each report with realistic loan portfolio
    - Verify calculations are correct
    - Verify authorization checks
```

---

## 📋 CHECKLIST FOR DEVELOPERS

### Before Implementing Report Services
- [ ] Read `/docs/GL_TRANSACTION_FLOW.md` (GL entry types)
- [ ] Read `/docs/FINERACT_REPORTS_STRUCTURE.md` (what each report shows)
- [ ] Read `/docs/GL_API_STRUCTURE.md` (API contract)
- [ ] Understand chart of accounts (accounts table structure)
- [ ] Understand lending-service loan APIs (for portfolio data)

### Before Submitting PR
- [ ] All 8 GL query endpoints return correct data
- [ ] All 10 report endpoints return correct calculations
- [ ] Pagination works correctly (limit, offset, totalCount)
- [ ] Filtering works (date range, status, type, loan)
- [ ] Tenant isolation enforced (no data leakage)
- [ ] Error handling tested (@SecuredEndpoint errors)
- [ ] Performance acceptable (< 1s for typical queries)
- [ ] All DTOs serialized correctly (JSON output valid)
- [ ] Swagger docs generated correctly
- [ ] Unit tests pass (90%+ coverage)
- [ ] Integration tests pass
- [ ] No hardcoded values (${ENV_VAR} pattern used)

---

## 📊 EFFORT ESTIMATE

| Phase | Task | Effort | Priority |
|-------|------|--------|----------|
| 1 | GL Query Repository Methods | 1 day | HIGH |
| 1 | JPA Query Methods | 1 day | HIGH |
| 1 | Repository Implementation | 2 days | HIGH |
| 1 | Controller Logic Wiring | 1 day | HIGH |
| 1 | Mapper Methods | 1 day | HIGH |
| **Phase 1 Total** | | **6 days** | **HIGH** |
| 2 | Trial Balance Report | 2 days | HIGH |
| 2 | Portfolio Summary Report | 2 days | HIGH |
| 2 | DPD Bucket Report | 2 days | HIGH |
| 2 | Collections Report | 1 day | HIGH |
| 2 | Profit Revenue Report | 1 day | HIGH |
| **Phase 2 Total** | | **8 days** | **HIGH** |
| 3 | Write-off Provisions | 1 day | MEDIUM |
| 3 | Cash Flow Report | 1 day | MEDIUM |
| 3 | Investor Report | 2 days | MEDIUM |
| 3 | Reconciliation Report | 1 day | MEDIUM |
| 3 | SAMA Regulatory Report | 2 days | MEDIUM |
| **Phase 3 Total** | | **7 days** | **MEDIUM** |
| **Testing** | Unit + Integration Tests | 3 days | ALL |
| **Documentation** | API docs, examples | 1 day | ALL |
| **GRAND TOTAL** | | **~28 days** (4-5 weeks) | |

---

## 🚀 RECOMMENDED EXECUTION ORDER

1. **Week 1**: Phase 1 (GL Query Infrastructure)
   - Day 1-2: Repository methods
   - Day 3-4: JPA queries + implementation
   - Day 5: Controller wiring + testing

2. **Week 2-3**: Phase 2 (Core Reports)
   - Day 6-8: Trial Balance + Portfolio
   - Day 9-10: DPD Buckets
   - Day 11-13: Collections + Profit (parallel)

3. **Week 4**: Phase 3 (Advanced) + Testing
   - Day 14-17: Advanced reports + tests
   - Day 18: Final testing + documentation

4. **Week 5**: Dashboard Integration (separate task)

---

## 📞 QUESTIONS / CLARIFICATIONS NEEDED

- [ ] Which products are active? (Murabaha? Ijara? Both?)
- [ ] Is investor syndication in scope for Investor Report?
- [ ] Should SAMA report be monthly or daily?
- [ ] How often should reports be cached? (Daily? On-demand?)
- [ ] Need real-time report generation or batch?
- [ ] Is Fineract API available for reconciliation testing?
- [ ] Any existing reporting system to migrate from?

---

**Last Updated**: 2026-04-16  
**Created By**: Claude Code  
**Status**: Ready for implementation

