# 🏦 Prompt 19: Core Banking Adapter

**Objective**: Implement the Core Banking Adapter for Apache Fineract integration.

**Prerequisites**: ✅ SDK 01-08 (especially lms-adapter-sdk) + Template + Services 01-18

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/07_CORE_BANKING_ADAPTER.md` - **Primary reference (complete implementation guide)**
- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`

---

## 🎯 Implementation Requirements

### Responsibilities
This service is a **thin adapter** around `lms-adapter-sdk`. It:
- Exposes REST API for other services
- Implements Temporal activities for workflows
- Handles retries and error mapping

### Architecture
```
Lending Service → Core Banking Adapter → lms-adapter-sdk → Fineract API
```

### REST API (delegates to lms-adapter-sdk)
- POST `/api/v1/core-banking/loans` - Create loan in Fineract
- POST `/api/v1/core-banking/loans/{id}/approve` - Approve loan
- POST `/api/v1/core-banking/loans/{id}/disburse` - Disburse
- POST `/api/v1/core-banking/loans/{id}/repayments` - Record payment
- GET `/api/v1/core-banking/loans/{id}` - Get loan details

### Temporal Activities
Implement activities that use lms-adapter-sdk:
- `CreateLoanInLmsActivity`
- `ApproveLoanInLmsActivity`
- `DisburseLoanInLmsActivity`
- `RecordRepaymentInLmsActivity`

### Integration
- Uses: `lms-adapter-sdk` (all Fineract logic in SDK)
- Called by: Lending Service workflows
- Publishes: `LoanCreatedInLMS`, `DisbursementCompletedInLMS`

---

## ✅ Success Criteria
- [ ] All CRUD operations working
- [ ] Temporal activities implemented
- [ ] Error handling converts Fineract errors
- [ ] Retry logic uses SDK retry policies
- [ ] Integration tests with Fineract mock

---

## 🔄 Next: **Prompt 20** - Payment Adapter
