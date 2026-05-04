# Wallet-to-Wallet Transfer — Demo Scenario

> **Architecture**: Fineract = single source of truth for balance (Option A).
> wallet-service = transfer audit + idempotency layer only.
> Mobile-based recipient lookup via identity-service `/internal/users/lookup`.

## Files
- `wallet-transfers.postman_collection.json` — full Postman test scenarios (16 requests)
- `wallet-transfers-CREDENTIALS.md` — login credentials for test users
- Flyway migration: `services/wallet-service/src/main/resources/db/migration/V3__wallet_transfers.sql`
- Flyway migration: `services/wallet-service/src/main/resources/db/migration/V4__seed_real_demo_wallets.sql`

## Seed Data (loaded by V3 migration)

| Wallet | ID | Customer | Balance | Status |
|--------|----|----------|---------|--------|
| A | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` | `a0000000-0000-0000-0000-000000000001` | 10,000 SAR | ACTIVE |
| B | `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb` | `b0000000-0000-0000-0000-000000000002` |  5,000 SAR | ACTIVE |
| C | `cccccccc-cccc-cccc-cccc-cccccccccccc` | `c0000000-0000-0000-0000-000000000003` |      0 SAR | FROZEN |

Tenant: `11111111-1111-1111-1111-111111111111`

## Run

```
docker compose up -d postgres redis kafka keycloak kong wallet-service
```

Endpoints:
- Via Kong (use this in Postman): `http://localhost:8000/wallet-service/api/v1/...`
- Direct (bypass Kong):           `http://localhost:8088/api/v1/...`

Postman `baseUrl` default = `http://localhost:8000/wallet-service` (Kong route).
For direct: change `baseUrl` to `{{directUrl}}` (`http://localhost:8088`).

## Test Cases (in collection order)

| # | Scenario | Expected |
|---|----------|----------|
| 0 | Login as Wallet A owner | 200 OK, JWT auto-saved into `{{jwt}}` |
| 1-2 | GET balances (from Fineract) | A and B current balances |
| 3 | Lookup recipient by mobile +966591591591 | 200 OK, masked name, canReceive=true |
| 4 | Transfer **by mobile** A → B 250 SAR | 200 OK, COMPLETED, fineractTransferId set |
| 5 | Transfer **by walletId** A → B 1000 SAR | 200 OK, COMPLETED |
| 6 | Get transfer by ID | Returns transfer from #5 |
| 7 | List transfers for Wallet A | All sent + received transfers |
| 8 | ❌ Self-transfer | 422 WALLET.TRANSFER.SELF_NOT_ALLOWED |
| 9 | ❌ Insufficient balance | 422 WALLET.BALANCE.INSUFFICIENT (Fineract check) |
| 10 | ❌ Frozen destination | 422 WALLET.TRANSFER.DESTINATION_NOT_ACTIVE |
| 11 | ❌ Lookup unknown mobile | 200 OK, found=false |
| 12 | Idempotency first call (key=replay-001) | 200 OK, transferId saved |
| 13 | Idempotency replay (same key) | 200 OK, **same transferId** — no double-debit |
| 14-15 | Final balances (from Fineract) | Reflects all transfers |

## JWT Setup

**Auto-login**: Run request **0. Login** first — JWT saves automatically into `{{jwt}}` and all subsequent requests use it.

Manual: Get token from `/identity-service/api/v1/auth/login-with-pin` with `{nationalId, pin}`.

JWT must have:
- `tenant_id`: `00000000-0000-0000-0000-000000000001`
- `realm_access.roles` includes `customer`
- Casbin policies (already seeded for `customer` role):
  - `wallet.transfers:create | read | list | lookup`
  - `wallets:read | manage`

## Architecture (current)

```
Mobile app
    ↓ login + JWT
identity-service (Keycloak + user_identity_mapping for mobile↔customerId)
    ↓ /internal/users/lookup
wallet-service
    ├─ wallet_db: transfer audit + idempotency
    └─ Fineract: balance + GL + every transaction (single source of truth)
```

- **Balance reads**: GET /balance → Fineract via wallet-service
- **Transfer flow**: POST /transfers or /transfers/by-mobile → wallet-service → Fineract `/accounttransfers`
- **Mobile lookup**: GET /transfers/recipient?mobile=… → identity-service `/internal/users/lookup` → masked recipient view
- **Idempotency**: `wallet_transfers.idempotency_key` UNIQUE(tenant_id, key); replay returns cached transfer
- **Audit**: Every status transition recorded in `wallet_transfer_status_history` (DB trigger)
- **Events**: Kafka `financing.wallet.transfer.{initiated,completed,failed,reversed}`
