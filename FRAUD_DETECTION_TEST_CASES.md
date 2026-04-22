# Fraud Detection - Negative Test Cases

## Test Environment Setup

### Prerequisites
1. Risk-service running on `http://localhost:8090`
2. Valid tenant UUID (use: `550e8400-e29b-41d4-a716-446655440000`)
3. Postman or curl

### Test Tenant ID
```
X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000
```

---

## Test Case 1: Invalid NID Format

**Scenario**: Submit invalid NID that doesn't match SAU regex

### Test Data
- **NID**: `123456` (too short, must be 10 digits)
- **Mobile**: `+966500000000`
- **Country**: `SAU`

### Request
```bash
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "123456",
    "mobileNumber": "+966500000000",
    "countryCode": "SAU"
  }'
```

### Expected Response (FAIL - HARD_BLOCK)
```json
{
  "assessmentId": "uuid-xxx",
  "status": "BLOCKED",
  "overallDecision": "HARD_BLOCK",
  "riskLevel": null,
  "blockReason": "Saudi NID must be 10 digits starting with 1 (citizen) or 2 (resident). Got: 6 digits",
  "stepResults": [],
  "totalScore": 0,
  "flags": []
}
```

### Valid NID Patterns by Country
```
SAU (Saudi):  1xxxxxxxxx or 2xxxxxxxxx  (10 digits)
  Valid:   1234567890
  Invalid: 0234567890 (starts with 0)
  Invalid: 12345678 (too short)

PAK (Pakistan): xxxxxxxxxxxxxxxxxx (13 digits)

ARE (Emirates): 784xxxxxxxxxxx (15 digits, starts with 784)
```

---

## Test Case 2: NID on Blacklist (Silent Block)

**Scenario**: Valid NID format but NID is blacklisted

### Test Data
- **NID**: `1111111111` (assume this is on blacklist - add to blacklist first)
- **Mobile**: `+966500001111`

### Step 1: Add NID to Blacklist
```bash
curl -X POST http://localhost:8090/api/v1/risk/blacklist/nid \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "1111111111",
    "reason": "FRAUD_CONFIRMED",
    "status": "ACTIVE"
  }'
```

### Step 2: Run Internal Checks
```bash
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "1111111111",
    "mobileNumber": "+966500001111",
    "countryCode": "SAU"
  }'
```

### Expected Response (FAIL - SILENT BLOCK)
```json
{
  "assessmentId": "uuid-xxx",
  "status": "BLOCKED",
  "overallDecision": "HARD_BLOCK",
  "blockReason": "Unable to proceed with registration at this time.",
  "stepResults": [
    {"checkName": "NID_FORMAT_VALIDATION", "decision": "PASS"},
    {"checkName": "CIF_LOOKUP", "decision": "PASS"},
    {"checkName": "DUPLICATE_MOBILE", "decision": "PASS"},
    {"checkName": "BLACKLIST_WATCHLIST", "decision": "HARD_BLOCK"}
  ],
  "totalScore": 0,
  "flags": []
}
```

**Key Point**: Blacklist hits return generic "Unable to proceed..." message (not "Blacklisted") for security.

---

## Test Case 3: Mobile on Blacklist

**Scenario**: Valid NID but mobile is on blacklist

### Step 1: Add Mobile to Blacklist
```bash
curl -X POST http://localhost:8090/api/v1/risk/blacklist/mobile \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "mobileNumber": "+966500002222",
    "reason": "FRAUD_CONFIRMED",
    "status": "ACTIVE"
  }'
```

### Step 2: Run Internal Checks
```bash
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "1234567890",
    "mobileNumber": "+966500002222"
  }'
```

### Expected Response (FAIL)
```json
{
  "status": "BLOCKED",
  "overallDecision": "HARD_BLOCK",
  "blockReason": "Unable to proceed with registration at this time."
}
```

---

## Test Case 4: Duplicate Mobile

**Scenario**: Mobile used in another registration session

### Test Data
- **Mobile**: `+966500003333`
- **First attempt**: Session A, Device A, IP 192.168.1.1
- **Second attempt**: Session B, Device B, IP 192.168.1.2

### First Request
```bash
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Device-Id: device-001" \
  -H "X-Session-Id: session-001" \
  -d '{
    "mobileNumber": "+966500003333"
  }'
```

### Second Request (Same Mobile, Different Device/Session)
```bash
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Device-Id: device-002" \
  -H "X-Session-Id: session-002" \
  -d '{
    "mobileNumber": "+966500003333"
  }'
```

### Expected Response (SECOND REQUEST - FLAG)
```json
{
  "status": "COMPLETED",
  "overallDecision": "FLAG_ENHANCED_MONITORING",
  "riskLevel": "MEDIUM",
  "flags": ["DUPLICATE_MOBILE"],
  "stepResults": [
    {"checkName": "DUPLICATE_MOBILE", "decision": "FLAG_HIGH_RISK", "details": "Mobile used in another session"}
  ]
}
```

---

## Test Case 5: Velocity Exceeded

**Scenario**: Multiple attempts from same IP/device/NID within short time

### Rules
```
IP-based:       max 10 registrations per IP per hour
Device-based:   max 5 registrations per device per day
NID-based:      max 3 registrations per NID per day
Mobile-based:   max 3 registrations per mobile per day
```

### Rapid-Fire Test (Same IP)
```bash
# First 10 attempts succeed
for i in {1..10}; do
  curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
    -H "X-Client-Ip: 192.168.1.100" \
    -d "{
      \"nationalId\": \"123456789$i\",
      \"mobileNumber\": \"+96650000$((1000+i))\"
    }"
done

# Attempt 11 from same IP
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Client-Ip: 192.168.1.100" \
  -d '{
    "nationalId": "1234567890",
    "mobileNumber": "+966500005555"
  }'
```

### Expected Response (11th Request - FAIL)
```json
{
  "status": "COMPLETED",
  "overallDecision": "FLAG_HIGH_RISK",
  "riskLevel": "HIGH",
  "flags": ["VELOCITY_EXCEEDED_IP"],
  "stepResults": [
    {
      "checkName": "VELOCITY_CHECK",
      "decision": "FLAG_HIGH_RISK",
      "details": "IP 192.168.1.100 exceeded 10 requests/hour limit",
      "cooldownSeconds": 3600
    }
  ],
  "totalScore": 65
}
```

---

## Test Case 6: Device Multi-NID Association

**Scenario**: Same device used with different NIDs (suspicious behavior)

### Setup: Register 2 NIDs from Same Device
```bash
# Registration 1: NID A, Device X
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Device-Id: device-multiuse" \
  -H "X-Device-Fingerprint: fingerprint-abc123" \
  -d '{
    "nationalId": "1234567890",
    "mobileNumber": "+966500006666"
  }'

# Registration 2: NID B, Same Device X
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Device-Id: device-multiuse" \
  -H "X-Device-Fingerprint: fingerprint-abc123" \
  -d '{
    "nationalId": "2234567890",
    "mobileNumber": "+966500007777"
  }'
```

### Expected Response (2nd Registration - FLAGGED)
```json
{
  "status": "COMPLETED",
  "overallDecision": "FLAG_ENHANCED_MONITORING",
  "riskLevel": "MEDIUM",
  "totalScore": 30,
  "flags": ["MULTI_NID_DEVICE"],
  "stepResults": [
    {
      "checkName": "DEVICE_FINGERPRINT",
      "decision": "FLAG_HIGH_RISK",
      "details": "Device associated with 2 NIDs",
      "nidAssociationCount": 2
    }
  ]
}
```

---

## Test Case 7: Watchlist Match

**Scenario**: NID matches watchlist (requires manual review but allows continuation)

### Setup: Add to Watchlist
```bash
curl -X POST http://localhost:8090/api/v1/risk/blacklist/nid \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "1245678901",
    "reason": "WATCHLIST_PEP",
    "status": "ACTIVE"
  }'
```

### Run Check
```bash
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "1245678901",
    "mobileNumber": "+966500008888"
  }'
```

### Expected Response (FLAG - Not Hard Block)
```json
{
  "status": "COMPLETED",
  "overallDecision": "FLAG_ENHANCED_MONITORING",
  "riskLevel": "HIGH",
  "totalScore": 40,
  "flags": ["WATCHLIST_MATCH"],
  "stepResults": [
    {
      "checkName": "BLACKLIST_WATCHLIST",
      "decision": "FLAG_ENHANCED_MONITORING",
      "details": "NID matches watchlist"
    }
  ]
}
```

**Key Difference**: Watchlist = FLAG + continue. Blacklist = HARD_BLOCK + stop.

---

## Test Case 8: Existing Customer Routes to Login

**Scenario**: NID already exists in system

### Expected Response
```json
{
  "status": "COMPLETED",
  "overallDecision": "PASS",
  "riskLevel": "LOW",
  "routeTo": "LOGIN",
  "blockReason": null,
  "flags": []
}
```

---

## Test Case 9: Account Locked

**Scenario**: Customer account locked by admin

### Run Check
```bash
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "1256789012"
  }'
```

### Expected Response (HARD_BLOCK)
```json
{
  "status": "BLOCKED",
  "overallDecision": "HARD_BLOCK",
  "blockReason": "Account locked - Unable to proceed with registration",
  "stepResults": [
    {"checkName": "ACCOUNT_LOCK", "decision": "HARD_BLOCK"}
  ]
}
```

---

## Test Case 10: Combined Critical Risk

**Scenario**: Multiple risk factors trigger CRITICAL level

### Setup
```bash
# 1. Rapid registrations from same IP (accumulate velocity)
for i in {1..5}; do
  curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
    -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
    -H "X-Client-Ip: 192.168.1.150" \
    -d "{\"nationalId\": \"135791357$i\", \"mobileNumber\": \"+96650000999$i\"}"
done

# 2. Add watchlist hit
curl -X POST http://localhost:8090/api/v1/risk/blacklist/nid \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "nationalId": "1357913579",
    "reason": "WATCHLIST_HIGH_RISK",
    "status": "ACTIVE"
  }'

# 3. Final attempt
curl -X POST http://localhost:8090/api/v1/risk/internal-checks \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Client-Ip: 192.168.1.150" \
  -H "X-Device-Id: device-risky" \
  -d '{
    "nationalId": "1357913579",
    "mobileNumber": "+966500009999"
  }'
```

### Expected Response
```json
{
  "status": "BLOCKED",
  "overallDecision": "HARD_BLOCK",
  "riskLevel": "CRITICAL",
  "totalScore": 100,
  "flags": ["WATCHLIST_MATCH", "VELOCITY_EXCEEDED_IP", "MULTI_NID_DEVICE"]
}
```

---

## Scoring Model

| Factor | Points |
|--------|--------|
| New customer | +15 |
| Watchlist match | +40 |
| Velocity exceeded | +25 |
| Multi-NID device | +30 |
| Duplicate mobile | +15 |

### Risk Levels
```
0-30:    LOW    → PASS
31-60:   MEDIUM → FLAG_ENHANCED_MONITORING
61-80:   HIGH   → FLAG_HIGH_RISK
81-100:  CRITICAL → HARD_BLOCK
```

---

## Verification Commands

### Check Blacklist Entries
```bash
curl http://localhost:8090/api/v1/risk/blacklist/nid \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" | jq .
```

### View Audit Trail
```bash
curl http://localhost:8090/api/v1/risk/audit-trail \
  -H "X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000" | jq .
```

### Enable Debug Logs
```yaml
logging:
  level:
    com.ksa.financing.risk: DEBUG
```

