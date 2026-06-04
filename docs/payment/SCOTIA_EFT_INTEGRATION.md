# Scotiabank EFT Integration — Local Canada Fund Transfer (FT)

> Status: **Implemented** in `middleware-third-party` (DEV). Rail = **EFT** (account-number based
> Canada fund transfer). Routed through the existing third-party gateway — no caller holds Scotia
> credentials. Migration `V18__add_scotiabank_eft_apis.sql`.

---

## 1. Why EFT (not RTP / Wire)

Account-number transfer within Canada uses the **EFT** rail (CPA batch, next-day). An account is
identified by `Institution Code (3) + Transit (5) + Account Number`.

| Rail | Account# based | Speed | Used here |
|------|----------------|-------|-----------|
| **EFT Payments** | ✅ | batch / next-day | **Yes — local FT** |
| Real-time (INTERAC) | mostly alias | instant | No |
| Wire | ✅ | same-day | No (large/intl) |

## 2. Flow

```
caller ──(X-Secret-Key | JWT)──► middleware-third-party ──(panel/Vault creds)──► Scotia TranXact
        validate → create → submit → inquire                                      (env-routed: DEV/PROD)
```

| Step | API code | Method + path |
|------|----------|---------------|
| 1. Validate account | `SCOTIABANK_ACCOUNT_VALIDATION` | `POST /treasury/validation/v2/account-validation` |
| 2. Create EFT (draft) | `SCOTIABANK_EFT_CREATE` | `POST /treasury/payments/eft/v1/payments` |
| 3. Submit (execute) | `SCOTIABANK_EFT_SUBMIT` | `POST /treasury/payments/eft/v1/submissions/{submissionId}` |
| 4. Inquire (status) | `SCOTIABANK_EFT_INQUIRE` | `GET /treasury/payments/eft/v1/submissions/{submissionId}` |

Step 2 returns `submission_id` → use it as the `{submissionId}` path param for steps 3 & 4.

## 3. Authentication model — two separate tokens

- **Caller → middleware**: customer JWT (external `/{apiCode}`) **or** `X-Secret-Key` only
  (`/{apiCode}/simple`, pre-login / internal). The JWT never reaches Scotia.
- **Middleware → Scotia**: per-API headers from the env config (panel/Vault managed). **No OAuth
  token endpoint** — Scotia uses direct API-key / signature auth:
  - Account validation: `x-api-key` + `customer-profile-id` + `x-country-code: CA`
  - Create EFT: `customer-profile-id` + **`x-jws-signature`** (detached RS256 over the body) — no `x-api-key`
  - Submit / Inquire: `customer-profile-id` (+ `x-channel-id`, `x-originating-appl-code`)

## 4. Environment routing (client-secret driven)

`ExecuteApiService` resolves the calling client by `X-Secret-Key`, reads its environment, and:

| Client env | Target | Audit table |
|------------|--------|-------------|
| TEST | local `ScotiaBankMockProvider` (canned responses) | `client_request_test` |
| DEV  | Scotia hosted sandbox `https://developer.api.scotiabank.com/mock/{33,38}` | `client_request_dev` |
| PROD | `https://api.scotiabank.com` (creds TBD via panel/Vault) | `client_request_prod` |

Full request + response (headers, body, status, duration) are persisted as JSONB in the
env-specific table. Secrets (`x-api-key`, `x-jws-signature`, …) are redacted in the stored headers.

Clients seeded by V18:
- `TEST_MOCK_CLIENT` (TEST) — secret `test-mock-secret-key-2026`
- `PAYMENT_SERVICE` (DEV) — secret `payment-service-dev-secret-2026`

## 5. Dynamic header tokens

Env-config header values may contain tokens resolved per request by `ExecuteApiService.resolveDynamicHeaders`:

| Token | Resolves to |
|-------|-------------|
| `{{TRACE_ID}}` / `{{SPAN_ID}}` | fresh 16-hex B3 ids |
| `{{JWS}}` | detached RS256 JWS over the outgoing body, signed with `credentials.jwsPrivateKey` (PKCS#8 PEM) |

DEV uses Scotia's documented sample `x-jws-signature` (sandbox mock does not verify).
**PROD uses `{{JWS}}` + the real signing key** — set `credentials.jwsPrivateKey` (PKCS#8) and the
real `customer-profile-id` / `x-api-key` on the SCOTIABANK PROD env configs via the panel.

JWS generator: `infrastructure/crypto/JwsSignatureUtil.detachedRs256()` (Nimbus JOSE, RS256).
Unit test: `JwsSignatureUtilTest`.

## 6. How a caller invokes (examples)

TEST / pre-login (no JWT), via the public `/simple` endpoint (default client = `TEST_MOCK_CLIENT`):

```bash
# 1. validate
curl -X POST http://middleware-third-party:8093/api/v1/execute/SCOTIABANK_ACCOUNT_VALIDATION/simple \
  -H 'Content-Type: application/json' \
  -d '{"account_information":{"account_number":"800020403814","institution_number":"002","transit":"80002","name":{"full_name":"Test 1 Company"}}}'

# 3. submit (submission id from create as path param)
curl -X POST http://middleware-third-party:8093/api/v1/execute/SCOTIABANK_EFT_SUBMIT/simple \
  -H 'X-Path-Params: {"submissionId":"1000000001"}'
```

External authenticated callers use `POST /api/v1/execute/{apiCode}` with `Authorization: Bearer <jwt>`
+ `X-Secret-Key: <client secret>` (DEV client → DEV urls → `client_request_dev`).

## 7. Reusability

Any service calls the middleware by API code — Scotia stays an implementation detail. Adding RBC/TD
later = register a new provider + APIs + env configs; callers are unchanged. The JWS/trace token
mechanism is provider-agnostic (used by any provider needing body signing).

## 8. Files

| File | Purpose |
|------|---------|
| `db/migration/V18__add_scotiabank_eft_apis.sql` | 4 EFT APIs + TEST/DEV/PROD env configs + client grants |
| `adapter/mock/provider/ScotiaBankMockProvider.java` | TEST canned responses for the 4 EFT codes |
| `infrastructure/crypto/JwsSignatureUtil.java` | detached RS256 JWS signer |
| `application/usecase/ExecuteApiService.java` | dynamic header resolution (`{{JWS}}`/`{{TRACE_ID}}`/`{{SPAN_ID}}`) |

## 9. Pending before PROD

- Fill PROD `customer-profile-id`, `x-api-key`, and `jwsPrivateKey` (PKCS#8) via the panel.
- Confirm the EFT **Inquire** endpoint shape (registered as `GET /…/submissions/{submissionId}` —
  best-effort; adjust if Scotia's spec differs).
- DEV live call needs outbound reach to `developer.api.scotiabank.com` + a JWT on the external endpoint.
