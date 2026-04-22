# Postman Collection - Endpoints Summary

## 📌 All Endpoints in Fraud Detection Testing Collection

### Core Fraud Check Endpoint

#### 1. **Run Internal Checks** (Main Endpoint)
```
POST /api/v1/risk/internal-checks
```
- **Used in**: Tests 1, 2, 3, 4, 5, 6, 7, 8
- **Purpose**: Executes all 10 fraud checks sequentially
- **Headers Required**: 
  - `X-Tenant-Id` (required)
  - `X-Device-Id` (optional)
  - `X-Device-Fingerprint` (optional)
  - `X-Client-Ip` (optional)
  - `X-Session-Id` (optional)
- **Request Body**:
  ```json
  {
    "nationalId": "1234567890",      // Optional
    "mobileNumber": "+966500000000",  // Optional (at least one required)
    "countryCode": "SAU"              // Optional, defaults to SAU
  }
  ```
- **Response**: 
  ```json
  {
    "assessmentId": "uuid",
    "status": "COMPLETED|BLOCKED",
    "overallDecision": "PASS|FLAG_ENHANCED_MONITORING|FLAG_HIGH_RISK|HARD_BLOCK",
    "riskLevel": "LOW|MEDIUM|HIGH|CRITICAL",
    "totalScore": 0-100,
    "blockReason": "string or null",
    "flags": ["FLAG_1", "FLAG_2"],
    "stepResults": [...]
  }
  ```

---

### Blacklist Management Endpoints

#### 2. **Add NID to Blacklist**
```
POST /api/v1/risk/blacklist/nid
```
- **Used in**: Setup for Test Case 2
- **Purpose**: Blacklist a National ID
- **Headers**: `X-Tenant-Id`
- **Request Body**:
  ```json
  {
    "nationalId": "1111111111",
    "reason": "FRAUD_CONFIRMED|WATCHLIST_PEP|...",
    "status": "ACTIVE"
  }
  ```

#### 3. **Add Mobile to Blacklist**
```
POST /api/v1/risk/blacklist/mobile
```
- **Used in**: Setup for Test Case 3
- **Purpose**: Blacklist a mobile number
- **Headers**: `X-Tenant-Id`
- **Request Body**:
  ```json
  {
    "mobileNumber": "+966500002222",
    "reason": "FRAUD_CONFIRMED",
    "status": "ACTIVE"
  }
  ```

#### 4. **View NID Blacklist** (Admin)
```
GET /api/v1/risk/blacklist/nid
```
- **Used in**: Admin verification
- **Purpose**: List all blacklisted NIDs
- **Headers**: `X-Tenant-Id`
- **Response**: 
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

#### 5. **View Mobile Blacklist** (Admin)
```
GET /api/v1/risk/blacklist/mobile
```
- **Used in**: Admin verification
- **Purpose**: List all blacklisted mobiles
- **Headers**: `X-Tenant-Id`
- **Response**: 
  ```json
  [
    {
      "id": "uuid",
      "mobileNumber": "+966500002222",
      "reason": "FRAUD_CONFIRMED",
      "status": "ACTIVE",
      "createdAt": "2026-04-17T..."
    }
  ]
  ```

---

### Audit & Verification Endpoints

#### 6. **View Audit Trail** (Admin)
```
GET /api/v1/risk/audit-trail?limit=50&offset=0
```
- **Used in**: Admin verification
- **Purpose**: View all risk assessments performed
- **Headers**: `X-Tenant-Id`
- **Query Parameters**:
  - `limit`: Number of records (default: 50)
  - `offset`: Pagination offset (default: 0)
- **Response**: 
  ```json
  {
    "total": 150,
    "items": [
      {
        "assessmentId": "uuid",
        "nationalId": "1234567890",
        "mobileNumber": "+966500000000",
        "overallDecision": "PASS",
        "riskLevel": "LOW",
        "totalScore": 15,
        "flags": [],
        "createdAt": "2026-04-17T..."
      }
    ]
  }
  ```

---

## 📋 Test Cases & Their Endpoints

| # | Test Case | Endpoints Used |
|---|---|---|
| 1 | Invalid NID Format | POST /internal-checks |
| 2 | Blacklist Hit | POST /blacklist/nid (setup) + POST /internal-checks |
| 3 | Mobile Blacklist | POST /blacklist/mobile (setup) + POST /internal-checks |
| 4 | Duplicate Mobile | POST /internal-checks (2x) |
| 5 | Velocity Exceeded | POST /internal-checks (11x) |
| 6 | Device Multi-NID | POST /internal-checks (2x) |
| 7 | Watchlist Match | POST /blacklist/nid (setup) + POST /internal-checks |
| 8 | Clean Registration | POST /internal-checks |
| Admin | View Blacklists | GET /blacklist/nid + GET /blacklist/mobile |
| Admin | View Audit Trail | GET /audit-trail |

---

## 🎯 Total Endpoints in Collection

### By Type:
- **POST** (Checks & Setup): 4
  - POST /api/v1/risk/internal-checks
  - POST /api/v1/risk/blacklist/nid
  - POST /api/v1/risk/blacklist/mobile
  
- **GET** (Admin/Verification): 3
  - GET /api/v1/risk/blacklist/nid
  - GET /api/v1/risk/blacklist/mobile
  - GET /api/v1/risk/audit-trail

### By Purpose:
- **Fraud Detection**: 1 endpoint
- **Blacklist Management**: 4 endpoints
- **Admin/Audit**: 2 endpoints

---

## 🔐 Required Headers

All requests require:
```
Header: X-Tenant-Id
Value:  550e8400-e29b-41d4-a716-446655440000
```

Optional headers (for /internal-checks):
```
X-Device-Id           → Device identifier
X-Device-Fingerprint  → Device fingerprint hash
X-Client-Ip           → Client IP for velocity checks
X-Session-Id          → Session identifier
```

---

## 📝 Postman Collection Structure

```
Fraud Detection Testing (Collection)
├── Variables
│   ├── BASE_URL: http://localhost:8090
│   ├── TENANT_ID: 550e8400-e29b-41d4-a716-446655440000
│   ├── DEVICE_ID: device-test-001
│   ├── SESSION_ID: session-test-001
│   └── CLIENT_IP: 192.168.1.100
│
└── Requests (16 total)
    ├── 1. Test Invalid NID Format
    ├── 2. Add NID to Blacklist
    ├── 2. Test Blacklist Hit - Silent Block
    ├── 3. Add Mobile to Blacklist
    ├── 3. Test Mobile Blacklist Hit
    ├── 4. Test Duplicate Mobile - First Registration
    ├── 4. Test Duplicate Mobile - Second Registration
    ├── 5. Test Velocity Exceeded - Rapid Fire (1-10)
    ├── 5. Test Velocity Exceeded - Request 11 (Over Limit)
    ├── 6. Test Device Multi-NID - First NID
    ├── 6. Test Device Multi-NID - Second NID
    ├── 7. Add Watchlist Entry
    ├── 7. Test Watchlist Match (Not Blacklist)
    ├── 8. Test Existing Customer (Pass Case)
    ├── Admin: View Blacklist Entries
    └── Admin: View Audit Trail
```

---

## ✨ How to Use Collection

### Import to Postman:
1. File → Import
2. Select: `docs/postman/Fraud-Detection-Tests.postman_collection.json`
3. Click Import

### Run Tests:
1. Click on any request
2. Modify variables if needed (click {{}} icon)
3. Click **Send**
4. Check response

### View All Requests:
- Left sidebar shows all 16 requests
- Click any request to view details
- Pre-configured headers and body templates included

---

## 📊 Response Examples

### ✅ PASS (Clean Registration)
```json
{
  "status": "COMPLETED",
  "overallDecision": "PASS",
  "riskLevel": "LOW",
  "totalScore": 15
}
```

### ❌ HARD_BLOCK (Blacklist)
```json
{
  "status": "BLOCKED",
  "overallDecision": "HARD_BLOCK",
  "blockReason": "Unable to proceed with registration at this time.",
  "totalScore": 0
}
```

### ⚠️ FLAG (Watchlist)
```json
{
  "status": "COMPLETED",
  "overallDecision": "FLAG_ENHANCED_MONITORING",
  "riskLevel": "HIGH",
  "totalScore": 40,
  "flags": ["WATCHLIST_MATCH"]
}
```

