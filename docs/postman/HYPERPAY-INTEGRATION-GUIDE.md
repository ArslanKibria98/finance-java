# HyperPay Payment Integration Guide

**Complete API Reference for Mobile HyperPay Payment Flow**

---

## 📚 Table of Contents

1. [Quick Start](#quick-start)
2. [API Endpoints](#api-endpoints)
3. [Request/Response Examples](#requestresponse-examples)
4. [Error Handling](#error-handling)
5. [Security](#security)
6. [Testing Guide](#testing-guide)
7. [Troubleshooting](#troubleshooting)

---

## 🚀 Quick Start

### Prerequisites

- **Postman Collection**: `HyperPay-Payment-Flow.postman_collection.json`
- **JWT Token**: Valid token with `tenant_id` claim
- **Base URL**: `https://api.kfs.com` (production) or `http://localhost:8087` (dev)
- **HyperPay Account**: Merchant profile ID and API key

### Setup Steps

1. **Import Collection** into Postman
2. **Set Environment Variables**:
   ```
   base_url = https://api.kfs.com
   jwt_token = your_jwt_token
   tenant_id = your_tenant_uuid
   customer_id = your_customer_uuid
   loan_id = your_loan_uuid
   mobile_number = 966501234567
   ```
3. **Run Step 1**: POST Initiate Payment
4. **Copy checkout_url** from response
5. **Run Step 2**: Navigate to HyperPay checkout
6. **Run Step 3**: Webhook callback (simulated)
7. **Run Step 4**: Poll status until COMPLETED

---

## 🔗 API Endpoints

### 1. POST /api/v1/payments - Initiate Payment

**Purpose**: Mobile app initiates HyperPay payment

**Method**: `POST`  
**URL**: `{{base_url}}/api/v1/payments`

#### Headers

```
Authorization: Bearer {{jwt_token}}
Content-Type: application/json
X-Idempotency-Key: {{idempotency_key}}     // Unique per payment attempt
Accept-Language: ar-SA                      // or en-US
```

#### Request Body

```json
{
  "loanId": "770e8400-e29b-41d4-a716-446655440000",
  "installmentId": "inst-2026-05-15-001",
  "invoiceId": "inv-2026-05-15-001",
  "amount": 5000.00,
  "paymentMethod": "HYPERPAY_MADA",
  "idempotencyKey": "mobile-payment-unique-key-12345",
  "mobileNumber": "966501234567",
  "returnUrl": "myapp://payment-callback"
}
```

**Field Descriptions**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `loanId` | UUID | ✅ | Loan ID to pay for |
| `installmentId` | UUID | ✅ | **Specific installment being paid** - identifies which installment this payment covers |
| `invoiceId` | String | ✅ | **Invoice ID/number** - unique invoice identifier (e.g., INV-2026-05-15-001) - used for tracking & duplicate prevention |
| `amount` | Decimal | ✅ | Payment amount in SAR (must match or be less than installment outstanding) |
| `paymentMethod` | Enum | ✅ | HYPERPAY_MADA, HYPERPAY_VISA, HYPERPAY_MASTERCARD, HYPERPAY_APPLE_PAY |
| `idempotencyKey` | String | ✅ | Unique key (prevents duplicate charges if request retried) |
| `mobileNumber` | String | ⚠️ | Customer mobile (recommended for OTP) |
| `returnUrl` | String | ⚠️ | Deep link for app callback |

### Why Invoice ID & Installment ID are CRITICAL

```
Scenario: Loan has 3 installments (all 5000 SAR each, due on May 15, June 15, July 15)

❌ WITHOUT installmentId:
POST /api/v1/payments {
  "loanId": "loan-123",
  "amount": 5000,
  "paymentMethod": "HYPERPAY_MADA"
}
→ System doesn't know which of the 3 installments to mark as PAID!
→ Could mark wrong installment
→ Corruption of repayment schedule

✅ WITH installmentId:
POST /api/v1/payments {
  "loanId": "loan-123",
  "installmentId": "inst-2026-05-15-001",  // ← Clarifies WHICH installment
  "invoiceId": "inv-2026-05-15-001",      // ← Tracks this payment uniquely
  "amount": 5000,
  "paymentMethod": "HYPERPAY_MADA"
}
→ System knows exactly which installment to pay
→ Correct allocation waterfall (fees → profit → principal)
→ Invoice trail for auditing
```

#### Response (202 Accepted)

```json
{
  "workflowId": "payment-5a9d8e1f-2b4c-4d8e-9f1a-2b3c4d5e6f7g",
  "paymentId": "880e8400-e29b-41d4-a716-446655440000",
  "checkoutUrl": "https://checkout.hyperpay.com/session/CPR-123456789",
  "status": "INITIATED",
  "amount": 5000.00,
  "loanId": "770e8400-e29b-41d4-a716-446655440000",
  "nextAction": "REDIRECT_TO_CHECKOUT"
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `workflowId` | String | Temporal workflow ID - **save this to track payment** |
| `paymentId` | String | Internal payment ID |
| `checkoutUrl` | String | **HyperPay checkout page URL** - redirect mobile to this |
| `status` | Enum | INITIATED, PROCESSING, COMPLETED, FAILED |
| `nextAction` | String | REDIRECT_TO_CHECKOUT (what mobile should do) |

#### Error Responses

**400 Bad Request** - Invalid input
```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "COMMON.VALIDATION.FAILED",
  "message": "Loan not found: invalid UUID format",
  "path": "/api/v1/payments",
  "traceId": "a1b2c3d4"
}
```

**401 Unauthorized** - Missing/invalid JWT
```json
{
  "status": 401,
  "error": "Unauthorized",
  "code": "COMMON.AUTH.INVALID_CREDENTIALS",
  "message": "JWT token expired",
  "path": "/api/v1/payments",
  "traceId": "a1b2c3d4"
}
```

**422 Unprocessable Entity** - Business rule violation
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "PAYMENT.AMOUNT.EXCEEDS_OUTSTANDING",
  "message": "Payment amount 10000 exceeds outstanding balance 5000",
  "path": "/api/v1/payments",
  "traceId": "a1b2c3d4"
}
```

---

### 2. [Client-Side] HyperPay Checkout Page

**Purpose**: Customer enters card details and pays

**URL**: `{{checkoutUrl}}` from Step 1 response

**Process**:
1. Mobile opens URL in WebView or browser
2. HyperPay checkout page displays
3. Customer enters:
   - Card number
   - Expiry date (MM/YY)
   - CVV
   - Cardholder name
4. Click "Pay Now"
5. 3D Secure verification (OTP)
6. Customer enters OTP
7. HyperPay redirects to returnUrl with result

**Deep Link Results**:

**Success**:
```
myapp://payment-callback?checkoutId=CPR-123456789&result=APPROVE
```

**Declined**:
```
myapp://payment-callback?checkoutId=CPR-123456789&result=DECLINED&reason=insufficient_funds
```

**Cancelled**:
```
myapp://payment-callback?checkoutId=CPR-123456789&result=CANCELLED
```

---

### 3. POST /webhooks/hyperpay-callback - Webhook Callback

**Purpose**: HyperPay notifies Collections-Service of payment result

**Method**: `POST`  
**URL**: `{{base_url}}/webhooks/hyperpay-callback`  
**Triggered By**: HyperPay servers (automatic, not called by client)

#### Headers (Set by HyperPay)

```
Content-Type: application/json
X-Signature: sha256=HMAC-SHA256(payload, api_secret)     // Required for security
X-Webhook-Event-Id: EV-999
```

#### Request Body

```json
{
  "id": "EV-999",
  "resourceId": "CPR-123456789",
  "timestamp": "2026-05-15T12:05:30Z",
  "eventType": "CHECKOUT.COMPLETED",
  "data": {
    "checkoutId": "CPR-123456789",
    "status": "APPROVED",
    "transactionId": "TXN-999",
    "amount": 500000,
    "currency": "SAR",
    "paymentMethod": "MADA",
    "cardToken": "nBt...",
    "resultCode": "000",
    "resultDescription": "Transaction approved",
    "merchantReferenceId": "880e8400-e29b-41d4-a716-446655440000",
    "eci": "05",
    "timestamp": "2026-05-15T12:05:25Z"
  }
}
```

**Webhook Processing**:
1. Collections-Service **verifies signature** (MANDATORY)
2. Checks for **duplicate event ID** (idempotency)
3. Finds associated **payment & workflow**
4. **Sends signal** to Temporal workflow
5. Workflow **continues** with allocation/ledger/notification
6. Returns **200 OK**

#### Response (200 OK)

```json
{
  "acknowledged": true,
  "eventId": "EV-999"
}
```

---

### 4. GET /api/v1/payments/{workflowId}/status - Poll Payment Status

**Purpose**: Mobile polls to check payment processing status

**Method**: `GET`  
**URL**: `{{base_url}}/api/v1/payments/{{workflow_id}}/status`

#### Headers

```
Authorization: Bearer {{jwt_token}}
Accept: application/json
```

#### Response States

**State 1: Still Awaiting Gateway**
```json
{
  "workflowId": "payment-5a9d8e1f-2b4c-4d8e-9f1a-2b3c4d5e6f7g",
  "status": "PROCESSING",
  "currentStep": "AWAITING_GATEWAY_CALLBACK",
  "amount": 5000.00,
  "failureReason": null
}
```

**State 2: Applying Allocation**
```json
{
  "workflowId": "payment-5a9d8e1f-2b4c-4d8e-9f1a-2b3c4d5e6f7g",
  "status": "PROCESSING",
  "currentStep": "APPLYING_ALLOCATION",
  "amount": 5000.00,
  "failureReason": null
}
```

**State 3: Posting to Ledger**
```json
{
  "workflowId": "payment-5a9d8e1f-2b4c-4d8e-9f1a-2b3c4d5e6f7g",
  "status": "PROCESSING",
  "currentStep": "POSTING_LEDGER",
  "amount": 5000.00,
  "failureReason": null
}
```

**State 4: Completed Successfully ✅**
```json
{
  "workflowId": "payment-5a9d8e1f-2b4c-4d8e-9f1a-2b3c4d5e6f7g",
  "status": "COMPLETED",
  "currentStep": "DONE",
  "amount": 5000.00,
  "appliedAllocations": {
    "fees": 100.00,
    "profit": 1500.00,
    "principal": 3400.00
  },
  "ledgerPosted": true,
  "notificationSent": true,
  "failureReason": null
}
```

**State 5: Payment Failed ❌**
```json
{
  "workflowId": "payment-5a9d8e1f-2b4c-4d8e-9f1a-2b3c4d5e6f7g",
  "status": "FAILED",
  "failureReason": "Bank declined payment: Insufficient funds",
  "appliedAllocations": null
}
```

#### Polling Strategy (Mobile)

```typescript
// Poll every 2 seconds
const maxAttempts = 30;  // Total 60 seconds
let attempts = 0;

const checkStatus = setInterval(async () => {
  attempts++;
  
  const response = await fetch(
    `{{base_url}}/api/v1/payments/{{workflow_id}}/status`,
    { headers: { 'Authorization': `Bearer {{jwt_token}}` } }
  );
  
  const status = await response.json();
  
  if (status.status === 'COMPLETED') {
    clearInterval(checkStatus);
    showSuccessScreen(status);
  } else if (status.status === 'FAILED') {
    clearInterval(checkStatus);
    showErrorScreen(status.failureReason);
  } else if (attempts >= maxAttempts) {
    clearInterval(checkStatus);
    showTimeoutScreen();
  }
}, 2000);  // Poll every 2 seconds
```

---

## 📋 Request/Response Examples

### Example 1: Happy Path (Payment Success)

#### Step 1: Initiate Payment
```bash
curl -X POST https://api.kfs.com/api/v1/payments \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-123" \
  -d '{
    "loanId": "770e8400-e29b-41d4-a716-446655440000",
    "amount": 5000.00,
    "paymentMethod": "HYPERPAY_MADA",
    "idempotencyKey": "unique-123",
    "mobileNumber": "966501234567"
  }'
```

**Response**:
```json
{
  "workflowId": "payment-abc123",
  "checkoutUrl": "https://checkout.hyperpay.com/session/CPR-789",
  "status": "INITIATED"
}
```

#### Step 2-3: Customer pays on HyperPay checkout
- Enters card details
- Completes 3D Secure
- HyperPay redirects to app

#### Step 4: Poll Status
```bash
curl -X GET https://api.kfs.com/api/v1/payments/payment-abc123/status \
  -H "Authorization: Bearer eyJhbGc..."
```

**Response after 5-10 seconds**:
```json
{
  "status": "COMPLETED",
  "appliedAllocations": {
    "fees": 100.00,
    "profit": 1500.00,
    "principal": 3400.00
  }
}
```

### Example 2: Duplicate Payment (Idempotency)

#### First Request
```bash
curl -X POST https://api.kfs.com/api/v1/payments \
  -H "X-Idempotency-Key: unique-123" \
  -d '{ "loanId": "...", "amount": 5000 }'
```

**Response**: HTTP 202, creates payment

#### Second Request (Same Idempotency Key)
```bash
curl -X POST https://api.kfs.com/api/v1/payments \
  -H "X-Idempotency-Key: unique-123" \
  -d '{ "loanId": "...", "amount": 5000 }'
```

**Response**: HTTP 200, returns existing payment
```json
{
  "workflowId": "payment-abc123",
  "isDuplicate": true,
  "message": "Payment already exists for this idempotency key"
}
```

### Example 3: Payment Failure

#### Poll Status (Payment Failed)
```bash
curl -X GET https://api.kfs.com/api/v1/payments/payment-abc123/status \
  -H "Authorization: Bearer eyJhbGc..."
```

**Response**:
```json
{
  "status": "FAILED",
  "failureReason": "Bank declined payment: Insufficient funds"
}
```

---

## ⚠️ Error Handling

### Common Error Codes

| Code | HTTP | Description | Action |
|------|------|-------------|--------|
| `COMMON.VALIDATION.FAILED` | 400 | Invalid input (amount, loanId, etc) | Validate input, retry |
| `COMMON.AUTH.INVALID_CREDENTIALS` | 401 | JWT expired or invalid | Re-login, get new JWT |
| `COMMON.AUTH.ACCESS_DENIED` | 403 | No permission for endpoint | Check user role |
| `PAYMENT.LOAN.NOT_FOUND` | 404 | Loan doesn't exist | Verify loan UUID |
| `PAYMENT.AMOUNT.EXCEEDS_OUTSTANDING` | 422 | Payment > outstanding balance | Reduce amount, show available |
| `PAYMENT.DUPLICATE_REQUEST` | 409 | Payment already in progress | Use same idempotencyKey to get existing |
| `PAYMENT.GATEWAY_ERROR` | 500 | HyperPay API error | Retry, contact support |
| `PAYMENT.TIMEOUT` | 504 | Payment processing took too long | Retry different method |

### Error Response Format

```json
{
  "timestamp": "2026-05-15T12:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "PAYMENT.AMOUNT.EXCEEDS_OUTSTANDING",
  "message": "Payment amount 10000 SAR exceeds outstanding balance 5000 SAR",
  "path": "/api/v1/payments",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Mobile Error Handling

```typescript
async function handlePaymentError(error) {
  if (error.response?.status === 401) {
    // Token expired
    await refreshToken();
    return retryPayment();
  } else if (error.response?.status === 422) {
    // Business rule violation
    const code = error.response.data.code;
    if (code === 'PAYMENT.AMOUNT.EXCEEDS_OUTSTANDING') {
      showAlert('Amount too high', 'Reduce payment amount');
    }
    return false;
  } else if (error.response?.status === 404) {
    // Resource not found
    showAlert('Loan not found', 'Please select a different loan');
    return false;
  } else if (error.response?.status >= 500) {
    // Server error
    showAlert('Service temporarily unavailable', 'Try again in a moment');
    return retryPayment();
  }
}
```

---

## 🔐 Security

### 1. JWT Token Validation

**All endpoints require valid JWT token** in Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**JWT Claims Required**:
- `sub`: Customer ID (UUID)
- `tenant_id`: Tenant ID (UUID)
- `exp`: Expiration timestamp
- `iat`: Issued at timestamp

### 2. Webhook Signature Verification

**HyperPay signs every webhook** with HMAC-SHA256:

```
X-Signature: sha256=abcd1234efgh5678ijkl9012mnop3456...
```

**Verification Process**:
```java
// Server-side verification
String payload = objectMapper.writeValueAsString(webhookBody);
String expectedSignature = HmacSHA256.sign(payload, HYPERPAY_API_SECRET);

if (!expectedSignature.equals(receivedSignature)) {
  // REJECT - spoofed webhook
  return ResponseEntity.status(401).build();
}
```

### 3. Idempotency Protection

**Use unique idempotencyKey per payment attempt**:

```
X-Idempotency-Key: mobile-payment-{timestamp}-{uuid}
```

**Prevents**:
- Double charging if network retries
- Duplicate wallet debits
- Duplicate ledger entries

### 4. PCI Compliance

**Never store card data**:
- HyperPay handles card details (PCI compliant)
- Collections-Service only stores HyperPay's token
- Never log card numbers in any form

### 5. HTTPS Required

**All endpoints use HTTPS**:
- API calls over HTTPS only
- WebView handles certificate validation
- No cleartext transmission

---

## 🧪 Testing Guide

### Development Environment Setup

```bash
# 1. Import Postman collection
# HyperPay-Payment-Flow.postman_collection.json

# 2. Create Postman Environment
{
  "base_url": "http://localhost:8087",  // Dev Collections-Service
  "jwt_token": "your_dev_jwt_token",
  "tenant_id": "test-tenant-uuid",
  "loan_id": "test-loan-uuid",
  "mobile_number": "966501234567"
}

# 3. Start Collections-Service locally
cd services/collections-service
mvn spring-boot:run

# 4. Verify Temporal is running
docker compose ps | grep temporal

# 5. Verify Postgres has collections schema
psql -h localhost -U postgres -c "\\dt public.*"
```

### Test Scenarios

#### Test 1: Success Path
```
✅ POST /api/v1/payments (HYPERPAY_MADA)
✅ Receive checkoutUrl
✅ Simulate HyperPay webhook (success)
✅ Poll status → COMPLETED
✅ Verify allocation applied
```

#### Test 2: Idempotency
```
✅ POST /api/v1/payments with key "test-123"
✅ POST /api/v1/payments with same key "test-123"
✅ Both return same paymentId
✅ No duplicate charge
```

#### Test 3: Timeout
```
✅ POST /api/v1/payments
✅ Poll status for > 30 minutes
✅ Workflow auto-fails
✅ Error response returned
```

#### Test 4: Webhook Replay
```
✅ Send webhook with duplicate event ID
✅ Server rejects (already processed)
✅ Returns 200 (idempotent)
```

#### Test 5: Bad JWT
```
✅ Call API without Authorization header
✅ Returns 401 Unauthorized
✅ Error code: COMMON.AUTH.INVALID_CREDENTIALS
```

---

## 🐛 Troubleshooting

### Issue 1: "401 Unauthorized - JWT token expired"

**Symptoms**: Every API call returns 401

**Solution**:
1. Get new JWT token from identity-service
2. Update `jwt_token` variable in Postman
3. Retry the request

### Issue 2: "422 Unprocessable Entity - Loan not found"

**Symptoms**: Payment initiation fails with loan not found

**Solution**:
1. Verify loan UUID is correct
2. Verify loan belongs to customer (same tenant_id)
3. Check loan status is not CLOSED/CANCELLED
4. Confirm loan has active repayment schedule

### Issue 3: "Webhook never received / Workflow stuck in PROCESSING"

**Symptoms**: Poll status shows PROCESSING for > 5 minutes

**Solution**:
1. Check Temporal server is running: `docker compose ps`
2. Check Temporal logs: `docker compose logs temporal`
3. Check Collections-Service logs for activity errors
4. Verify webhook endpoint URL is correct
5. Check firewall allows incoming webhooks from HyperPay

### Issue 4: "Payment shows COMPLETED but installment still DUE"

**Symptoms**: Payment processed but repayment schedule not updated

**Solution**:
1. Check allocation records in DB: `SELECT * FROM payment_allocations WHERE paymentId = ?`
2. Verify installment was updated: `SELECT outstanding_principal FROM installments WHERE installmentId = ?`
3. Check ledger entries posted: `SELECT * FROM ledger_entries WHERE paymentId = ?`
4. Check for allocation activity errors in Temporal logs

### Issue 5: "Postman collection not working - variables not resolving"

**Symptoms**: Requests show `{{variable}}` instead of actual values

**Solution**:
1. Click "Environment" dropdown in Postman
2. Select or create environment with same variables
3. Verify all required variables are set:
   - `base_url`
   - `jwt_token`
   - `tenant_id`
   - `loan_id`
   - `mobile_number`
4. Click "Send" (Postman auto-resolves variables)

---

## 📞 Support

**For issues**:
1. Check logs: `docker compose logs collections-service`
2. Check Temporal UI: http://localhost:8233
3. Check database: `psql -h localhost -U postgres -d collections_db`
4. Contact: support@kfs.com with trace ID from error response

---

## 📊 Monitoring

### Key Metrics to Track

- Payment success rate: `payments.status = COMPLETED` / total
- Average time to completion: webhook received → status = COMPLETED
- Webhook failure rate: rejected signatures / total
- Timeout rate: status = FAILED (timeout) / total
- Retry rate: duplicate idempotencyKeys / total

### Sample Queries

```sql
-- Success rate
SELECT COUNT(*) FILTER (WHERE status = 'COMPLETED') * 100.0 / COUNT(*)
FROM payments
WHERE created_at > NOW() - INTERVAL '24 hours';

-- Average completion time
SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at))) as avg_seconds
FROM payments
WHERE status = 'COMPLETED'
AND created_at > NOW() - INTERVAL '24 hours';

-- Failed payments
SELECT payment_id, status, failure_reason, created_at
FROM payments
WHERE status = 'FAILED'
AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;
```

---

## 🎯 Summary

**HyperPay Payment Flow**:

1. **Mobile initiates** → POST /api/v1/payments (202 ACCEPTED)
2. **Receives checkout URL** → Saves workflowId
3. **Redirects to HyperPay** → Customer enters card details
4. **HyperPay processes** → 3D Secure OTP verification
5. **Sends webhook** → POST /webhooks/hyperpay-callback (server-side)
6. **Signal to Temporal** → Workflow continues
7. **Mobile polls status** → GET /api/v1/payments/{workflowId}/status
8. **Shows result** → Success screen with allocation details or error screen

**Key Points**:
- ✅ All endpoints require JWT
- ✅ Use idempotencyKey for replay safety
- ✅ Verify webhook signatures (security critical)
- ✅ Poll status every 2 seconds until COMPLETED/FAILED
- ✅ Handle timeout after 30 minutes
- ✅ Allocations applied in waterfall order (fees → profit → principal)

---

**Version**: 1.0.0  
**Last Updated**: 2026-05-15  
**Maintained By**: Collections Team
