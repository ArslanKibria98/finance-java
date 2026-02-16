# 📝 Implementation Prompts - KSA Islamic Financing Platform

This directory contains **implementation prompts** for each SDK and microservice. Each prompt references the master blueprint and lets the implementing agent decide on the details.

---

## 📂 Directory Structure

```
prompts/
├── README.md                          # This file
├── 00-INITIAL-SETUP.md                # ✅ Initial directory structure
│
├── sdks/                              # SDK Implementation Prompts
│   ├── 01-FOUNDATIONAL-INFRA-SDK.md   # ✅ Security, Logging, Resilience
│   ├── 02-DOMAIN-CORE-SDK.md          # ✅ Sharia Math, Entities, Value Objects
│   ├── 03-MESSAGING-EVENT-SDK.md      # ✅ Kafka, gRPC, Avro
│   ├── 04-WORKFLOW-ORCHESTRATION-SDK.md # ✅ Temporal, SAGA
│   ├── 05-LMS-ADAPTER-SDK.md          # ✅ Fineract Abstraction
│   ├── 06-COMPLIANCE-LOCALIZATION-SDK.md # ✅ ZATCA, SAMA, Hijri
│   ├── 07-REPORTING-PROJECTION-SDK.md # ✅ CQRS Read Models
│   └── 08-TEST-HARNESS-SDK.md         # ✅ Testing Utilities
│
├── services/                          # Service Implementation Prompts
│   ├── 00-SERVICE-TEMPLATE.md         # ✅ Master template
│   │
│   ├── 01-GLOBAL-PROFILE-INDEX.md     # ✅ Global CRM
│   ├── 02-PII-VAULT.md                # ✅
│   ├── 03-KYC-ORCHESTRATOR.md         # ✅
│   ├── 04-IDENTITY-FEDERATION.md      # ✅
│   │
│   ├── 05-LENDING-SERVICE.md          # ✅ Core Domain
│   ├── 06-SHARIA-COMPLIANCE.md        # ✅
│   ├── 07-LEDGER-SERVICE.md           # ✅
│   ├── 08-CUSTOMER-SERVICE.md         # ✅
│   ├── 09-PRODUCT-SERVICE.md          # ✅
│   ├── 10-WALLET-SERVICE.md           # ✅
│   │
│   ├── 11-RISK-SERVICE.md             # ✅ Supporting
│   ├── 12-CREDIT-DECISIONING.md       # ✅
│   ├── 13-COLLECTIONS.md              # ✅
│   ├── 14-DOCUMENT-SERVICE.md         # ✅
│   ├── 15-PARTNER-SERVICE.md          # ✅
│   │
│   ├── 16-NOTIFICATION-SERVICE.md     # ✅ Generic
│   ├── 17-AUDIT-SERVICE.md            # ✅
│   │
│   ├── 18-KYC-ADAPTER.md              # ✅ Adapters
│   ├── 19-CORE-BANKING-ADAPTER.md     # ✅
│   └── 20-PAYMENT-ADAPTER.md          # ✅
│
└── infrastructure/                    # Infrastructure Setup Prompts
    ├── 01-DOCKER-COMPOSE.md           # ✅ Local development stack
    ├── 02-KUBERNETES-ELK.md           # ✅ ELK Stack deployment
    ├── 03-KUBERNETES-MONITORING.md    # ✅ Prometheus, Grafana, Jaeger
    └── 04-KUBERNETES-SERVICES.md      # ✅ Service deployments
```

---

## 🎯 Implementation Order

Execute prompts in this order (dependencies matter):

### Phase 1: Foundation ✅ COMPLETE
1. **00-INITIAL-SETUP.md** - Create directory structure

### Phase 2: SDKs ✅ COMPLETE
2. **01-FOUNDATIONAL-INFRA-SDK.md** - Base for all other SDKs
3. **02-DOMAIN-CORE-SDK.md** - Pure domain logic
4. **03-MESSAGING-EVENT-SDK.md** - Kafka, gRPC
5. **04-WORKFLOW-ORCHESTRATION-SDK.md** - Temporal
6. **05-LMS-ADAPTER-SDK.md** - Fineract integration
7. **06-COMPLIANCE-LOCALIZATION-SDK.md** - ZATCA, SAMA
8. **07-REPORTING-PROJECTION-SDK.md** - CQRS read models
9. **08-TEST-HARNESS-SDK.md** - Testing utilities

### Phase 3: Service Template ✅ COMPLETE
10. **00-SERVICE-TEMPLATE.md** - Master service template

### Phase 4: Microservices ✅ COMPLETE
11-30. All 20 microservice prompts

### Phase 5: Infrastructure ✅ COMPLETE
31-34. Infrastructure setup prompts

---

## 📋 Prompt Format

Each prompt follows this structure:

### Header
- **Objective**: What will be built
- **Prerequisites**: What must be completed first

### Reference Documents Section
- Lists all relevant master blueprint documents
- Lists ERD documentation
- Lists product specifications
- **Agent must read these before implementation**

### Implementation Requirements
- High-level requirements
- Technologies and versions
- What to implement (package/class list)
- Core principles to follow

### Testing Requirements
- What tests to write
- Testing strategies

### Success Criteria
- Verification checklist
- Build/test commands

### Next Step
- Which prompt to execute next

---

## 🔧 How to Use These Prompts

### For AI Agent Implementation

1. **Read the prompt carefully**
2. **Read ALL reference documents** mentioned in the prompt
3. **Understand the requirements** from the reference docs
4. **Implement** based on your understanding
5. **Verify** with success criteria
6. **Move to next prompt**

Example:
```
Agent: Read prompts/sdks/02-DOMAIN-CORE-SDK.md
Agent: Read /var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md
Agent: Read /var/www/docs/islamic-financing/erd-docs/lending-service.md
Agent: Read /var/www/docs/islamic-financing/product-specifications/01_MURABAHA_PERSONAL_FINANCING.md
Agent: Implement based on understanding
Agent: Run: mvn test -pl shared-libraries/domain-core-sdk
Agent: Proceed to next prompt
```

### For Manual Implementation

1. Open the prompt file
2. Read all reference documents
3. Implement step by step
4. Verify with success criteria
5. Move to next prompt

---

## 📊 Progress Tracking

| Category | Total | Complete | Status |
|----------|-------|----------|--------|
| **Initial Setup** | 1 | 1 | ✅ 100% |
| **SDK Prompts** | 8 | 8 | ✅ 100% |
| **Service Template** | 1 | 1 | ✅ 100% |
| **Microservice Prompts** | 20 | 20 | ✅ 100% |
| **Infrastructure Prompts** | 4 | 4 | ✅ 100% |
| **TOTAL** | **34** | **34** | ✅ **100%** |

---

## 📖 Reference Documents Location

All prompts reference documents at:
- **Master Blueprint**: `/var/www/docs/islamic-financing/master-blueprint/*.md`
- **ERD Documentation**: `/var/www/docs/islamic-financing/erd-docs/*.md`
- **Product Specs**: `/var/www/docs/islamic-financing/product-specifications/*.md`
- **User Journeys**: `/var/www/docs/islamic-financing/user-journeys/*.md`

---

## ✅ Quality Standards

Each prompt ensures:
1. **Reference-based**: Points to master blueprint for details
2. **Self-contained**: Can be executed independently (after prerequisites)
3. **Verification**: Includes success criteria checklist
4. **Best Practices**: Follows architecture principles
5. **Testing**: Includes test requirements
6. **Documentation**: JavaDoc/inline comments expected

---

## 🚀 Quick Start

To begin implementation:

```bash
# 1. Initial setup (if not done)
cd /var/www/islamic-financing-platform
cat prompts/00-INITIAL-SETUP.md

# 2. Implement SDKs in order
cat prompts/sdks/01-FOUNDATIONAL-INFRA-SDK.md
# Read reference documents
# Implement
# Verify

# 3. Continue with remaining SDKs...
```

---

## 📝 Notes

- **Reference documents are mandatory**: Don't skip reading them
- **Order matters**: Follow the dependency chain
- **Verify incrementally**: Build and test after each prompt
- **Ask questions**: If reference docs are unclear, ask for clarification

---

## 🎉 STATUS: ALL 34 PROMPTS COMPLETE!

**Ready For**: Implementation by another agent or development team.
**Total Prompts**: 34 (1 initial + 8 SDKs + 1 template + 20 services + 4 infrastructure)
**Estimated Code**: ~91,000 lines when fully implemented
