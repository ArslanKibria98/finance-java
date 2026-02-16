# 🔍 Prompt 03: KYC Orchestrator Service

**Objective**: Implement the KYC Orchestrator service for customer verification workflows.

**Prerequisites**:
- ✅ All SDK prompts (01-08) complete
- ✅ Service template (00) complete

---

## 📚 Reference Documents

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
  - Section: KYC Orchestrator definition

- `/var/www/docs/islamic-financing/master-blueprint/10_KYC_CREDIT_DECISIONING.md`
  - KYC workflow steps
  - Verification levels

- `/var/www/docs/islamic-financing/master-blueprint/08_KSA_GOVERNMENT_APIS.md`
  - Nafath integration
  - Absher integration

### ERD Documentation
- `/var/www/docs/islamic-financing/erd-docs/kyc-adapter.sql`

### User Journeys
- `/var/www/docs/islamic-financing/user-journeys/01_INDIVIDUAL_BORROWER.md`
  - Section: KYC verification flow

---

## 🎯 Implementation Requirements

### Service Responsibilities
1. **Orchestrate KYC Workflow**: Multi-step verification via Temporal
2. **Identity Verification**: Nafath authentication
3. **Document Verification**: ID card, address proof
4. **Liveness Check**: Facial recognition
5. **Risk Scoring**: AML/CFT checks

### Temporal Workflows
- `KycVerificationWorkflow` - Main orchestration
  - Step 1: Nafath authentication
  - Step 2: Document upload & OCR
  - Step 3: Liveness check
  - Step 4: Simah credit bureau check
  - Step 5: AML screening
  - Step 6: Risk scoring

### Integration
- Calls: Nafath API, Simah API, KYC Adapter
- Consumes: `CustomerCreated`
- Publishes: `KycStarted`, `KycCompleted`, `KycFailed`, `DocumentVerified`

### REST API & gRPC
Design APIs for:
- KYC workflow initiation
- KYC status monitoring
- Document submission
- Workflow retry mechanisms

**Note**: Design based on KYC levels and user journeys from reference documents.

### KYC Levels
- **Level 1**: Nafath + basic info
- **Level 2**: Level 1 + document verification
- **Level 3**: Level 2 + liveness + AML

---

## 🧪 Testing Requirements
- Test workflow with mocked external APIs
- Test retry logic on failures
- Test partial completion (save progress)
- Test timeout handling (30-day workflow limit)

---

## ✅ Success Criteria
- [ ] Temporal workflow orchestrates all KYC steps
- [ ] Nafath integration works
- [ ] Document OCR extracts data correctly
- [ ] Risk scoring algorithm implemented
- [ ] All KYC levels supported
- [ ] All tests pass

---

## 🔄 Next Step
After completing this service, proceed to:
- **Prompt 04**: Identity Federation service
