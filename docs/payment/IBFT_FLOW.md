# IBFT (Inter-Bank Funds Transfer) via Scotia EFT — wallet-service

Bank-grade IBFT: a customer sends money from their wallet to an **external Canadian bank account** via
Scotiabank **EFT** (batch/clearing rail — async settlement). Funds are **held** on initiate, **finalized**
(debited) on settlement success, **released** on failure. Reconciled by a cron polling Scotia INQUIRE.

## State machine
```
INITIATED → HELD → SUBMITTED → PROCESSING → COMPLETED   (settled → final debit)
                       ↘ FAILED                          (create/submit reject → release)
                                  ↘ FAILED                (settlement rejected → release)
```
- **HELD**: Fineract `holdAmount` blocks the amount (availableBalance ↓), `reserved_balance` ↑,
  `IBFT_HOLD` movement, GL `Dr Consumer Wallet 110401 / Cr IBFT Clearing 120602`. No double-spend.
- **COMPLETED**: Fineract `releaseAmount` then `withdraw` → money truly leaves; `IBFT_DEBIT` movement; reserved → 0.
- **FAILED**: Fineract `releaseAmount` → funds restored; `IBFT_RELEASE` movement; GL reversed.

## Money safety
- Fineract is the source of truth; hold prevents double-spend at source.
- Idempotent: `UNIQUE(tenant, idempotency_key)` on `ibft_transactions`; movement keys `key:HOLD|:DEBIT|:RELEASE`;
  Scotia idempotency via `end_to_end_id`. Finalize/release are status-guarded (cron-safe, no double debit).
- Debtor (sender) = authenticated user (JWT `mobile_number` → wallet) — never a body param.
- Daily/monthly limits enforced (`TransactionLimitEnforcer`, counts `IBFT_HOLD`).

## Endpoints (JWT)
**Beneficiaries** (`@SecuredEndpoint obj=ibft.beneficiaries`):
- `POST /api/v1/ibft/beneficiaries` — `{nickname, beneficiaryName, institutionNumber, transit, accountNumber, bankName, currency}`
  → Scotia `ACCOUNT_VALIDATION`, dedupe, save. Duplicate → `IBFT.BENEFICIARY.DUPLICATE`.
- `GET /api/v1/ibft/beneficiaries` · `GET …/{id}` · `DELETE …/{id}` (deactivate)

**Transfers** (`obj=ibft.transfers`):
- `POST /api/v1/ibft/transfers` — `{beneficiaryId, amount, currency, purposeNote}`, header `X-Idempotency-Key`
  → 202 SUBMITTED (held + Scotia create+submit) / 422 on insufficient/limit/failed.
- `GET …/{id}` · `GET …/by-wallet/{walletId}`

**Internal**: `POST /internal/wallets/ibft/reconcile` — trigger reconciliation on demand (cron also runs `0 */2 * * * *`).

## Scotia EFT rail (middleware)
`ScotiaEftClient` → middleware `/api/v1/execute/{apiCode}/simple` (TEST mock in DEV):
`SCOTIABANK_ACCOUNT_VALIDATION` → `_EFT_CREATE` (JWS, → submission_id) → `_EFT_SUBMIT` (X-Path-Params submissionId)
→ `_EFT_INQUIRE` (status COMPLETED + payments[].SETTLED). Response double-wrapped (`data.responseBody.data`).

## Reconciliation cron
`IbftReconciliationCronService` (`@Scheduled ${ksa.wallet.ibft.recon.cron}`): query SUBMITTED/PROCESSING →
`INQUIRE` → settled→finalize, rejected→release, pending→keep. Per-transfer `IbftSettlementService` in its own
`REQUIRES_NEW` tx. Writes `ibft_reconciliation_logs`.

## Tables (wallet-service V18–V20)
`ibft_beneficiaries`, `ibft_transactions` (+ `ibft_status` enum, `IBFT_HOLD|DEBIT|RELEASE` purposes),
`ibft_status_history` (trigger), `scotia_eft_logs`, `ibft_reconciliation_logs`.
GL: ledger-service `120602 IBFT Settlement Clearing`. Casbin: identity V43.

## Fineract hold plumbing (cross-service)
`ledger-fineract-client-sdk` `holdAmount`/`releaseHold` → ledger-service proxy `/accounts/{id}/hold-amount`
(Fineract `command=holdAmount`, injects `reasonForBlock` code-value) + `/transactions/{holdTxnId}/release-amount`
(`command=releaseAmount`). Wallet `FineractSavingsPort.hold/releaseHold`.

## Smoke test (DEV, Scotia mock)
```bash
TOK=<customer JWT with mobile_number>
# 1. Add beneficiary
curl -X POST $W/api/v1/ibft/beneficiaries -H "Authorization: Bearer $TOK" -d \
  '{"beneficiaryName":"Jane Smith","institutionNumber":"003","transit":"12345","accountNumber":"6789012","bankName":"RBC"}'
# 2. Initiate (held + submitted)
curl -X POST $W/api/v1/ibft/transfers -H "Authorization: Bearer $TOK" -H "X-Idempotency-Key: k1" -d \
  '{"beneficiaryId":"<id>","amount":40,"currency":"CAD"}'   # → SUBMITTED; available -40, reserved +40
# 3. Reconcile → settle → final debit
curl -X POST $W/internal/wallets/ibft/reconcile                # → settled=1; reserved 0, COMPLETED
```
Verified DEV: hold/settle, idempotency (same key → same ibft), insufficient → 422, duplicate beneficiary → 422,
movements (IBFT_HOLD + IBFT_DEBIT), ledger (IBFT_OUTBOUND POSTED).
