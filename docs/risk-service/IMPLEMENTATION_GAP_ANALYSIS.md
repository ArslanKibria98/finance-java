# Risk & Fraud Service - Implementation Gap Analysis
> **Source Documents**: 
> - Risk Assessment Service HLD-v1.pdf (FV-HLD-RISK-002)
> - Finova Fraud Monitoring HLD v1.docx
>
> **Last Updated**: 2026-04-16
> **Status**: COMPREHENSIVE GAP ANALYSIS

---

## EXECUTIVE SUMMARY

| Category | Status | % Complete | Notes |
|----------|--------|-----------|-------|
| **Internal Fraud Checks** | ✅ IMPLEMENTED | 100% | 9-step pipeline fully working |
| **AML Risk Scoring** | ✅ IMPLEMENTED | 100% | EastNets model, category-weighted scoring |
| **Blacklist Management** | ✅ IMPLEMENTED | 100% | NID + Mobile blacklist |
| **Credit Scoring** | ✅ IMPLEMENTED | 100% | Field definitions, eligibility evaluation |
| **Tenant Management (Admin Portal)** | ❌ MISSING | 0% | Self-service config interface needed |
| **LOV (List of Values) Management** | ⚠️ PARTIAL | 40% | Basic tables exist, but admin UI missing |
| **Risk Parameter Management** | ⚠️ PARTIAL | 30% | Basic domain model, but admin UI missing |
| **Answer History & Audit Trail** | ❌ MISSING | 0% | No version control, no delta tracking |
| **Assessment Session Lifecycle** | ⚠️ PARTIAL | 50% | Core states exist, some transitions missing |
| **Score Explainability** | ❌ MISSING | 0% | No breakdown showing parameter contribution |
| **Third-Party Integration Audit** | ⚠️ PARTIAL | 50% | AML checks done, but audit logging missing |
| **Admin Configuration Portal** | ❌ MISSING | 0% | CRITICAL - Tenants cannot self-configure |

**Overall Completion**: ~45%

---

## DETAILED GAP ANALYSIS

### ✅ FULLY IMPLEMENTED (4/11 Categories)

#### 1. **Internal Fraud Checks** (FR-C: Compliance Checks)
**Status**: FULLY WORKING ✓

```
Location: services/risk-service/src/main/java/com/ksa/financing/risk/
├── application/usecase/RunInternalChecksService.java          ✓
├── infrastructure/check/
│   ├── NidFormatValidatorImpl.java                           ✓
│   ├── BlacklistWatchlistCheckImpl.java                      ✓
│   ├── FraudHistoryCheckImpl.java                            ✓
│   ├── VelocityCheckImpl.java                                ✓
│   ├── CifLookupCheckImpl.java                               ✓
│   ├── DuplicateMobileCheckImpl.java                         ✓
│   ├── AccountLockCheckImpl.java                             ✓
│   └── InternalSanctionsCheckImpl.java                       ✓
└── adapter/rest/controller/InternalChecksController.java    ✓
```

**Endpoint**: `POST /api/v1/risk/internal-checks`
- ✓ 9-step sequential pipeline
- ✓ Signal logging (fraud_signal_logs table)
- ✓ Severity assignment
- ✓ Decision output (APPROVE/REVIEW/BLOCK)

---

#### 2. **AML Risk Scoring** (FR-S: Scoring Engine - Partial)
**Status**: FULLY WORKING ✓

```
Location: services/risk-service/src/main/java/com/ksa/financing/risk/
├── domain/model/aml/
│   ├── AmlRiskScore.java                                    ✓
│   ├── AmlScoringInput.java                                 ✓
│   ├── AmlRiskCategory.java                                 ✓
│   ├── AmlRiskCategoryFactor.java                           ✓
│   ├── AmlRiskThreshold.java                                ✓
│   ├── AmlCategoryScoreBreakdown.java                       ✓
│   └── AmlRiskScoringEngine.java (Domain Service)           ✓
├── application/usecase/CalculateAmlRiskScoreService.java    ✓
└── adapter/rest/controller/AmlRiskScoreController.java      ✓
```

**Endpoint**: `POST /api/v1/risk/aml-score`
- ✓ Category-weighted scoring formula
- ✓ Factor weights (0-100)
- ✓ Risk levels (LOW/MEDIUM/HIGH/CRITICAL)
- ✓ EastNets reference data (occupations, cities, FATF)
- ✓ Risk thresholds (configurable per tenant)

**Implemented Categories**:
- ✓ Geopolitical Risk (FATF list)
- ✓ Occupation Risk
- ✓ Age/Income Risk
- ✓ Source of Wealth Risk
- ✓ PEP/Sanctions override

---

#### 3. **Blacklist Management** (FR-L: LOV partial)
**Status**: FULLY WORKING ✓

```
Location: services/risk-service/src/main/java/com/ksa/financing/risk/
├── domain/model/
│   ├── NidBlacklistEntry.java                               ✓
│   └── MobileBlacklistEntry.java                            ✓
├── infrastructure/persistence/
│   ├── BlacklistRepositoryImpl.java                          ✓
│   └── entity/
│       ├── NidBlacklistJpaEntity.java                       ✓
│       └── MobileBlacklistJpaEntity.java                    ✓
└── adapter/rest/controller/BlacklistController.java         ✓
```

**Endpoints**:
- ✓ `POST /api/v1/risk/blacklist/nid` — Add NID to blacklist
- ✓ `GET /api/v1/risk/blacklist/nid/{nid}` — Check NID
- ✓ `DELETE /api/v1/risk/blacklist/nid/{nid}` — Remove NID
- ✓ `POST /api/v1/risk/blacklist/mobile` — Add mobile
- ✓ `GET /api/v1/risk/blacklist/mobile/{mobile}` — Check mobile
- ✓ `DELETE /api/v1/risk/blacklist/mobile/{mobile}` — Remove mobile

---

#### 4. **Credit Scoring** (FR-S: partial)
**Status**: FULLY WORKING ✓

```
Location: services/risk-service/src/main/java/com/ksa/financing/risk/
├── domain/model/credit/
│   ├── CreditScoringFieldDefinition.java                    ✓
│   ├── CreditScoringCriteria.java                           ✓
│   ├── CreditScoringRule.java                               ✓
│   └── CreditScoringOperator.java                           ✓
├── domain/service/CreditScoringDecisionEngine.java          ✓
├── application/usecase/
│   ├── ManageCreditScoringService.java                      ✓
│   ├── GetCreditScoringFieldsService.java                   ✓
│   └── EvaluateEligibilityService.java                      ✓
└── adapter/rest/controller/CreditScoringController.java     ✓
```

**Endpoints**:
- ✓ `GET /api/v1/risk/credit-scoring/fields` — Get field definitions
- ✓ `POST /api/v1/risk/credit-scoring/criteria` — Create scoring criteria
- ✓ `POST /api/v1/risk/credit-scoring/evaluate` — Evaluate eligibility

---

### ⚠️ PARTIALLY IMPLEMENTED (3/11 Categories)

#### 5. **LOV (List of Values) Management** (FR-L: 40% complete)
**Status**: PARTIALLY WORKING ⚠️

**What EXISTS**:
```
Database Tables: ✓ (V4-V7 migrations)
├── lov_categories (Nationality, Occupation, Income, etc.)
├── lov_entries (Answer options with factors)
├── aml_occupations (Reference data)
├── aml_cities (Reference data)
└── aml_fatf_category (PEP list)
```

**What's MISSING**:
- ❌ **Admin REST API for CRUD** - Tenants cannot create/modify LOV sets via API
- ❌ **Bulk import from CSV/Excel** (FR-L.4)
- ❌ **LOV versioning for in-flight assessments** (FR-L.5)
- ❌ **Configurable category types** (Dominant vs Mutual Exclusive) UI (FR-L.6)
- ❌ **Admin portal interface** to manage LOV sets

**Implementation**: Only seed data loaded at startup; no runtime management.

**Gap**: Tenants cannot configure their own answer option libraries. This is a **CRITICAL blocker** for multi-tenant self-service.

---

#### 6. **Risk Parameter Management** (FR-P: 30% complete)
**Status**: PARTIALLY WORKING ⚠️

**What EXISTS**:
```
Domain Models: ✓
├── RiskParameter.java (domain/model/parameter/)
├── RiskType.java (CUSTOMER, BUSINESS, LOAN)
├── ParameterInputType.java (TEXT, BOOLEAN, LOV, LINKED_LOV)
└── ParameterFlagType.java (PEP, EDD, KYC)

Database Tables: ✓ (V4 migration)
├── risk_parameters (Questions)
├── parameter_lov_mappings (Link to LOV sets)
└── parameter_hierarchy (Parent-child questions)
```

**What's MISSING**:
- ❌ **Admin REST API for CRUD** (FR-P.1, FR-P.4) - No endpoints to create/modify parameters
- ❌ **Parameter versioning** (FR-P.5) - In-flight assessments not protected from config changes
- ❌ **Conditional child questions** (FR-P.3) - Parent-child relationships exist in schema but not enforced
- ❌ **Draft assessments** (FR-P.7) - Users cannot save partial assessments
- ❌ **Parameter activation/deactivation** (FR-P.8) - Parameters are immutable once created
- ❌ **Admin portal UI** to configure parameters

**Gap**: Tenants cannot configure their assessment questionnaires at runtime.

---

#### 7. **Assessment Session Lifecycle** (FR-S: 50% complete)
**Status**: PARTIALLY WORKING ⚠️

**What EXISTS**:
```
Domain Models: ✓
├── AssessmentSessionStatus.java (INITIATED, SUBMITTED, SCORED, etc.)
├── InternalCheckState.java (state machine)

Database Tables: ✓
├── risk_assessments (session root)
├── assessment_answers (captured KYC answers)
└── answer_history (versioned answers)
```

**What's IMPLEMENTED**:
- ✓ INITIATED → SUBMITTED → SCORED → COMPLETED flow
- ✓ Fraud check execution
- ✓ AML scoring
- ✓ Basic state transitions

**What's MISSING**:
- ❌ **DRAFT_SAVED state** (FR-P.7) - Partial answer saving not supported
- ❌ **Re-evaluation flow** (FR-S.7) - Answer modifications don't trigger full re-scoring
- ❌ **Answer modification tracking** - No delta capture on post-submission edits
- ❌ **In-flight assessment protection** (FR-L.5, FR-P.5) - Parameter changes break active sessions

**Gap**: Sessions cannot be saved mid-way; answer modifications don't propagate correctly.

---

### ❌ NOT IMPLEMENTED (4/11 Categories)

#### 8. **Tenant Management & Admin Portal** (FR-T: 0% complete)
**Status**: NOT IMPLEMENTED ❌

**CRITICAL MISSING**: The entire tenant onboarding workflow (Section 3.1 HLD).

**Required by HLD** (7-step process):
```
Step 1: Register Tenant              ❌ MISSING
Step 2: Configure Risk Types         ❌ MISSING
Step 3: Define LOV Master Sets       ❌ MISSING
Step 4: Create Risk Parameters       ❌ MISSING
Step 5: Set Scoring Thresholds       ❌ MISSING
Step 6: Test & Validate              ❌ MISSING
Step 7: Go Live                       ❌ MISSING
```

**What's MISSING**:
- ❌ **Tenant admin REST endpoints** (FR-T.1 through FR-T.5)
- ❌ **Tenant activation/deactivation** without impacting active assessments
- ❌ **Configuration audit trail** - Regulatory requirement for SAMA/CFTR
- ❌ **Self-service admin portal UI** - Tenants must manually configure via DB
- ❌ **Tenant isolation enforcement** - Some query-level filtering missing

**Impact**: **BLOCKING ISSUE** - Platform is not currently multi-tenant self-service capable.

---

#### 9. **Answer History & Audit Trail** (FR-A: 0% complete)
**Status**: NOT IMPLEMENTED ❌

**Required by HLD** (Section 6.5 - Regulatory requirement).

**What EXISTS**:
```
Database Table: ✓ (answer_history created in V1)
├── id, assessment_id, parameter_id, answer_value
└── (Only stores current answer, no versioning)
```

**What's MISSING**:
- ❌ **Version management** (FR-A.2) - Previous answer versions not captured
- ❌ **Change reason tracking** (FR-A.3) - Why was an answer modified?
- ❌ **Delta capture** (FR-A.3) - Old value, new value, change reason
- ❌ **Side-by-side diff view** (FR-A.4) - Compliance officer cannot compare versions
- ❌ **7-year data retention** (FR-A.5) - No archival/retention policy
- ❌ **Immutable audit log** (FR-A.6) - Current logs are mutable
- ❌ **Detailed audit format** (FR-A.7) - Missing JSON, UTC timestamp, IP, actor details

**Compliance Gap**: SAMA/CRFR require full audit trail. **This is a regulatory violation.**

**Required Fields** (per FR-A.1):
```
For each answer submission:
- Version number
- Assessment session ID
- Parameter ID
- Submitting user ID
- Answer value
- Answer type
- Language
- Weight contribution
- Risk score at submission time
- Risk level at submission time
```

---

#### 10. **Score Explainability & Breakdown** (FR-S.8: 0% complete)
**Status**: NOT IMPLEMENTED ❌

**HLD Requirement** (FR-S.8): "Engine shall produce a score breakdown showing each parameter's individual contribution to the total score."

**What's MISSING**:
- ❌ **Score breakdown response** - No endpoint returning parameter-level contributions
- ❌ **Explainability data** - Feature importance not computed
- ❌ **Weight attribution** - How much did each parameter contribute?
- ❌ **Factor analysis** - Which factors triggered high scores?

**API Response Should Include** (per HLD 4.2):
```json
{
  "totalScore": 45,
  "breakdown": [
    {
      "parameter": "Nationality",
      "categoryWeight": "16%",
      "factorWeight": 100,
      "contribution": 16,
      "riskLevel": "LOW"
    },
    {
      "parameter": "Occupation",
      "categoryWeight": "20%",
      "factorWeight": 75,
      "contribution": 15,
      "riskLevel": "MEDIUM"
    }
  ]
}
```

**Impact**: Compliance officers cannot explain why a customer was declined/flagged.

---

#### 11. **Third-Party AML & Sanctions Audit Logging** (FR-TP.7: 0% complete)
**Status**: PARTIALLY WORKING (calls made, audit missing) ⚠️

**What EXISTS**:
```
✓ AML middleware integration
✓ Sanctions/blocklist checks
✓ Results stored in screening_results table
```

**What's MISSING** (FR-TP.7 - Regulatory requirement):
- ❌ **Detailed audit logging** of third-party calls
- ❌ **Raw response capture** - Original AML response stored in audit
- ❌ **Timestamp logging** - When was each check performed?
- ❌ **Entity reference** - Who initiated? Which entity was checked?
- ❌ **Check type tracking** - AML screening vs sanctions vs blocklist vs local test
- ❌ **Result logging** - Match/no-match, confidence score, outcome

**Required by FR-TP.7**:
```
Each third-party check call must log:
✗ Timestamp
✗ Check type (AML / Sanctions / Blocklist / Local Test)
✗ Entity ID & Reference
✗ Input data submitted
✗ Raw response received
✗ Outcome (match / no-match / error)
✗ Confidence/match score
```

**Compliance Gap**: SAMA requires full traceability of external checks.

---

## SUMMARY BY FUNCTIONAL REQUIREMENT CATEGORY

| FR Code | Category | Required | Implemented | Status | Priority |
|---------|----------|----------|-------------|--------|----------|
| **FR-T** | Tenant Management | 5 reqs | 0 | ❌ NOT DONE | **CRITICAL** |
| **FR-L** | LOV Management | 6 reqs | 2 | ⚠️ 33% | **HIGH** |
| **FR-P** | Risk Parameter Mgmt | 8 reqs | 2 | ⚠️ 25% | **HIGH** |
| **FR-S** | Scoring Engine | 8 reqs | 5 | ⚠️ 62% | **HIGH** |
| **FR-A** | Answer History & Audit | 7 reqs | 0 | ❌ NOT DONE | **HIGH** |
| **FR-TP** | Third-Party Integration | 7 reqs | 4 | ⚠️ 57% | **MEDIUM** |
| **FR-C** | Compliance Checks | (Internal) | 9 | ✅ 100% | DONE |
| **FR-CS** | Credit Scoring | 3 reqs | 3 | ✅ 100% | DONE |

---

## CRITICAL BLOCKERS FOR PRODUCTION

### 🚨 BLOCKER #1: No Admin Portal for Tenant Configuration
**Impact**: Multi-tenancy is non-functional
- Tenants cannot self-configure risk types, LOV sets, or parameters
- All configuration must be done by platform admins via direct DB manipulation
- **Solution**: Build FR-T admin REST API + React admin portal

### 🚨 BLOCKER #2: No Answer History / Audit Trail
**Impact**: SAMA/CRFR compliance violation
- No version control on answers
- No change reason tracking
- No 7-year retention policy
- **Solution**: Implement FR-A.1 through FR-A.7

### 🚨 BLOCKER #3: No Score Breakdown Explainability
**Impact**: Cannot explain declined/flagged decisions
- Compliance officers have no visibility into why scores were assigned
- **Solution**: Implement FR-S.8 score breakdown computation

### 🚨 BLOCKER #4: No Third-Party Call Audit Logging
**Impact**: SAMA audit trail requirement not met
- AML/sanctions checks not logged for regulatory review
- **Solution**: Implement FR-TP.7 detailed audit logging

---

## RECOMMENDATIONS

### Phase 1: CRITICAL (Required for Production)
```
Priority: IMMEDIATE
├── Implement FR-T (Tenant Admin API)
├── Implement FR-A (Answer History & Audit)
├── Implement FR-S.8 (Score Breakdown)
└── Implement FR-TP.7 (Audit Logging)

Effort: 8-10 weeks
```

### Phase 2: HIGH (Required for Multi-Tenant Self-Service)
```
Priority: BEFORE PUBLIC LAUNCH
├── Complete FR-L (LOV Admin API + CSV Import)
├── Complete FR-P (Parameter Admin API + Versioning)
├── Build Admin Portal UI
└── Add Parameter Versioning for in-flight assessments

Effort: 6-8 weeks
```

### Phase 3: MEDIUM (Enhancement)
```
Priority: POST-MVP
├── Draft assessment saving (FR-P.7)
├── Answer modification re-evaluation (FR-S.7)
├── Multi-language support enhancements
└── Performance optimization

Effort: 4-6 weeks
```

---

## FILES TO CREATE/MODIFY

### Phase 1 Implementation Checklist

**New REST Controllers**:
```
services/risk-service/src/main/java/com/ksa/financing/risk/adapter/rest/controller/
├── TenantConfigController.java                  ← NEW (FR-T)
├── AnswerHistoryController.java                 ← NEW (FR-A)
└── RiskParameterAdminController.java            ← NEW (FR-P enhanced)
```

**New Domain Services**:
```
services/risk-service/src/main/java/com/ksa/financing/risk/domain/service/
├── TenantConfigurationService.java              ← NEW (FR-T)
├── AnswerHistoryService.java                    ← NEW (FR-A)
├── ScoreBreakdownEngine.java                    ← NEW (FR-S.8)
└── AuditLoggingService.java                     ← NEW (FR-TP.7)
```

**New Database Migrations**:
```
services/risk-service/src/main/resources/db/migration/
├── V8__add_tenant_config_tables.sql             ← NEW (FR-T)
├── V9__enhance_answer_history_versioning.sql    ← NEW (FR-A)
├── V10__add_score_breakdown_tables.sql          ← NEW (FR-S.8)
└── V11__add_third_party_audit_logging.sql       ← NEW (FR-TP.7)
```

---

## CONCLUSION

The Risk Service implementation is **45% complete**. The core fraud detection and AML scoring engines are fully functional, but **critical multi-tenant and audit features are missing**.

**Before production launch**, all Phase 1 items (Tenant Admin, Audit Trail, Score Breakdown, AML Audit Logging) must be implemented to meet regulatory compliance and enable multi-tenant self-service.

---

**Document Status**: READY FOR REVIEW
**Next Step**: Approval to proceed with Phase 1 implementation

