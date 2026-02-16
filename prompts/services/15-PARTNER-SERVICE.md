# 🤝 Prompt 15: Partner Service

**Objective**: Implement the Partner Service for merchant/dealer partnership management.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-14

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/erd-docs/partner-service.sql`

---

## 🎯 Implementation Requirements

### Responsibilities
- Partner onboarding (merchants, car dealers)
- Commission management
- Partner settlements
- API key management for partners
- Partner portal access

### Database Schema
- `partners`, `partner_agreements`, `commissions`, `settlements`

### Partner Types
- AUTO_DEALER - Car dealerships
- MERCHANT - General merchants
- REAL_ESTATE_DEVELOPER - Property developers

### REST API & gRPC
Design APIs for:
- Partner onboarding and management
- Agreement management
- Commission tracking
- Settlement processing
- Partner portal integration (for partners to submit applications)

**Note**: Separate internal APIs from partner-facing APIs for security.

### Integration
- Consumes: `LoanDisbursed` (calculate commission)
- Publishes: `PartnerOnboarded`, `CommissionEarned`, `SettlementCompleted`

---

## ✅ Success Criteria
- [ ] Partner onboarding working
- [ ] Commission calculation automatic
- [ ] Settlement processing
- [ ] Partner API secured with API keys
- [ ] Partner portal access

---

## 🔄 Next: **Prompt 16** - Notification Service
