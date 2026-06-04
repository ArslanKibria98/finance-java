# Scotiabank Real-Time Payments (RTP) Integration — INTERAC e-Transfer* for Business

> Status: **Implemented** in `middleware-third-party`. Rail = **RTP** (alias / account-number
> instant payment via INTERAC e-Transfer for Business). Routed through the existing third-party
> gateway — no caller holds Scotia credentials. Provider registered in
> `V16__add_scotiabank_provider.sql`; corrected by `V19` (deactivate phantom APIs),
> `V20` (real `/treasury/payments/rtp/v1/*` paths + `payment-id-source` header), and
> `V21` (DEV → local mock — see §5). Verified against the Scotia sandbox curls (mock proxy 28).

---

## 1. RTP vs the EFT rail

| Rail | Recipient by | Speed | Doc |
|------|--------------|-------|-----|
| **Real-Time Payments (this)** | alias (email/mobile) **or** account number | instant | this file |
| EFT Payments | account number only | batch / next-day | [SCOTIA_EFT_INTEGRATION.md](SCOTIA_EFT_INTEGRATION.md) |

Source: `developer.api.scotiabank.com` → APIs → Payments → **Real-time Payments** (`real-time-payments/v1-0-17`).

## 2. The 5 real endpoints (and which are mandatory)

The product exposes **exactly 5** endpoints. (V16 had also speculatively registered
`/v1/payments/validate` and `POST /v1/payments`; these are **not** part of this product and are
**deactivated** by V19.)

All paths are prefixed `/treasury/payments/rtp`.

| # | API code | Method + path | Role | Required in flow |
|---|----------|---------------|------|------------------|
| 1 | `SCOTIABANK_PAYMENT_OPTIONS_INQUIRY` | `POST /treasury/payments/rtp/v1/payment-options/inquiry` | Is the creditor reachable (Autodeposit / Real-time Account Deposit)? | **Yes** (eligibility gate) |
| 2 | `SCOTIABANK_PAYMENT_COMMIT` | `POST /treasury/payments/rtp/v1/payments/secure/commit-transaction` | **Execute (send) the payment** | **Yes** (the money move) |
| 3 | `SCOTIABANK_PAYMENT_SUMMARY` | `GET /treasury/payments/rtp/v1/payments/{id}/summary` | Payment status (poll) | **Yes** (confirm settlement) |
| 4 | `SCOTIABANK_PAYMENT_DETAILS` | `GET /treasury/payments/rtp/v1/payments/{payment-id}` | Full FI-to-FI status report | Optional (audit / dispute) |
| 5 | `SCOTIABANK_PAYMENT_CANCEL` | `POST /treasury/payments/rtp/v1/payments/{payment-id}/cancel` | Cancel an **unclaimed** e-Transfer | Optional (only when unclaimed) |

## 3. Flow

```
caller ─(X-Secret-Key | JWT)─► middleware-third-party ─(panel/Vault creds)─► Scotia RTP
                                                                              (env-routed)

 MANDATORY:   1 options-inquiry ──► 2 commit-transaction ──► 3 summary (poll until settled)
 OPTIONAL:    4 details (audit)            5 cancel (if still unclaimed)
```

- Step **1** returns the available `payment_options` (e.g. `REALTIME_ACCOUNT_DEPOSIT_PAYMENT`) + the
  max outgoing amount. If empty → recipient not eligible → stop.
- Step **2** returns `payment_id` + `clearing_system_reference` + `status`. **This is the only call
  that signs the body** (`x-jws-signature`).
- Step **3/4/5** take the `payment_id` from step 2 as a **path param** (`X-Path-Params`).
  Summary's `{id}` may also be your own `message_identification` — select via the `PaymentIdSource`
  header (`PAYMENT_ID` default, or `MESSAGE_IDENTIFICATION`).

## 4. Authentication model — two separate tokens

- **Caller → middleware**: customer JWT (external `/{apiCode}`) **or** `X-Secret-Key` only
  (`/{apiCode}/simple`, internal). The JWT never reaches Scotia.
- **Middleware → Scotia** (per-API headers from env config, panel/Vault managed; **no OAuth token endpoint**):
  - **All 5 calls** send `x-api-key` + `customer-profile-id` + `x-country-code: CA` + `x-b3-traceid`/`x-b3-spanid`.
  - **Commit-transaction additionally** sends **`x-jws-signature`** (detached RS256 over the body; sandbox accepts a blank/sample value, PROD uses a real key).
  - **Summary** additionally sends **`payment-id-source`** to say whether `{id}` is Scotia's
    `payment_id` or your own `message_identification`. The env config defaults it to **`PAYMENT_ID`**
    (the canonical/standard query — use the `payment_id` returned by commit). A caller may override it
    per request — see §4a.

### 4a. Standard summary-query flow + caller-overridable headers

The standard query uses the Scotia `payment_id` from the commit response (`payment-id-source: PAYMENT_ID`,
the seeded default — caller does nothing). To query by your own `message_identification` instead, the
caller overrides the header via the **`X-Forward-Headers`** request header (JSON map), e.g.
`X-Forward-Headers: {"payment-id-source":"MESSAGE_IDENTIFICATION"}`.

`X-Forward-Headers` is a generic, provider-agnostic pass-through: any header in it is merged onto the
outbound provider request, **except a protected set** the caller may never set/override —
`authorization`, `x-api-key`, `x-jws-signature`, `customer-profile-id`, `sullis-api-key`,
`client-secret`, `cookie` (middleware-managed credentials/identity). Attempts to override those are
ignored and logged. (Verified live: `payment-id-source` override lands on the request; a forged
`x-api-key` is dropped and the real managed key is used.)

## 5. Environment routing (client-secret driven)

| Client env | Target | Audit table |
|------------|--------|-------------|
| TEST | local `ScotiaBankMockProvider` (canned responses) | `client_request_test` |
| DEV  | `credentials.mockMode=true` → local mock (real shapes) | `client_request_dev` |
| PROD | `https://api.scotiabank.com` (creds TBD via panel/Vault) | `client_request_prod` |

> **Why DEV is mock-mode, not live (V21).** The Scotia "Try it" sandbox URL
> `https://developer.api.scotiabank.com/mock/28/...` is **gated behind a logged-in developer-portal
> browser session**. A server-to-server call with only `x-api-key` returns **HTTP 302 → `/session-error`**
> (verified). So it cannot be hit from the middleware container. DEV therefore serves the same canned
> RTP shapes locally. **To go live:** when Scotia issues real server-to-server credentials against a
> reachable host (`api.scotiabank.com` or a true sandbox), panel → Providers → SCOTIABANK → Env Config
> (DEV) → set `base_url` + `x-api-key` + `customer-profile-id` (+ `jwsPrivateKey` for commit) and remove
> `mockMode`. Paths/headers are already correct, so it's a config-only flip.

Full request + response persisted as JSONB; secrets (`x-api-key`, `x-jws-signature`) redacted.

Clients: `TEST_MOCK_CLIENT` (TEST, all RTP APIs from V16) and `PAYMENT_SERVICE`
(DEV, granted the 5 RTP APIs by V19) — secret `payment-service-dev-secret-2026`.

## 6. Dynamic header tokens (provider-agnostic)

| Token | Resolves to |
|-------|-------------|
| `{{TRACE_ID}}` / `{{SPAN_ID}}` | fresh 16-hex B3 ids |
| `{{JWS}}` | detached RS256 JWS over the outgoing body, signed with `credentials.jwsPrivateKey` (PKCS#8 PEM) |

DEV/TEST commit uses Scotia's documented sample `x-jws-signature` (mock does not verify).
**PROD commit uses `{{JWS}}` + the real `credentials.jwsPrivateKey`.**
Signer: `infrastructure/crypto/JwsSignatureUtil.detachedRs256()`.

## 7. How a caller invokes (examples)

```bash
# 1. eligibility — is this recipient reachable?
curl -X POST http://middleware-third-party:8093/api/v1/execute/SCOTIABANK_PAYMENT_OPTIONS_INQUIRY/simple \
  -H 'Content-Type: application/json' \
  -d '{"creditor":{"deposit_handle":{"type":"EMAIL","value":"payee@example.com"}}}'

# 2. send the payment — SIMPLE body; middleware expands it to the full Scotia payload
#    (see "Simple request bodies" below). Body is signed via x-jws-signature.
curl -X POST http://middleware-third-party:8093/api/v1/execute/SCOTIABANK_PAYMENT_COMMIT/simple \
  -H 'Content-Type: application/json' \
  -d '{"amount":10.01,"debtorAccount":"002-80150-6666600","creditorAccount":"002-80010-9999999"}'
#    → returns payment_id (e.g. 6000792002)

# 3. status — standard: query by Scotia payment_id (payment-id-source defaults to PAYMENT_ID)
curl -X POST http://middleware-third-party:8093/api/v1/execute/SCOTIABANK_PAYMENT_SUMMARY/simple \
  -H 'X-Path-Params: {"id":"6000792002"}'
#    ...or query by your own message_identification:
curl -X POST http://middleware-third-party:8093/api/v1/execute/SCOTIABANK_PAYMENT_SUMMARY/simple \
  -H 'X-Path-Params: {"id":"2450779"}' \
  -H 'X-Forward-Headers: {"payment-id-source":"MESSAGE_IDENTIFICATION"}'

# 4. full details / 5. cancel — same path-param pattern
curl ... /SCOTIABANK_PAYMENT_DETAILS/simple -H 'X-Path-Params: {"payment-id":"6000792002"}'
curl -X POST ... /SCOTIABANK_PAYMENT_CANCEL/simple -H 'X-Path-Params: {"payment-id":"6000792002"}'
```

External authenticated callers use `POST /api/v1/execute/{apiCode}` with `Authorization: Bearer <jwt>`
+ `X-Secret-Key: <client secret>`. GET/summary/details use `X-Path-Params` (JSON) for the id.

### Simple request bodies (request templates)

Callers send a **simple flat body**; the middleware expands it into the exact Scotia payload using a
per-API template stored in `provider_apis.request_template` (`JsonTemplateRenderer`). Static structure
(addresses, scheme codes, priorities…) is baked into the template; only dynamic fields are sent, and
numbers stay numbers.

| API | Simple fields (others have defaults) |
|-----|--------------------------------------|
| Options Inquiry | `depositHandle` (`depositType`, `productCode` optional) |
| Commit | `amount`, `debtorAccount`, `creditorAccount` (`currency`, `messageIdentification`, names/emails optional) |
| Cancel | `reason` (optional) |

e.g. `{"amount":10.01,"debtorAccount":"002-80150-6666600","creditorAccount":"002-80010-9999999"}`
→ middleware builds the full `initiation{…}` body. The audit row (`client_request_*`) stores the
expanded body that was actually sent/signed.

## 8. Files

| File | Purpose |
|------|---------|
| `db/migration/V16__add_scotiabank_provider.sql` | original SCOTIABANK provider + RTP APIs |
| `db/migration/V19__fix_scotiabank_rtp_rail.sql` | deactivated 2 phantom APIs, commit=JWS, DEV grants |
| `db/migration/V20__fix_scotiabank_rtp_paths_and_dev_live.sql` | real `/treasury/payments/rtp/v1/*` paths, `x-api-key` on all, `payment-id-source` header |
| `db/migration/V21__scotiabank_rtp_dev_mockmode.sql` | DEV → local mock (portal proxy is session-gated, returns 302) |
| `adapter/mock/provider/ScotiaBankMockProvider.java` | canned responses for all RTP codes (details = `fi_to_fi_payment_status_report`) |
| `adapter/rest/controller/ApiExecutionController.java` | `X-Forward-Headers` pass-through (lets callers set `payment-id-source` etc.) |
| `application/usecase/ExecuteApiService.java` | applies forwarded headers + protected-header blocklist (`buildHeaders`/`buildMultipartHeaders`) |
| `application/usecase/ExecuteApiService.java` | env routing + `{{JWS}}`/`{{TRACE_ID}}`/`{{SPAN_ID}}` resolution |
| `infrastructure/crypto/JwsSignatureUtil.java` | detached RS256 JWS signer |

## 9. Pending before PROD

- Fill PROD `customer-profile-id`, `x-api-key`, and `jwsPrivateKey` (PKCS#8) via the panel.
- Obtain **server-to-server** RTP credentials + a reachable host from Scotia (the portal `mock/28`
  proxy is browser-session-gated → 302). Set them on DEV and drop `mockMode` for live DEV calls.
- Request/response shapes verified against the Scotia sandbox curls (mock proxy 28); mocks match them 1:1.
