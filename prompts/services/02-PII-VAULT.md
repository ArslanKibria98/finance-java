# 🔐 Prompt 02: PII Vault Service

**Objective**: Implement the PII Vault service for encrypted storage of personally identifiable information.

**Prerequisites**:
- ✅ All SDK prompts (01-08) complete
- ✅ Service template (00) complete

---

## 📚 Reference Documents

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md`
  - Section: PII encryption and tokenization
  - PDPL compliance requirements

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
  - Section: PII Vault service definition

### ERD Documentation
- `/var/www/docs/islamic-financing/erd-docs/pii-vault.sql`

---

## 🎯 Implementation Requirements

### Service Responsibilities
1. **Encrypt & Store PII**: Name, email, phone, address
2. **Tokenization**: Return tokens for stored PII
3. **Decryption**: Retrieve PII by token (with authorization)
4. **Key Rotation**: Support encryption key rotation
5. **Audit Logging**: Log all PII access

### Database Schema
- `pii_records` - Encrypted PII storage
- `pii_tokens` - Token → encrypted_record mapping
- `encryption_keys` - Key version management

### Encryption
- **Algorithm**: AES-256-GCM
- **Key Management**: Rotate keys every 90 days
- **At-rest**: All PII encrypted in database
- **In-transit**: TLS 1.3

### REST API & gRPC
Design APIs for:
- PII storage with tokenization
- PII retrieval by token (with authorization checks)
- Bulk PII operations
- Key rotation triggers

**Note**: Design specific endpoints at implementation time based on security requirements.

### Integration
- Consumes: `CustomerCreated` (store PII)
- Publishes: `PiiStored`, `PiiAccessed`, `KeyRotated`

---

## 🧪 Testing Requirements
- Test encryption/decryption correctness
- Test key rotation (re-encrypt all records)
- Test unauthorized access prevention
- Test audit logging on all PII access

---

## ✅ Success Criteria
- [ ] All PII encrypted with AES-256-GCM
- [ ] Tokens generated and retrievable
- [ ] Key rotation works without data loss
- [ ] Audit logs capture all access
- [ ] PDPL compliance verified
- [ ] All tests pass

---

## 🔄 Next Step
After completing this service, proceed to:
- **Prompt 03**: KYC Orchestrator service
