# ☪️ Prompt 06: Sharia Compliance Service

**Objective**: Implement the Sharia Compliance Service for Islamic finance validation and approvals.

**Prerequisites**:
- ✅ All SDK prompts (01-08) complete
- ✅ Service template (00) complete
- ✅ Services 01-05 complete

---

## 📚 Reference Documents

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md`
  - **ALL sections** - This is the primary reference

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
  - Section: Sharia Compliance Service definition

### Product Specifications
- ALL product specs - validate each product's Sharia compliance

---

## 🎯 Implementation Requirements

### Service Responsibilities
1. **Pre-Approval Validation**: Check loan structure is Sharia-compliant
2. **Sharia Board Approval**: Route to Sharia scholars for review
3. **Profit Rate Validation**: Ensure rates are fair and compliant
4. **Contract Generation**: Generate Sharia-compliant contracts (Arabic)
5. **Periodic Audit**: Audit all loans for ongoing compliance
6. **Fatwa Management**: Store and apply Sharia rulings

### Database Schema
- `sharia_approvals` - Approval records
- `sharia_board_members` - Scholars
- `fatwas` - Sharia rulings
- `compliance_audits` - Periodic audits
- `prohibited_transactions` - Riba detection

### Domain Model
- Use `ShariaMathEngine` from domain-core-sdk
- Implement validation rules
- Implement contract templates

### REST API & gRPC
Design APIs for:
- Sharia compliance validation
- Sharia board approval workflows
- Compliance audit triggers
- Fatwa management

**Note**: Design based on Sharia compliance requirements from reference documents.

### Temporal Workflows
- `ShariaApprovalWorkflow` - Route to scholars, wait for decision
  - Human task: Scholar reviews contract
  - Timeout: 7 days
  - Escalation: If no response

### Validation Rules (from references)
- **Riba Check**: No interest-based calculations
- **Gharar Check**: No excessive uncertainty
- **Asset Backing**: Murabaha must have real asset
- **Ownership**: Lender must own asset before selling (Murabaha)
- **Profit Rate Fairness**: Not exploitative
- **Charity Penalty**: Late fees go to charity, not lender

### Integration
- Consumes: `LoanApplicationCreated` (validate before approval)
- Publishes: `ShariaApprovalGranted`, `ShariaApprovalRejected`, `ComplianceViolationDetected`

---

## 🧪 Testing Requirements
- Test Riba detection (reject interest-based loans)
- Test Gharar detection (excessive uncertainty)
- Test profit rate validation
- Test Sharia board approval workflow
- Test contract generation (Arabic text)

---

## ✅ Success Criteria
- [ ] All Sharia validation rules implemented
- [ ] Sharia board approval workflow working
- [ ] Contracts generated in Arabic
- [ ] Riba detection prevents non-compliant loans
- [ ] Audit log tracks all approvals
- [ ] All tests pass

---

## 🔄 Next Step
After completing this service, proceed to:
- **Prompt 07**: Ledger Service
