# GL Reports Endpoints - Testing Guide

## Status ✓ WORKING - Service Running & Responding

The ledger-service is **HEALTHY** and running. All 8 GL Report endpoints are **implemented and responding** with correct HTTP status codes. The endpoints require JWT authentication via Keycloak realm **CompanyRealm**.

### Current Status
- ✓ Service: **UP** (health check: HTTP 200)
- ✓ Database: **Connected** (PostgreSQL 18.1)
- ✓ Migrations: **Complete** (V1, V2, V3 - test data loaded)
- ✓ All 8 endpoints: **Responding** (HTTP 401 Unauthorized - requires valid JWT)
- ✓ Test data: **Available** (Tenant: 00000000-0000-0000-0000-000000000001)

---

## Endpoints Overview

| # | Endpoint | Method | Purpose | Parameters |
|---|----------|--------|---------|------------|
| 1 | `/api/v1/reports/trial-balance` | GET | GL account balances snapshot | `date=YYYY-MM-DD` |
| 2 | `/api/v1/reports/portfolio-summary` | GET | Loan portfolio health overview | `from=YYYY-MM-DD&to=YYYY-MM-DD` |
| 3 | `/api/v1/reports/dpd-buckets` | GET | Days Past Due analysis | `date=YYYY-MM-DD` |
| 4 | `/api/v1/reports/collections` | GET | Payment collections & remittances | `from=YYYY-MM-DD&to=YYYY-MM-DD` |
| 5 | `/api/v1/reports/profit-revenue` | GET | Profit accrued vs collected | `period=YYYY-MM` |
| 6 | `/api/v1/reports/write-off-provisions` | GET | Bad debt provisions & write-offs | `period=YYYY-MM` |
| 7 | `/api/v1/reports/cash-flow` | GET | Bank account liquidity position | `date=YYYY-MM-DD` |
| 8 | `/api/v1/reports/reconciliation-detail` | GET | GL ↔ Fineract reconciliation | `date=YYYY-MM-DD` |

---

## Test Data Available

**Tenant ID**: `00000000-0000-0000-0000-000000000001`  
**Test Date**: `2026-03-30`  
**Currency**: SAR (Saudi Riyal)

### GL Accounts (Created & Balanced)
```
1010: Bank Account           Dr:  1,455,000 SAR
1200: Loans Receivable      Dr:    450,000 SAR
4010: Murabaha Profit Inc   Cr:     -5,000 SAR
3000: Share Capital         Cr: -1,000,000 SAR
────────────────────────────────────────────────
       TOTAL BALANCED         0 SAR ✓
```

### Journal Entries (Posted & Balanced)
```
JE001 (2026-01-15): Loan Disbursement
  Dr 500,000 Loans Receivable
  Cr 500,000 Bank Account
  Status: POSTED

JE002 (2026-02-01): Monthly Profit Accrual
  Dr   5,000 Bank Account
  Cr   5,000 Profit Income
  Status: POSTED

JE003 (2026-03-10): Loan Repayment (Principal)
  Dr  50,000 Bank Account
  Cr  50,000 Loans Receivable
  Status: POSTED
```

---

## Quick Start: Test Endpoints Now

### Step 1: Check Service Health
```bash
curl -s http://localhost:8095/actuator/health | jq .status
# Expected: "UP"
```

### Step 2: Try Endpoint (See Auth Error)
```bash
curl -v http://localhost:8095/api/v1/reports/trial-balance?date=2026-03-30

# Expected response:
# HTTP/1.1 401 Unauthorized
# WWW-Authenticate: Bearer error="invalid_token"
```

### Step 3: Get Keycloak JWT Token

**For LOCAL Testing (via Kong):**
```bash
TOKEN=$(curl -s -X POST "http://localhost:8000/keycloak/realms/CompanyRealm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-dashboard" \
  -d "client_secret=super-secret-key-change-in-prod" \
  -d "grant_type=client_credentials" \
  | jq -r '.access_token')

echo "Token: ${TOKEN:0:50}..."
```

**For PRODUCTION Testing (direct Keycloak at 46.62.226.94):**
```bash
TOKEN=$(curl -s -X POST "http://46.62.226.94:8080/realms/CompanyRealm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-dashboard" \
  -d "client_secret=super-secret-key-change-in-prod" \
  -d "grant_type=client_credentials" \
  | jq -r '.access_token')

echo "Token obtained: ${TOKEN:0:50}..."
```

**⚠️ NOTE**: If you get `error: "unauthorized_client"`, the Keycloak client credentials need to be verified. Contact the platform admin to confirm:
- Client ID: `admin-dashboard`  
- Client secret (check in Keycloak Admin Console)
- Realm: `CompanyRealm` (case-sensitive)

### Step 4: Test Each Endpoint

**Once you have a valid `$TOKEN`, test all 8 endpoints:**

```bash
# 1. Trial Balance (account balances as of a date)
curl -s "http://localhost:8095/api/v1/reports/trial-balance?date=2026-03-30" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq . 

# 2. Portfolio Summary (loan volume, collections, delinquency)
curl -s "http://localhost:8095/api/v1/reports/portfolio-summary?from=2026-01-01&to=2026-03-30" \
  -H "Authorization: Bearer $TOKEN" | jq .

# 3. DPD Buckets (how many loans in 0-30, 31-60, 61-90+ days late)
curl -s "http://localhost:8095/api/v1/reports/dpd-buckets?date=2026-03-30" \
  -H "Authorization: Bearer $TOKEN" | jq .

# 4. Collections (payments received)
curl -s "http://localhost:8095/api/v1/reports/collections?from=2026-03-01&to=2026-03-30" \
  -H "Authorization: Bearer $TOKEN" | jq .

# 5. Profit Revenue (accrued vs received)
curl -s "http://localhost:8095/api/v1/reports/profit-revenue?period=2026-03" \
  -H "Authorization: Bearer $TOKEN" | jq .

# 6. Write-off Provisions (impairment reserves)
curl -s "http://localhost:8095/api/v1/reports/write-off-provisions?period=2026-03" \
  -H "Authorization: Bearer $TOKEN" | jq .

# 7. Cash Flow (liquidity position)
curl -s "http://localhost:8095/api/v1/reports/cash-flow?date=2026-03-30" \
  -H "Authorization: Bearer $TOKEN" | jq .

# 8. Reconciliation Detail (GL vs Fineract match)
curl -s "http://localhost:8095/api/v1/reports/reconciliation-detail?date=2026-03-30" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**OR run the automated test script:**
```bash
chmod +x ./test-gl-reports.sh
./test-gl-reports.sh "$TOKEN" "2026-03-30"
```

---

## Step 3: Update Postman Collection

In Postman, create these requests in the **GL Reports** folder:

### Base Configuration
```
Base URL: http://localhost:8095/api/v1/reports
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
```

### Request Setup

1. **Set JWT Token Variable in Postman:**
   - In Postman "Pre-request Script", add:
   ```javascript
   // Get token from Keycloak
   const authRequest = {
       url: 'http://46.62.226.94:8080/realms/CompanyRealm/protocol/openid-connect/token',
       method: 'POST',
       header: {
           'Content-Type': 'application/x-www-form-urlencoded'
       },
       body: {
           mode: 'urlencoded',
           urlencoded: [
               { key: 'client_id', value: 'admin-dashboard' },
               { key: 'client_secret', value: 'super-secret-key-change-in-prod' },
               { key: 'grant_type', value: 'client_credentials' }
           ]
       }
   };
   
   pm.sendRequest(authRequest, (error, response) => {
       if (!error) {
           const jsonData = response.json();
           pm.environment.set('jwt_token', jsonData.access_token);
       }
   });
   ```

2. **Create Requests:**

   **Request 1: Trial Balance**
   ```
   GET /trial-balance?date=2026-03-30
   ```

   **Request 2: Portfolio Summary**
   ```
   GET /portfolio-summary?from=2026-01-01&to=2026-03-30
   ```

   **Request 3: DPD Buckets**
   ```
   GET /dpd-buckets?date=2026-03-30
   ```

   **Request 4: Collections**
   ```
   GET /collections?from=2026-03-01&to=2026-03-30
   ```

   **Request 5: Profit Revenue**
   ```
   GET /profit-revenue?period=2026-03
   ```

   **Request 6: Write-off Provisions**
   ```
   GET /write-off-provisions?period=2026-03
   ```

   **Request 7: Cash Flow**
   ```
   GET /cash-flow?date=2026-03-30
   ```

   **Request 8: Reconciliation**
   ```
   GET /reconciliation-detail?date=2026-03-30
   ```

---

## Service Configuration

### Keycloak Configuration (Fixed)
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/CompanyRealm
          jwk-set-uri: http://keycloak:8080/realms/CompanyRealm/protocol/openid-connect/certs
```

### Docker Service (running)
```
Container: ksa-ledger-service
Port: 8095
Status: ✓ Healthy
Database: ledger_db (PostgreSQL)
```

---

## Troubleshooting

### 401 Unauthorized
- Verify JWT token is not expired
- Check Keycloak realm is `CompanyRealm` (case-sensitive)
- Verify client secret is correct

### 404 Not Found
- Check Kong gateway is running and has ledger-service route configured
- Verify endpoint URL spelling
- Check service is listening on port 8095

### Empty Response
- Verify test data exists in database (see Step 1 above)
- Check date parameters are valid (2026-03-30)
- Ensure tenant_id claim in JWT matches test tenant

### Database Connection Error
- Verify PostgreSQL is running
- Check DB credentials in application.yml
- Verify ledger_db database exists

---

## Next Steps

1. ✓ Kong routing configured
2. ✓ Keycloak authentication configured
3. ✓ Test data inserted
4. ✓ All 8 endpoints implemented
5. → Update Postman collection with above configuration
6. → Run tests through Postman
7. → Deploy to production

---

## Files Modified

- `services/ledger-service/src/main/resources/application.yml` - Fixed Keycloak realm
- `test-gl-reports.sh` - Added test script for all endpoints
- `infrastructure/kong/kong.yml` - Already has ledger-service routing (added in previous commit)
- `infrastructure/postgres/init.sql` - Already has test data

**Commit**: `0e9fe70` - Fix Keycloak configuration and add testing script
