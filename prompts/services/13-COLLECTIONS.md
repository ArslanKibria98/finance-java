# 📞 Prompt 13: Collections Service

**Objective**: Implement the Collections Service for overdue loan management and recovery.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-12

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/master-blueprint/17_LOAN_SERVICING_RESTRUCTURING.md` - Restructuring rules
- `/var/www/docs/islamic-financing/erd-docs/collections-service.sql`

---

## 🎯 Implementation Requirements

### Responsibilities
- Overdue detection (daily job)
- Collections workflow (automated reminders → calls → legal)
- Restructuring proposals
- Write-off management
- Recovery tracking

### Database Schema
- `collections_cases`, `collection_actions`, `restructuring_proposals`, `write_offs`

### Collections Stages (from references)
- **Stage 1 (1-30 days)**: Automated SMS/email reminders
- **Stage 2 (31-60 days)**: Phone calls by collectors
- **Stage 3 (61-90 days)**: Field visits
- **Stage 4 (91+ days)**: Legal action / Write-off

### Temporal Workflows
- `CollectionsWorkflow` - Multi-stage collections process
  - Send reminder (Day 1, 7, 14, 21, 28)
  - Assign to collector (Day 31)
  - Field visit (Day 61)
  - Legal referral (Day 91)

### REST API & gRPC
Design APIs for:
- Overdue loan queries
- Collections case management
- Collection action recording
- Restructuring proposals
- Write-off processing

**Note**: Design based on collections stages and workflows from reference documents.

### Integration
- Consumes: `PaymentOverdue` (from Lending Service)
- Calls: Notification Service (send reminders)
- Publishes: `CollectionCaseCreated`, `LoanRestructured`, `LoanWrittenOff`

---

## ✅ Success Criteria
- [ ] Overdue detection working (daily cron)
- [ ] Collections workflow automated
- [ ] Restructuring proposals working
- [ ] Write-off process implemented
- [ ] PAR (Portfolio at Risk) calculation

---

## 🔄 Next: **Prompt 14** - Document Service
