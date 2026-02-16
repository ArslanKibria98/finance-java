# 🔌 Prompt 18: KYC Adapter

**Objective**: Implement the KYC Adapter for integrating external KYC/AML services (Simah, Nafath, etc.).

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-17

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/08_KSA_GOVERNMENT_APIS.md` - **Primary reference**
- `/var/www/docs/islamic-financing/master-blueprint/10_KYC_CREDIT_DECISIONING.md`
- `/var/www/docs/islamic-financing/erd-docs/kyc-adapter.sql`

---

## 🎯 Implementation Requirements

### Responsibilities
- Nafath authentication integration
- Simah credit bureau integration
- Absher data verification
- AML screening (third-party)
- Sanctions list checking

### Integrations

#### 1. Nafath (National Authentication)
- Endpoint: Nafath API
- Flow: Generate QR → User scans → Callback with verification
- Response: National ID, Name, DOB verified

#### 2. Simah (Credit Bureau)
- Endpoint: Simah SOAP API
- Request: National ID
- Response: Credit score, existing debt, payment history

#### 3. Absher (Government Portal)
- Endpoint: Absher API
- Verify: Employment, salary, address

### REST API & gRPC
Design APIs for:
- Nafath authentication workflow
- Simah credit report retrieval
- Absher data verification
- AML screening operations
- Sanctions list checking

**Note**: Design specific endpoints at implementation time based on KSA government API specifications and KYC requirements from reference documents.

### Database Schema
- `kyc_requests`, `simah_reports`, `nafath_sessions`, `aml_results`

### Integration
- Called by: KYC Orchestrator Service
- Publishes: `NafathVerified`, `SimahReportReceived`, `AmlCheckCompleted`

---

## ✅ Success Criteria
- [ ] Nafath integration working (test with sandbox)
- [ ] Simah integration working (test with sandbox)
- [ ] AML screening implemented
- [ ] Error handling for API failures
- [ ] Retry logic with exponential backoff

---

## 🔄 Next: **Prompt 19** - Core Banking Adapter
