# External Fund Transfer (Scotia RTP) — wallet-service

Move money between a customer wallet and an **external Canadian bank account** via Scotiabank
Real-Time Payments (RTP / INTERAC e-Transfer for Business), routed through `middleware-third-party`.

## Model
- Every wallet is assigned a **virtual Canadian-format account number** at creation:
  `<FI>-<transit>-<7-digit seq>` e.g. `002-80150-0000123` (config: `ksa.wallet.scotia.fi-number`,
  `ksa.wallet.scotia.transit`). Exposed on wallet read responses (`accountNumber`).
- The platform moves money via **one shared corporate Scotia account**
  (`ksa.wallet.scotia.corporate-account`); the user's virtual account number is the remittance reference.
- DEV: Scotia RTP runs in **mock** (middleware default client `TEST_MOCK_CLIENT` → `ScotiaBankMockProvider`
  returns `status:"SUCCESS"`). No real money moves.

## Money mechanics
- Balance source of truth = Fineract (via ledger-service savings proxy).
- OUTBOUND: Scotia RTP commit → `fineract.withdraw` (debit) → `wallet_movements` `TRANSFER_OUT` →
  GL: **Dr Consumer Wallet `110401` / Cr Scotia RTP Clearing `120601`**.
- INBOUND: reuse `CreditWalletUseCase` (`fineract.deposit`) → `wallet_movements` `TRANSFER_IN` →
  GL: **Dr Scotia RTP Clearing `120601` / Cr Consumer Wallet `110401`**.
- All legs are idempotent (unique `(tenant_id, idempotency_key)`). GL posting is best-effort.

## Endpoints

### Outbound (JWT, `@SecuredEndpoint("wallet.transfers","create")`)
`POST /api/v1/wallets/transfers/external`  — header `X-Idempotency-Key`
```json
{
  "sourceWalletId": "<uuid>",          // optional; else resolved from JWT user
  "counterpartyName": "Jane External",
  "counterpartyAccount": "003-12345-6789012",   // required (external bank account)
  "counterpartyEmail": "jane@example.ca",
  "counterpartyBankCode": "003",
  "amount": 30,
  "currency": "CAD",
  "purposeNote": "Rent payment"
}
```
→ `200 COMPLETED` with `scotiaPaymentId`, `scotiaClearingRef`, `movementId`, `ledgerEntryId`.

`GET /api/v1/wallets/transfers/external/{id}` — read one
`GET /api/v1/wallets/transfers/external/by-wallet/{walletId}?page&size&search` — paginated list

### Inbound (internal, no JWT — future: Scotia inbound webhook)
`POST /internal/wallets/external-credit`  — header `X-Tenant-Id`
```json
{
  "accountNumber": "002-80150-0000087",   // or "customerId"
  "amount": 50,
  "currency": "CAD",
  "senderName": "Acme Corp",
  "senderAccount": "003-12345-6789012",
  "reference": "PO-9",
  "idempotencyKey": "ext-in-001"
}
```
→ wallet credited, `external_fund_transfers` INBOUND COMPLETED, GL posted.

### Listing
External legs also surface in the unified history `GET /api/v1/wallets/{walletId}/transactions`
(as `TRANSFER_OUT` / `TRANSFER_IN` movements).

## Smoke test (DEV)
```bash
TOKEN=$(curl -s -X POST http://localhost:8000/identity-service/api/v1/auth/login \
  -H "Content-Type: application/json" -d '{"username":"superadmin","password":"Admin@1234"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')

# Outbound (wallet -> external bank)
curl -s -X POST http://localhost:8088/api/v1/wallets/transfers/external \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "X-Idempotency-Key: ext-out-1" \
  -d '{"sourceWalletId":"<walletId>","counterpartyAccount":"003-12345-6789012","amount":30,"currency":"CAD"}'

# Inbound (external bank -> wallet)
curl -s -X POST http://localhost:8088/internal/wallets/external-credit \
  -H "Content-Type: application/json" -H "X-Tenant-Id: 00000000-0000-0000-0000-000000000001" \
  -d '{"accountNumber":"002-80150-0000087","amount":50,"currency":"CAD","idempotencyKey":"ext-in-1"}'
```

## Key files
- Migrations: `wallet-service V14__external_fund_transfers.sql`, `ledger-service V20__seed_scotia_rtp_clearing_account.sql`, `identity-service V41__add_wallet_external_transfer_policies.sql`
- Use cases: `InitiateExternalTransferService`, `RecordInboundTransferService`
- Adapters: `infrastructure/scotia/ScotiaRtpClient`, `infrastructure/ledger/LedgerJournalClient`
- REST: `adapter/rest/controller/ExternalTransferController`, `InternalWalletController#externalCredit`
- Config: `ksa.wallet.scotia.*`, `ksa.wallet.gl.accounts.*` in `application.yml`
