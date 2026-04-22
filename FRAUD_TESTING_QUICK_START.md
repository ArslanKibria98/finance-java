# Fraud Detection Testing - Quick Start Guide

## 📋 What's Been Implemented

Your risk-service has **10 sequential fraud checks**:

```
1. NID Format Validation     → HARD_BLOCK if invalid
2. CIF Lookup                → Routes to NEW/LOGIN
3. Duplicate Mobile Check    → Flags if reused
4. Blacklist/Watchlist       → HARD_BLOCK (blacklist) / FLAG (watchlist)
5. Fraud History Check       → HARD_BLOCK if confirmed fraud
6. Device Fingerprint        → FLAGS if device has 2+ NIDs
7. Velocity Check            → FLAGS if exceeds IP/device/NID/mobile limits
8. Account Lock Check        → HARD_BLOCK if locked
9. Internal Sanctions Check  → HARD_BLOCK if on SAMA list
10. Risk Score Calculation   → Assigns LOW/MEDIUM/HIGH/CRITICAL
```

---

## 🚀 Quick Testing Guide

### Step 1: Import Postman Collection

1. Open Postman
2. **File → Import**
3. Select: `/var/www/islamic-financing-platform/docs/postman/Fraud-Detection-Tests.postman_collection.json`
4. Click **Import**

### Step 2: Set Variables

In Postman, click **Environment** → **Manage Environments** and set:

```
BASE_URL:   http://localhost:8090
TENANT_ID:  550e8400-e29b-41d4-a716-446655440000
DEVICE_ID:  device-test-001
SESSION_ID: session-test-001
CLIENT_IP:  192.168.1.100
```

### Step 3: Run Tests in Order

Run these **in sequence**:

```
✅ 1. Test Invalid NID Format
  Expected: HARD_BLOCK with validation error

✅ 2. Add NID to Blacklist (Setup)
  Then: Test Blacklist Hit
  Expected: HARD_BLOCK with "Unable to proceed..." message

✅ 3. Add Mobile to Blacklist (Setup)
  Then: Test Mobile Blacklist Hit
  Expected: HARD_BLOCK

✅ 4. Test Duplicate Mobile
  First Registration:  should PASS
  Second Registration: should FLAG_ENHANCED_MONITORING

✅ 5. Test Velocity Exceeded
  Requests 1-10 from same IP: PASS
  Request 11 from same IP:    FLAG_HIGH_RISK

✅ 6. Test Device Multi-NID
  First NID:  should PASS
  Second NID: should FLAG_ENHANCED_MONITORING

✅ 7. Test Watchlist Match
  First: Add to watchlist
  Then: Test check - should FLAG (not HARD_BLOCK)

✅ 8. Test Clean Registration (Positive)
  Expected: PASS with LOW risk
```

---

## 📊 Risk Scoring Model

Each risk factor adds points:

| Factor | Points | Risk Level |
|--------|--------|-----------|
| New customer | +15 | LOW |
| Watchlist match | +40 | HIGH |
| Velocity exceeded | +25 | HIGH |
| Multi-NID device | +30 | MEDIUM |
| Duplicate mobile | +15 | MEDIUM |

**Total Score → Decision**:
- `0-30`:   PASS (allow)
- `31-60`:  FLAG_ENHANCED_MONITORING (allow + review)
- `61-80`:  FLAG_HIGH_RISK (allow + manual review)
- `81-100`: HARD_BLOCK (deny)

---

## 🎯 Test Checklist

### Test Case 1: Invalid NID Format ✓
```bash
# Invalid NID (too short)
POST /api/v1/risk/internal-checks
Body: {"nationalId": "123456"}
Expected: HARD_BLOCK
```

### Test Case 2: Blacklist ✓
```bash
# Add to blacklist
POST /api/v1/risk/blacklist/nid
Body: {"nationalId": "1111111111", "reason": "FRAUD_CONFIRMED"}

# Check
POST /api/v1/risk/internal-checks
Body: {"nationalId": "1111111111"}
Expected: HARD_BLOCK with generic message
```

### Test Case 3: Mobile Blacklist ✓
```bash
# Add to blacklist
POST /api/v1/risk/blacklist/mobile
Body: {"mobileNumber": "+966500002222"}

# Check
POST /api/v1/risk/internal-checks
Body: {"mobileNumber": "+966500002222"}
Expected: HARD_BLOCK
```

### Test Case 4: Duplicate Mobile ✓
```bash
# First registration - PASS
# Second registration with same mobile - FLAG_ENHANCED_MONITORING
Expected: flags=["DUPLICATE_MOBILE"], totalScore >= 15
```

### Test Case 5: Velocity Exceeded ✓
```bash
# Make 11 requests from same IP (max is 10/hour)
Expected: 11th request flags=["VELOCITY_EXCEEDED_IP"]
```

### Test Case 6: Device Multi-NID ✓
```bash
# Register 2 different NIDs from same device
Expected: 2nd registration flags=["MULTI_NID_DEVICE"], totalScore=30
```

### Test Case 7: Watchlist Match ✓
```bash
# Add to watchlist (not blacklist)
POST /api/v1/risk/blacklist/nid
Body: {"reason": "WATCHLIST_PEP"}

# Check
Expected: FLAG_ENHANCED_MONITORING (not HARD_BLOCK!)
Decision: allows continuation
Points: +40
```

### Test Case 8: Positive Case ✓
```bash
# Clean registration, no risk factors
Expected: PASS, LOW risk, no flags
```

---

## 🔍 How to Verify Rules Are Working

### 1. Enable Debug Logging
```yaml
# services/risk-service/src/main/resources/application.yml
logging:
  level:
    com.ksa.financing.risk: DEBUG
    com.ksa.financing.infra: DEBUG
```

### 2. Check Logs
```bash
# Watch risk-service logs
docker logs -f risk-service | grep -i "fraud\|velocity\|blacklist\|watchlist\|device"

# Example output:
# "Starting fraud history check via fraud-service"
# "Blacklist/Watchlist check HARD_BLOCK on NID"
# "IP 192.168.1.100 exceeded 10 requests/hour limit"
# "Device device-001 associated with 2 NIDs"
```

### 3. Check Database (if you have access)
```sql
-- NID Blacklist
SELECT * FROM nid_blacklist_entries 
WHERE tenant_id = '550e8400-e29b-41d4-a716-446655440000';

-- Mobile Blacklist
SELECT * FROM mobile_blacklist_entries 
WHERE tenant_id = '550e8400-e29b-41d4-a716-446655440000';

-- Velocity Logs
SELECT * FROM velocity_logs 
WHERE tenant_id = '550e8400-e29b-41d4-a716-446655440000' 
ORDER BY created_at DESC LIMIT 20;

-- Device Registry
SELECT * FROM device_registry 
WHERE tenant_id = '550e8400-e29b-41d4-a716-446655440000';
```

### 4. View Audit Trail via API
```bash
curl http://localhost:8090/api/v1/risk/audit-trail \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" | jq .
```

### 5. Check Blacklist Entries via API
```bash
# View all NID blacklist entries
curl http://localhost:8090/api/v1/risk/blacklist/nid \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" | jq .

# View all mobile blacklist entries
curl http://localhost:8090/api/v1/risk/blacklist/mobile \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" | jq .
```

---

## 🏗️ Architecture Overview

```
HTTP Request
    ↓
InternalChecksController
    ↓
RunInternalChecksUseCase
    ↓
[Sequential Checks via Temporal Workflow]
    ├→ NidFormatValidationActivity
    ├→ CifLookupActivity
    ├→ DuplicateMobileCheckActivity
    ├→ BlacklistWatchlistActivity
    ├→ FraudHistoryActivity
    ├→ DeviceFingerprintActivity
    ├→ VelocityCheckActivity
    ├→ AccountLockActivity
    ├→ InternalSanctionsActivity
    └→ RiskScoreActivity
    ↓
InternalCheckResult (assessment_id, decision, flags, totalScore)
    ↓
Response: 200 OK with detailed breakdown
```

---

## 📝 Response Structure

### Successful Registration (PASS)
```json
{
  "assessmentId": "uuid-xxx",
  "status": "COMPLETED",
  "overallDecision": "PASS",
  "riskLevel": "LOW",
  "totalScore": 15,
  "routeTo": "ONBOARDING",
  "blockReason": null,
  "flags": [],
  "stepResults": [
    {
      "checkName": "NID_FORMAT_VALIDATION",
      "decision": "PASS",
      "checkDetails": "Valid Saudi NID format"
    },
    ...
  ]
}
```

### Blocked (HARD_BLOCK)
```json
{
  "status": "BLOCKED",
  "overallDecision": "HARD_BLOCK",
  "blockReason": "Unable to proceed with registration at this time.",
  "riskLevel": null,
  "totalScore": 0,
  "stepResults": [...],
  "flags": ["BLACKLIST_MATCH"]
}
```

### Flagged (FLAG_ENHANCED_MONITORING)
```json
{
  "status": "COMPLETED",
  "overallDecision": "FLAG_ENHANCED_MONITORING",
  "riskLevel": "HIGH",
  "totalScore": 40,
  "routeTo": "MANUAL_REVIEW",
  "flags": ["WATCHLIST_MATCH"],
  "stepResults": [...]
}
```

---

## ⚠️ Important Notes

### Silent Blocking
- **Blacklist** = returns generic "Unable to proceed..." message (don't reveal reason)
- **Watchlist** = returns FLAG (not HARD_BLOCK) and allows continuation

### Velocity Rules
- **IP**: max 10 registrations per hour
- **Device**: max 5 registrations per day
- **NID**: max 3 registrations per day
- **Mobile**: max 3 registrations per day

### Country-Specific NID Validation
```
SAU (Saudi):  1xxxxxxxxx or 2xxxxxxxxx  (10 digits)
PAK:          xxxxxxxxxxxxxxxxxx        (13 digits)
ARE (Emirates): 784xxxxxxxxxxx          (15 digits)
```

### Risk Score Capping
- Maximum score is 100 (capped)
- Score 81-100 = CRITICAL = HARD_BLOCK
- Multiple factors accumulate

---

## 📚 Reference Files

1. **Test Cases**: `FRAUD_DETECTION_TEST_CASES.md`
2. **Postman Collection**: `docs/postman/Fraud-Detection-Tests.postman_collection.json`
3. **Source Code**:
   - Controllers: `services/risk-service/src/main/java/com/ksa/financing/risk/adapter/rest/controller/`
   - Domain: `services/risk-service/src/main/java/com/ksa/financing/risk/domain/`
   - Tests: `services/risk-service/src/test/java/com/ksa/financing/risk/`

---

## 🎓 Next Steps

1. **Import Postman collection**
2. **Run tests in order** (Test Case 1-8)
3. **Monitor logs** to see fraud checks in action
4. **Verify audit trail** to see all assessments
5. **Add custom test cases** as needed

Good luck with your fraud detection testing! 🚀

