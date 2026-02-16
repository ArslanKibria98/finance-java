# 📊 Prompt 07: Reporting & Projection SDK Implementation

**Objective**: Implement the `reporting-projection-sdk` for CQRS read models, projections, and analytics.

**Prerequisites**:
- ✅ Prompt 00-06 complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
  - Section: CQRS (Command Query Responsibility Segregation)

- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
  - Section: Database Strategy - Read models

- `/var/www/docs/islamic-financing/master-blueprint/15_DATABASE_STRATEGY.md`
  - Materialized views
  - Denormalized read models

---

## 🎯 Implementation Requirements

### Technologies
- **PostgreSQL**: 18.1 with materialized views
- **OpenSearch**: 2.x for full-text search (optional)

### Core Concept
**Event Sourcing + Projections**: Domain events → Projectors → Denormalized read models

### What to Implement

#### 1. Projector Base Class (in `projector/`)
- `EventProjector` - Base class for all projectors
  - Subscribe to domain events
  - Update read models
  - Handle projection errors
- `ProjectionStrategy` - Rebuild vs incremental update
- `ProjectionCheckpoint` - Track last processed event

#### 2. Read Model Repository (in `repository/`)
- `ReadModelRepository` - Base interface for read-only queries
- `LoanReadRepository` - Loan portfolio queries
  - `findActiveLoansByCustomer()`
  - `findOverdueLoans()`
  - `getPortfolioSummary()`
  - `getLoanAnalytics()`
- `CustomerReadRepository` - Customer analytics
- `PaymentReadRepository` - Payment history

#### 3. Read Models (in `readmodel/`)
Denormalized for fast queries:
- `LoanSummaryReadModel` - Loan overview
  - All loan fields flattened
  - Customer name (denormalized)
  - Product name (denormalized)
  - Computed fields (days overdue, outstanding principal)
- `PortfolioReadModel` - Portfolio analytics
  - Total outstanding
  - PAR (Portfolio at Risk)
  - NPL ratio
- `CustomerPortfolioReadModel` - Per-customer summary

#### 4. Projectors (in `projector/`)
Implement projectors that listen to events:
- `LoanProjector` - Projects loan events to `LoanSummaryReadModel`
  - On `LoanApproved` → Insert row
  - On `LoanDisbursed` → Update status
  - On `PaymentReceived` → Update outstanding amount
- `PortfolioProjector` - Aggregates portfolio metrics
- `CustomerProjector` - Customer-level aggregations

#### 5. Views (in `view/`)
Materialized views for analytics:
- `LoanPortfolioView` - SQL materialized view
- `OverdueLoansView` - Overdue loans with customer details
- `RevenueView` - Monthly/quarterly revenue

Refresh strategy:
- **Incremental**: Update on each event
- **Scheduled**: Refresh hourly for heavy aggregations

#### 6. Configuration (in `config/`)
- `ProjectionConfig` - Projector settings
- `ReadModelDataSourceConfig` - Separate read DB connection pool
- `MaterializedViewConfig` - Refresh schedules

---

## 🧪 Testing Requirements

- Test projectors update read models on events
- Test read model queries return denormalized data
- Test materialized view refresh
- Test projection rebuilding from event log
- Test query performance (< 100ms for typical queries)

---

## ✅ Success Criteria

- [ ] Projectors subscribe to Kafka domain events
- [ ] Read models update in real-time (< 5 sec latency)
- [ ] Queries are fast (denormalized, indexed)
- [ ] Materialized views refresh on schedule
- [ ] Projection checkpoint tracks progress
- [ ] Read models can be rebuilt from event log
- [ ] Tests pass: `mvn test -pl shared-libraries/reporting-projection-sdk`
- [ ] Build succeeds: `mvn clean install -pl shared-libraries/reporting-projection-sdk`

---

## 🔄 Next Step

After completing this SDK, proceed to:
- **Prompt 08**: `test-harness-sdk` implementation
