# HyperPay Payment - Quick Reference Cheat Sheet

## 🚀 5-Step Flow

```
┌─────────────────────────────────────┐
│ Step 1: Mobile POST /api/v1/payments│
│ → Get workflowId + checkoutUrl      │
└──────────────┬──────────────────────┘
               ⬇️
┌──────────────────────────────────┐
│ Step 2: Open checkoutUrl         │
│ Customer enters card & OTP       │
└──────────────┬───────────────────┘
               ⬇️
┌──────────────────────────────────┐
│ Step 3: HyperPay webhook         │
│ (Server → Server)                │
│ Signals Temporal workflow        │
└──────────────┬───────────────────┘
               ⬇️
┌──────────────────────────────────┐
│ Step 4: Mobile polls status      │
│ GET /api/v1/payments/{id}/status │
│ Every 2 seconds                  │
└──────────────┬───────────────────┘
               ⬇️
┌──────────────────────────────────┐
│ Step 5: Show result              │
│ COMPLETED ✅ or FAILED ❌        │
└──────────────────────────────────┘
```

---

## 📡 Endpoints Summary

| # | Method | Endpoint | Purpose | Response |
|---|--------|----------|---------|----------|
| 1️⃣ | POST | `/api/v1/payments` | Initiate payment | 202 + workflowId + checkoutUrl |
| 2️⃣ | GET | `checkoutUrl` | HyperPay checkout (browser) | Deep link redirect |
| 3️⃣ | POST | `/webhooks/hyperpay-callback` | HyperPay callback (auto) | 200 OK |
| 4️⃣ | GET | `/api/v1/payments/{id}/status` | Poll status | Current status |

---

## 🔄 Request/Response Quick Look

### 1️⃣ Initiate Payment

```bash
POST /api/v1/payments
Authorization: Bearer {{token}}
X-Idempotency-Key: {{unique_key}}

{
  "loanId": "uuid",
  "amount": 5000,
  "paymentMethod": "HYPERPAY_MADA",
  "idempotencyKey": "unique_key"
}
```

**Response** (202):
```json
{
  "workflowId": "payment-abc123",
  "checkoutUrl": "https://checkout.hyperpay.com/session/CPR-xyz",
  "status": "INITIATED"
}
```

### 4️⃣ Poll Status

```bash
GET /api/v1/payments/payment-abc123/status
Authorization: Bearer {{token}}
```

**Response** (COMPLETED):
```json
{
  "status": "COMPLETED",
  "appliedAllocations": {
    "fees": 100,
    "profit": 1500,
    "principal": 3400
  }
}
```

**Response** (FAILED):
```json
{
  "status": "FAILED",
  "failureReason": "Insufficient funds"
}
```

---

## ⚙️ Payment Methods

```
HYPERPAY_MADA         → Saudi Debit Card (MADA)
HYPERPAY_VISA         → International Visa
HYPERPAY_MASTERCARD   → International Mastercard
HYPERPAY_APPLE_PAY    → Apple Pay (tokenized)
```

---

## ⏱️ Mobile Implementation Checklist

- [ ] Set Authorization header with JWT token
- [ ] Generate unique idempotencyKey
- [ ] POST /api/v1/payments with payment details
- [ ] Save workflowId from response
- [ ] Extract checkoutUrl from response
- [ ] Open checkoutUrl in WebView or browser
- [ ] Listen for deep link callback (myapp://payment-callback)
- [ ] On callback, start polling GET /status
- [ ] Poll every 2 seconds for max 30 attempts (60 sec timeout)
- [ ] Stop polling when status = COMPLETED or FAILED
- [ ] Display success/error screen to customer

---

## 🔑 Headers Required

```
Authorization: Bearer {{jwt_token}}           ← Required on all API calls
Content-Type: application/json                ← Required on POST
X-Idempotency-Key: {{unique_key}}             ← Required on POST (prevents duplicates)
Accept-Language: ar-SA or en-US               ← Optional (for error messages)
X-Signature: sha256={{signature}}             ← Set by HyperPay on webhook
```

---

## 🧪 Test Requests (Copy-Paste)

### Initialize Payment
```json
{
  "loanId": "770e8400-e29b-41d4-a716-446655440000",
  "installmentId": "inst-2026-05-15-001",
  "invoiceId": "inv-2026-05-15-001",
  "amount": 5000,
  "paymentMethod": "HYPERPAY_MADA",
  "idempotencyKey": "test-key-{{$timestamp}}"
}
```

**Key Fields**:
- ✅ `installmentId` - WHICH installment to pay (critical!)
- ✅ `invoiceId` - Tracks this payment uniquely (for auditing)

### Check Status
```
GET /api/v1/payments/payment-abc123/status
```

---

## 🐛 Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| `401 Unauthorized` | JWT expired → Get new token |
| `404 Loan not found` | Wrong loanId → Verify UUID |
| `422 Amount exceeds` | Payment > outstanding → Reduce amount |
| `Webhook stuck` | Temporal down → Check `docker compose ps` |
| `Status still PROCESSING` | After 2 min | Check Temporal logs |
| Variables `{{var}}` not resolving | Import environment in Postman |

---

## 📊 Status Values

```
INITIATED       → Payment created, awaiting checkout
PROCESSING      → Waiting for customer payment or allocation
COMPLETED ✅    → Success! Allocation applied, ledger posted
FAILED ❌       → Bank declined, timeout, or error
```

---

## 🔐 Security Essentials

✅ **Always**:
- Use HTTPS only
- Verify JWT token before API calls
- Generate unique idempotencyKey per payment
- Never log card numbers
- Verify webhook signatures (server-side)

❌ **Never**:
- Hardcode JWT tokens
- Store card details
- Skip HTTPS
- Log sensitive data
- Trust unverified webhooks

---

## 💻 Mobile Code Example (React Native)

```typescript
// 1. Initiate Payment
const response = await fetch('https://api.kfs.com/api/v1/payments', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'X-Idempotency-Key': generateUUID(),
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    loanId: loanId,
    amount: 5000,
    paymentMethod: 'HYPERPAY_MADA',
    idempotencyKey: generateUUID()
  })
});

const data = await response.json();
const { workflowId, checkoutUrl } = data;

// 2. Open Checkout
WebView.open(checkoutUrl);

// 3. Handle Callback
Linking.addEventListener('url', (event) => {
  if (event.url.startsWith('myapp://payment-callback')) {
    startPolling(workflowId);
  }
});

// 4. Poll Status
const pollStatus = async (workflowId) => {
  for (let i = 0; i < 30; i++) {
    const response = await fetch(
      `https://api.kfs.com/api/v1/payments/${workflowId}/status`,
      { headers: { 'Authorization': `Bearer ${jwtToken}` } }
    );
    
    const status = await response.json();
    
    if (status.status === 'COMPLETED') {
      showSuccessScreen(status);
      break;
    } else if (status.status === 'FAILED') {
      showErrorScreen(status.failureReason);
      break;
    }
    
    await sleep(2000); // Wait 2 seconds
  }
};
```

---

## 📋 Environment Variables (Postman)

```
base_url           = https://api.kfs.com
jwt_token          = your_jwt_here
tenant_id          = xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
customer_id        = xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
loan_id            = xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
mobile_number      = 966501234567
idempotency_key    = {{$randomUUID}}
```

---

## 🔗 Important URLs

| Service | URL |
|---------|-----|
| Collections API | https://api.kfs.com |
| HyperPay Checkout | https://checkout.hyperpay.com |
| Temporal UI | http://localhost:8233 |
| Postman Collection | `docs/postman/HyperPay-Payment-Flow.postman_collection.json` |

---

## ✅ Success Criteria

- ✅ Payment initiated (202 response)
- ✅ Checkout URL generated
- ✅ Customer completes payment
- ✅ Webhook received (server-side)
- ✅ Status polling shows COMPLETED
- ✅ Allocations applied (fees → profit → principal)
- ✅ Ledger entries posted
- ✅ Customer receives SMS confirmation
- ✅ Installment marked as PAID

---

## 🎯 Remember

1. **Idempotency Key** = Prevents duplicate charges if request retried
2. **Workflow ID** = Use this to poll status (not payment ID)
3. **Poll Every 2 Seconds** = Don't spam too fast
4. **30 Second Timeout** = Don't poll forever
5. **Waterfall Allocation** = Fees → Profit → Principal (order matters!)

---

**Pro Tips**:
- 💡 Always set idempotencyKey client-side before hitting API
- 💡 Save workflowId immediately after Step 1
- 💡 Use environment variables in Postman (don't hardcode)
- 💡 Test webhook signature verification server-side first
- 💡 Log traceId from error responses for debugging

---

**Last Updated**: 2026-05-15  
**Status**: Production Ready ✅
