# 💰 Prompt 05: Lending Service

**Objective**: Implement the Lending Service - core domain service for loan lifecycle management.

**Prerequisites**:
- ✅ All SDK prompts (01-08) complete
- ✅ Service template (00) complete
- ✅ Services 01-04 complete

---

## 📚 Reference Documents

**CRITICAL**: This is the CORE domain service. Read ALL references thoroughly.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
  - Section: Lending Service (detailed responsibilities)

- `/var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md`
  - ALL sections (Murabaha, Ijara, Tawarruq calculations)

- `/var/www/docs/islamic-financing/master-blueprint/10_KYC_CREDIT_DECISIONING.md`
  - Credit scoring
  - Approval workflows

- `/var/www/docs/islamic-financing/master-blueprint/16_LOAN_ORIGINATION_DISBURSEMENT.md`
  - Complete loan origination flow

- `/var/www/docs/islamic-financing/master-blueprint/17_LOAN_SERVICING_RESTRUCTURING.md`
  - Repayment processing
  - Early settlement

### ERD Documentation
- `/var/www/docs/islamic-financing/erd-docs/lending-service.sql`
  - **ALL tables**: loans, repayments, installments, etc.

### Product Specifications
- `/var/www/docs/islamic-financing/product-specifications/01_MURABAHA_PERSONAL_FINANCING.md`
- `/var/www/docs/islamic-financing/product-specifications/02_TAWARRUQ_CASH_FINANCING.md`
- `/var/www/docs/islamic-financing/product-specifications/03_IJARA_FINANCING.md`

### User Journeys
- `/var/www/docs/islamic-financing/user-journeys/01_INDIVIDUAL_BORROWER.md`
  - Complete customer journey

---

## 🎯 Implementation Requirements

### Service Responsibilities
1. **Loan Application**: Create, validate applications
2. **Credit Assessment**: Risk scoring, affordability checks
3. **Approval Workflow**: Multi-level approvals
4. **Sharia Compliance**: Sharia board approval
5. **Disbursement**: Fund transfer orchestration
6. **Repayment**: Process payments, update schedules
7. **Collections**: Overdue management
8. **Early Settlement**: Calculate and process early payoff

### Domain Model (Use domain-core-sdk)
- `Loan` - Aggregate root (from domain-core-sdk)
- `Repayment` - Value object
- `InstallmentSchedule` - Entity
- Use `MurabahaCalculator`, `IjarahCalculator` from domain-core-sdk

### Database Schema
Implement ALL tables from ERD:
- `loans`
- `loan_applications`
- `installment_schedules`
- `repayments`
- `early_settlements`
- `loan_collaterals`

### Temporal Workflows
- `LoanOriginationWorkflow` - End-to-end loan creation
  - Activity: ValidateApplication
  - Activity: PerformCreditCheck (calls Risk Service)
  - Activity: GetShariaApproval (calls Sharia Service)
  - Activity: CreateLoanInLMS (calls LMS Adapter)
  - Activity: DisburseFunds (calls Wallet Service)

- `LoanDisbursementWorkflow` - SAGA for disbursement
  - Step 1: Reserve funds in wallet
  - Step 2: Update loan status
  - Step 3: Transfer to customer account
  - Compensation on failure

- `RepaymentProcessingWorkflow` - Process payments
  - Activity: RecordPayment
  - Activity: UpdateSchedule
  - Activity: CheckOverdue

### REST API & gRPC
Design APIs based on use cases from reference documents:
- Loan application management endpoints
- Loan approval/rejection endpoints
- Disbursement endpoints
- Repayment recording endpoints
- Loan queries and reporting

**Note**: Design specific endpoints based on actual requirements, not predefined list.

### Business Rules (from references)
- Minimum loan amount: 5,000 SAR
- Maximum loan amount: 2,000,000 SAR
- Tenure: 6-60 months
- Profit rate: Product-dependent, max 50%
- Early settlement: Ibra (rebate) calculation
- Late payment: Charity penalty (NOT interest)

### Integration
- Calls: Risk Service, Sharia Service, Wallet Service, Product Service, LMS Adapter
- Consumes: `CustomerCreated`, `PaymentReceived`
- Publishes: `LoanApplicationCreated`, `LoanApproved`, `LoanRejected`, `LoanDisbursed`, `RepaymentReceived`, `LoanOverdue`

---

## 🧪 Testing Requirements

**Critical Tests**:
1. **Murabaha Calculation**: Verify against product spec examples
2. **Early Settlement**: Test Ibra calculations
3. **Charity Penalty**: Late payment penalties
4. **SAGA Compensation**: Disbursement rollback on failure
5. **Workflow Orchestration**: End-to-end loan origination
6. **Multi-tenancy**: Loans isolated per tenant

---

## ✅ Success Criteria

- [ ] All loan lifecycle states working
- [ ] Sharia calculations accurate (match product specs)
- [ ] Temporal workflows orchestrate loan processes
- [ ] SAGA compensation works on disbursement failure
- [ ] Early settlement calculates correctly
- [ ] Charity penalty (not interest) on late payments
- [ ] Multi-tenant isolation enforced
- [ ] LMS Adapter integration working
- [ ] All database migrations applied
- [ ] All tests pass (>80% coverage)
- [ ] gRPC API working
- [ ] REST API documented with OpenAPI

---

## 🔄 Next Step

After completing this service, proceed to:
- **Prompt 06**: Sharia Compliance Service
