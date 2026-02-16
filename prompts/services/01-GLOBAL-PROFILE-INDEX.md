# 🌍 Prompt 01: Global Profile Index Service

**Objective**: Implement the Global Profile Index service for cross-tenant customer profile management.

**Prerequisites**:
- ✅ All SDK prompts (01-08) complete
- ✅ Service template (00) complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
  - Section: Global Profile Index service definition

- `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md`
  - Multi-tenancy isolation
  - Data residency rules

### ERD Documentation
- `/var/www/docs/islamic-financing/erd-docs/global-profile-index.sql`
  - Complete database schema

### Implementation Guide
- `/var/www/docs/islamic-financing/GLOBAL_CRM_IMPLEMENTATION_GUIDE.md`
  - Detailed implementation steps

---

## 🎯 Implementation Requirements

### Service Responsibilities
1. **Global Customer Index**: Map national_id → customer_id across tenants
2. **Cross-Tenant Deduplication**: Prevent same person from multiple accounts per tenant
3. **Consent Management**: Track data sharing consent
4. **Profile Linking**: Link profiles across subsidiaries

### Database Schema
Tables to implement (from ERD):
- `global_profile_index` - Main index table
- `profile_links` - Cross-tenant profile links
- `consent_records` - PDPL consent tracking

### REST API & gRPC
Design APIs based on requirements:
- Profile creation and management
- Cross-tenant profile queries
- Consent management
- Profile deduplication checks

**Note**: Design specific endpoints at implementation time based on actual use cases from reference documents.

### Temporal Workflows
- `ProfileCreationWorkflow` - Create profile with deduplication check
- `ConsentManagementWorkflow` - Handle consent lifecycle

### Domain Model
Key aggregates:
- `GlobalProfile` - Aggregate root
- `ConsentRecord` - Value object
- `ProfileLink` - Entity

### Integration
- Publishes events: `GlobalProfileCreated`, `ConsentGranted`, `ConsentRevoked`
- Consumes events: `CustomerCreated` (from Customer Service)

---

## 🧪 Testing Requirements

- Test deduplication logic (same national_id)
- Test cross-tenant profile linking
- Test consent management (grant, revoke, query)
- Test data residency enforcement (KSA only)

---

## ✅ Success Criteria

- [ ] Global profile index working
- [ ] Deduplication prevents duplicate national IDs per tenant
- [ ] Consent tracking complies with PDPL
- [ ] Cross-tenant queries work
- [ ] Database migrations apply successfully
- [ ] All tests pass
- [ ] Service starts and integrates with ELK logging
- [ ] Kubernetes deployment successful

---

## 🔄 Next Step

After completing this service, proceed to:
- **Prompt 02**: PII Vault service
