# Risk Service - Correct Endpoints Reference

## ⚠️ IMPORTANT: Authentication Required

**All endpoints (except /internal-checks) require JWT token:**

```bash
Authorization: Bearer {JWT_TOKEN}
X-Tenant-Id: {TENANT_UUID}
```

---

## 🚀 Main Fraud Detection Endpoint

### Run Internal Checks (NO JWT REQUIRED)
```
POST /api/v1/risk/internal-checks
```

**Headers:**
```
X-Tenant-Id: 550e8400-e29b-41d4-a716-446655440000    (required)
X-Device-Id: device-001                              (optional)
X-Device-Fingerprint: fingerprint-abc                (optional)
X-Client-Ip: 192.168.1.1                            (optional)
X-Session-Id: session-001                           (optional)
```

**Body:**
```json
{
  "nationalId": "1234567890",
  "mobileNumber": "+966500000000",
  "countryCode": "SAU"
}
```

**Response:**
```json
{
  "assessmentId": "uuid",
  "status": "COMPLETED|BLOCKED",
  "overallDecision": "PASS|FLAG_ENHANCED_MONITORING|FLAG_HIGH_RISK|HARD_BLOCK",
  "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
  "totalScore": 0-100,
  "blockReason": null,
  "flags": [],
  "stepResults": [...]
}
```

---

## 🛡️ Blacklist Management (JWT REQUIRED)

### Add NID to Blacklist
```
POST /api/v1/risk/blacklist/nid
```

**Body:**
```json
{
  "nationalId": "1111111111",
  "reason": "FRAUD_CONFIRMED|WATCHLIST_PEP|SANCTIONS_MATCH"
}
```

**Response:**
```json
{
  "id": "uuid",
  "nationalId": "1111111111",
  "reason": "FRAUD_CONFIRMED",
  "status": "ACTIVE",
  "createdAt": "2026-04-17T10:30:00Z"
}
```

---

### Add Mobile to Blacklist
```
POST /api/v1/risk/blacklist/mobile
```

**Body:**
```json
{
  "mobileNumber": "+966500002222",
  "reason": "FRAUD_CONFIRMED"
}
```

---

### List NID Blacklist
```
GET /api/v1/risk/blacklist/nid
```

**Response:**
```json
[
  {
    "id": "uuid",
    "nationalId": "1111111111",
    "reason": "FRAUD_CONFIRMED",
    "status": "ACTIVE",
    "createdAt": "2026-04-17T..."
  }
]
```

---

### List Mobile Blacklist
```
GET /api/v1/risk/blacklist/mobile
```

---

### Get NID Status
```
GET /api/v1/risk/blacklist/nid/{nationalId}/status
```

---

### Get Mobile Status
```
GET /api/v1/risk/blacklist/mobile/{mobileNumber}/status
```

---

### Remove NID from Blacklist
```
POST /api/v1/risk/blacklist/nid/{nationalId}/remove
```

---

### Remove Mobile from Blacklist
```
POST /api/v1/risk/blacklist/mobile/{mobileNumber}/remove
```

---

## 📋 Audit Trail (JWT REQUIRED)

### Get Audit Trail by Date Range
```
GET /api/v1/risk/audit/date-range?from=2026-04-01T00:00:00Z&to=2026-04-30T23:59:59Z
```

**Response:**
```json
[
  {
    "id": "uuid",
    "entityType": "ASSESSMENT",
    "entityId": "assessment-uuid",
    "action": "CREATED",
    "actor": "SYSTEM",
    "timestamp": "2026-04-17T10:30:00Z",
    "details": {...}
  }
]
```

---

### Get Audit Trail by Correlation ID
```
GET /api/v1/risk/audit/correlation/{correlationId}
```

---

### Get Audit Trail by Entity
```
GET /api/v1/risk/audit/entity/{entityType}/{entityId}
```

**Example:**
```
GET /api/v1/risk/audit/entity/CUSTOMER/customer-uuid-123
```

---

### Get Audit Trail by Actor
```
GET /api/v1/risk/audit/actor/{actorId}?from=2026-04-01T00:00:00Z&to=2026-04-30T23:59:59Z
```

---

## 📊 Endpoint Summary

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/v1/risk/internal-checks` | ❌ | Run fraud checks |
| POST | `/api/v1/risk/blacklist/nid` | ✅ | Add NID to blacklist |
| POST | `/api/v1/risk/blacklist/mobile` | ✅ | Add mobile to blacklist |
| GET | `/api/v1/risk/blacklist/nid` | ✅ | List NID blacklist |
| GET | `/api/v1/risk/blacklist/mobile` | ✅ | List mobile blacklist |
| GET | `/api/v1/risk/blacklist/nid/{id}/status` | ✅ | Check NID status |
| GET | `/api/v1/risk/blacklist/mobile/{id}/status` | ✅ | Check mobile status |
| POST | `/api/v1/risk/blacklist/nid/{id}/remove` | ✅ | Remove NID from blacklist |
| POST | `/api/v1/risk/blacklist/mobile/{id}/remove` | ✅ | Remove mobile from blacklist |
| GET | `/api/v1/risk/audit/date-range` | ✅ | Audit by date |
| GET | `/api/v1/risk/audit/entity/{type}/{id}` | ✅ | Audit by entity |
| GET | `/api/v1/risk/audit/actor/{id}` | ✅ | Audit by actor |
| GET | `/api/v1/risk/audit/correlation/{id}` | ✅ | Audit by correlation |

---

## 🔐 How to Get JWT Token

### Option 1: From Keycloak
```bash
curl -X POST http://localhost:8080/realms/CompanyRealm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=admin-dashboard" \
  -d "client_secret=your-client-secret"
```

**Response:**
```json
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

### Option 2: In Postman
1. Use "Pre-request Script" to fetch token automatically
2. Or paste token in `{{JWT_TOKEN}}` variable

---

## ❌ What NOT To Do

```bash
# ❌ WRONG - No JWT token (will get 401)
curl http://localhost:8090/api/v1/risk/blacklist/nid

# ❌ WRONG - Old endpoint (404 Not Found)
curl http://localhost:8090/api/v1/risk/audit-trail

# ✅ CORRECT - With JWT token
curl -H "Authorization: Bearer {TOKEN}" \
  http://localhost:8090/api/v1/risk/blacklist/nid
```

---

## 🎯 Testing Checklist

- [ ] Risk-service running on port 8090
- [ ] Keycloak running on port 8080
- [ ] Get JWT token from Keycloak
- [ ] Set `{{JWT_TOKEN}}` in Postman
- [ ] Set `{{TENANT_ID}}` in Postman
- [ ] Run Test 1 (no JWT needed) — should work
- [ ] Run Test 2 (add to blacklist) — needs JWT token
- [ ] Check response status codes:
  - 200 = OK (GET, POST)
  - 201 = Created (POST blacklist)
  - 401 = Missing/invalid JWT
  - 404 = Wrong endpoint
  - 422 = Business validation error

---

## 📝 Common Errors & Fixes

### 404 Not Found
```
Problem: Endpoint doesn't exist or path is wrong
Fix: Check path starts with /api/v1/risk/
  ✅ /api/v1/risk/audit/date-range
  ❌ /api/v1/risk/audit-trail (wrong!)
```

### 401 Unauthorized
```
Problem: Missing or invalid JWT token
Fix: 
  1. Get token from Keycloak
  2. Add Authorization header: Bearer {TOKEN}
  3. Token must have tenant_id claim
```

### 422 Unprocessable Entity
```
Problem: Invalid request body
Fix: Check:
  - NID format (10 digits, starts with 1 or 2)
  - Mobile format (international format)
  - Reason field is one of: FRAUD_CONFIRMED, WATCHLIST_PEP, etc
```

### 403 Forbidden
```
Problem: JWT token lacks required role/permission
Fix:
  1. Check token has correct roles
  2. Endpoint has @SecuredEndpoint(obj="risk.blacklist", act="create")
  3. Your role must have this permission
```

