# Scotiabank Wire Payments Integration — One-Time Wire Transfer

> Status: **Implemented** in `middleware-third-party` (`V23__add_scotiabank_wire_apis.sql`).
> Rail = **Wire** (one-time transfer to an account in Canada, the U.S., or internationally).
> Third Scotia rail after [EFT](SCOTIA_EFT_INTEGRATION.md) and [RTP](SCOTIA_RTP_INTEGRATION.md).
> Same provider-agnostic gateway — no caller holds Scotia credentials. Product: `wire-payments/v1-0-4`,
> verified against the Scotia sandbox curls (mock proxy 40).

---

## 1. The 3 endpoints

All paths prefixed `/treasury/payments/wire/v1`.

| # | API code | Method + path | Auth | Flow |
|---|----------|---------------|------|------|
| 1 | `SCOTIABANK_WIRE_VALIDATE` | `POST /treasury/payments/wire/v1/payments/validate` | **x-jws-signature** | **Yes** — validate before create |
| 2 | `SCOTIABANK_WIRE_CREATE` | `POST /treasury/payments/wire/v1/payments` | **x-jws-signature** | **Yes** — create the wire (returns `payment_id`) |
| 3 | `SCOTIABANK_WIRE_INQUIRE` | `GET /treasury/payments/wire/v1/payments/{payment-id}` | none | Optional — full details / status |

**Flow:** `1 validate → 2 create → 3 inquire`. `payment_id` from create feeds inquire via `X-Path-Params: {"payment-id":"..."}`.

## 2. Auth / headers (middleware → Scotia)

**No `x-api-key`** on this rail. Every call sends `x-channel-id: Online` + `x-country-code: CA` +
`customer-profile-id` + `x-b3-traceid`/`x-b3-spanid`. Validate + Create additionally send
**`x-jws-signature`** (detached RS256 over the body; sandbox sample, PROD `{{JWS}}` + real key).
Caller → middleware auth is the usual JWT (`/execute/{apiCode}`) or `X-Secret-Key` (`/simple`).

## 3. Environment routing

| Client env | Target | Audit table |
|------------|--------|-------------|
| TEST | local `ScotiaBankMockProvider` | `client_request_test` |
| DEV  | `credentials.mockMode=true` → local mock (real shapes) | `client_request_dev` |
| PROD | `https://api.scotiabank.com` (creds TBD via panel) | `client_request_prod` |

> DEV is mock-mode for the same reason as RTP: the portal "Try it" proxy
> `developer.api.scotiabank.com/mock/40` is browser-session-gated (server call → 302 `/session-error`).
> To go live: panel → SCOTIABANK → Env Config (DEV) → set real `base_url` + `customer-profile-id`
> (+ `jwsPrivateKey` for validate/create), remove `mockMode`.

Clients granted: `TEST_MOCK_CLIENT` (TEST), `SCOTIA_EFT_CLIENT` + `PAYMENT_SERVICE` (DEV).

## 4. Caller example

Callers send a **simple flat body** — the middleware expands it into the full Scotia wire payload via
the `request_template` (static addresses/codes baked in). Simple fields: `amount`, `debtorName`,
`debtorAccount`, `creditorName`, `creditorAccount` (`currency`, `messageIdentification`, `instructionId`,
town/province/postcode all optional with defaults).

```bash
SK={{scotiaDevSecret}}   # SCOTIA_EFT_CLIENT (DEV)
# 1. validate (simple body)
curl -X POST {kong}/api/v1/execute/SCOTIABANK_WIRE_VALIDATE -H "X-Secret-Key: $SK" \
  -H 'Content-Type: application/json' \
  -d '{"amount":500,"debtorName":"XYZ Communications","debtorAccount":"002-80002-0000515","creditorName":"ABC Technologies","creditorAccount":"353453535"}'
# 2. create → payment_id (e.g. 17000000001)   (same simple body)
curl -X POST {kong}/api/v1/execute/SCOTIABANK_WIRE_CREATE   -H "X-Secret-Key: $SK" \
  -H 'Content-Type: application/json' \
  -d '{"amount":500,"debtorName":"XYZ Communications","debtorAccount":"002-80002-0000515","creditorName":"ABC Technologies","creditorAccount":"353453535"}'
# 3. inquire by payment_id
curl -X POST {kong}/api/v1/execute/SCOTIABANK_WIRE_INQUIRE  -H "X-Secret-Key: $SK" \
  -H 'X-Path-Params: {"payment-id":"17000000001"}'
```

Postman: `SCOTIABANK (WIRE)` folder (Validate → Create captures `wirePaymentId` → Inquire).

## 5. Files

| File | Purpose |
|------|---------|
| `db/migration/V23__add_scotiabank_wire_apis.sql` | 3 Wire APIs + TEST/DEV(mockMode)/PROD env configs + grants |
| `adapter/mock/provider/ScotiaBankMockProvider.java` | `validatePaymentResponse` / `createPaymentResponse` / `wireInquireResponse` |

## 6. Pending before PROD

- Fill PROD `customer-profile-id` + `jwsPrivateKey` (PKCS#8) via the panel.
- Obtain server-to-server Wire creds + reachable host from Scotia (portal `mock/40` is session-gated → 302);
  set on DEV and drop `mockMode` for live calls.
