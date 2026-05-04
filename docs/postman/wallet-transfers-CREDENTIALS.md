# Wallet Transfer — Test Credentials

## Real Keycloak users assigned to demo wallets

| Wallet | Wallet ID | Owner (NID) | PIN | customerId | Mobile | Balance | Status |
|--------|-----------|-------------|-----|------------|--------|---------|--------|
| **A** | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` | `1010219431` | `147852` | `62d36093-c494-4133-8a11-79fecad3b465` | `+966561343161` | 10,000 SAR | ACTIVE ✅ |
| **B** | `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb` | `1211111111` | `147852` | `c0a61112-3d36-49ab-9256-16f26eb69b35` | `+966591591591` |  5,000 SAR | ACTIVE ✅ |
| **C** | `cccccccc-cccc-cccc-cccc-cccccccccccc` | `1234567010` | `147852` | `b01a5560-f62e-49c5-8f74-b916b28b7762` | `+966555687010` |      0 SAR | FROZEN ❌ |

```
Tenant: 00000000-0000-0000-0000-000000000001  (from JWT tenant_id claim)
Realm:  CompanyRealm
```

## 1. Login (get JWT)

### As Wallet A owner
```bash
curl -X POST 'http://localhost:8000/identity-service/api/v1/auth/login-with-pin' \
  -H 'Content-Type: application/json' \
  -H 'X-Device-Id: demo-device-A' \
  -d '{"nationalId":"1010219431","pin":"147852"}'
```

Response includes `data.accessToken` — copy it.

### As Wallet B owner
```bash
curl -X POST 'http://localhost:8000/identity-service/api/v1/auth/login-with-pin' \
  -H 'Content-Type: application/json' \
  -H 'X-Device-Id: demo-device-B' \
  -d '{"nationalId":"1211111111","pin":"147852"}'
```

## 2. Get balance

```bash
curl 'http://localhost:8000/wallet-service/api/v1/wallets/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/balance' \
  -H 'Authorization: Bearer <ACCESS_TOKEN_FROM_STEP_1>'
```

## 3. Initiate transfer A → B (1000 SAR)

```bash
curl -X POST 'http://localhost:8000/wallet-service/api/v1/wallets/transfers' \
  -H 'Authorization: Bearer <ACCESS_TOKEN_FROM_STEP_1>' \
  -H 'Content-Type: application/json' \
  -H 'X-Idempotency-Key: 11111111-2222-3333-4444-555555555555' \
  -d '{
    "sourceWalletId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "destinationWalletId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    "amount": 1000.00,
    "currency": "SAR",
    "purposeNote": "Test transfer A to B"
  }'
```

## 4. Postman setup

Postman collection variables already updated with these UUIDs.
After login, paste accessToken into `{{jwt}}` collection variable, then run requests 1→12.

## ⚠️ After applying V4 migration

Container needs rebuild + restart so V4 runs:
```bash
docker compose stop wallet-service
docker compose build wallet-service
docker compose up -d wallet-service
docker logs ksa-wallet-service 2>&1 | grep -iE "flyway|V4"
```

Verify wallets reassigned:
```bash
docker exec ksa-postgres psql -U postgres -d wallet_db -c \
  "SELECT id, wallet_number, tenant_id, customer_id, available_balance, status FROM wallets WHERE wallet_number LIKE 'WLT-DEMO-%';"
```
