# 🔍 Prompt 17: Audit Service

**Objective**: Implement the Audit Service for SAMA-compliant audit logging (7-year retention).

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-16

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md` - SAMA requirements
- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/erd-docs/audit-service.sql`

---

## 🎯 Implementation Requirements

### Responsibilities
- Immutable audit log (event sourcing)
- 7-year retention (SAMA requirement)
- User action tracking
- Data access logging (PII access)
- Compliance reporting

### Database Schema
- `audit_events` - Immutable append-only table
- Partitioned by year for 7-year retention

### What to Audit
- All API calls (request/response)
- All database changes (who, what, when)
- All login attempts
- All PII access
- All loan approvals/rejections
- All payment transactions

### REST API & gRPC
Design APIs for:
- Audit event recording
- Audit log queries (with filters)
- User activity reports
- Entity change history
- Compliance reporting

**Note**: All queries must be read-only. Only AUDITOR role can access. Design based on SAMA compliance requirements.

### Audit Event Structure
```json
{
  "eventId": "uuid",
  "timestamp": "ISO8601",
  "userId": "user-123",
  "tenantId": "tenant-001",
  "action": "LOAN_APPROVED",
  "entityType": "Loan",
  "entityId": "loan-456",
  "changes": {...},
  "ipAddress": "1.2.3.4",
  "userAgent": "..."
}
```

### Integration
- Consumes: ALL domain events (audit everything)
- Publishes: (none - audit is terminal)

### Compliance
- **Immutability**: No UPDATE or DELETE on audit_events
- **Retention**: 7 years (2,555 days)
- **Encryption**: Encrypt at rest
- **Access Control**: Only AUDITOR role can query

---

## ✅ Success Criteria
- [ ] All domain events audited
- [ ] Audit log immutable (no updates)
- [ ] 7-year retention enforced
- [ ] Query performance acceptable (indexed)
- [ ] SAMA compliance verified

---

## 🔄 Next: **Prompt 18** - KYC Adapter
