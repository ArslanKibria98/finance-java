# Ledger Service — Debit / Credit (Double-Entry) Guide

> **Urdu (Roman)**: Ye document batata hai ke **ledger-service** mein **debit** aur **credit** kaise kaam karte hain — har journal entry balanced hoti hai (total debit = total credit).

---

## 1. Basic concept (double-entry)

| Concept | Meaning |
|--------|---------|
| **Journal entry** | Ek accounting transaction jo **do ya zyada lines** par split hoti hai. |
| **Debit (Dr)** | Ek account par **debit side** — amount `debitAmount` field mein. |
| **Credit (Cr)** | Ek account par **credit side** — amount `creditAmount` field mein. |
| **Balanced entry** | Saari lines ka **sum(debits) = sum(credits)** — ye rule **domain + database** dono enforce karte hain. |

Currency: entries **SAR** use karte hain (`JournalEntryAggregate.create` sets currency `SAR`). Amounts **`BigDecimal`** hain, rounding **6 decimal places** par totals ke liye (`RoundingMode.HALF_UP`).

---

## 2. Single line (`JournalLine`) — kaise define hota hai

Har line **ek hi account** ko hit karti hai aur **ya to debit ya credit** — dono positive amounts ek hi line par allowed **nahi**.

**Factory methods:**

- `JournalLine.debit(accountId, amount, description, lineNumber)` → `debitAmount = amount`, `creditAmount = 0`
- `JournalLine.credit(accountId, amount, description, lineNumber)` → `creditAmount = amount`, `debitAmount = 0`

**Invariants** (validation agar fail ho to `IllegalArgumentException`):

1. Ek line par **debit aur credit dono positive** — **invalid**.
2. Dono zero — **invalid** (line must have Dr **ya** Cr > 0).
3. Negative amounts — **invalid**.

**Code:** `services/ledger-service/src/main/java/com/ksa/financing/ledger/domain/model/JournalLine.java`

---

## 3. Poori entry (`JournalEntryAggregate`) — balance rule

Jab entry **create** hoti hai:

1. Kam az kam **2 lines** zaroori hain.
2. **Total debit** = har line ka `debitAmount` ka sum.
3. **Total credit** = har line ka `creditAmount` ka sum.
4. **Total debit must equal total credit** — warna: `Journal entry is unbalanced: debits=... credits=...`
5. Kam az kam **ek debit line** aur **ek credit line** honi chahiye (sirf ek taraf ka sum zero ho to reject).

**Code:** `services/ledger-service/src/main/java/com/ksa/financing/ledger/domain/model/JournalEntryAggregate.java` — method `create(...)`.

---

## 4. Database layer

Table `journal_entries` par constraint:

```sql
CONSTRAINT chk_journal_entry_balanced CHECK (total_debit = total_credit)
```

**Migration:** `services/ledger-service/src/main/resources/db/migration/V1__initial_schema.sql`

Matlab persistence ke level par bhi **debit total = credit total** maintain rehta hai.

---

## 5. Posting flow (high level)

1. Caller **lines** banata hai (`List<JournalLine>`).
2. `PostJournalEntryUseCaseImpl.post(PostJournalEntryCommand)`:
   - Pehle **idempotency key** check — duplicate ho to purani entry return.
   - Phir `JournalEntryAggregate.create(...)` → domain balance validate.
   - Phir **POSTED** mark, save, events publish.

**Code:** `services/ledger-service/src/main/java/com/ksa/financing/ledger/application/usecase/PostJournalEntryUseCaseImpl.java`

---

## 6. Example: loan disbursement (Dr Loans Receivable, Cr Bank)

`GlPostingService.postDisbursement` ye lines banata hai:

| Line | Side | Account role (conceptual) | Amount |
|-----|------|---------------------------|--------|
| 1 | **Debit** | Loans receivable (resolved per tenant) | principal |
| 2 | **Credit** | Bank / cash account | principal |

**Code:** `services/ledger-service/src/main/java/com/ksa/financing/ledger/infrastructure/messaging/GlPostingService.java`

Accounts **codes** se resolve hote hain (`LedgerAccountCodes`, `LedgerAccountResolver`) — har tenant apna COA use kar sakta hai.

---

## 7. Example: admin simple movement (offset account ke saath)

`PostSimpleAccountMovementUseCaseImpl` user intent par ye banata hai:

| Movement enum | Line 1 | Line 2 |
|---------------|--------|--------|
| `CREDIT_ON_ACCOUNT` | **Credit** target account | **Debit** configured offset account |
| `DEBIT_ON_ACCOUNT` | **Debit** target account | **Credit** configured offset account |

Dono lines ka amount **same** — entry **hamesha balanced**.

**Code:** `services/ledger-service/src/main/java/com/ksa/financing/ledger/application/usecase/PostSimpleAccountMovementUseCaseImpl.java`

Offset account env se: `ledger.admin-adjustment.offset-account-code` / `LEDGER_ADMIN_ADJUSTMENT_OFFSET_ACCOUNT_CODE` (see `LedgerAdminAdjustmentProperties`).

---

## 8. Multi-line entries

Multiple debits / multiple credits **allowed** hain jab tak **sum(debits) = sum(credits)** — domain tests cover karte hain (`JournalEntryAggregateTest` — multiple debit lines + single credit).

---

## 9. Related docs

| File | Topic |
|------|--------|
| `docs/GL_TRANSACTION_FLOW.md` | Lending/collections se GL posting flows |
| `docs/chart-of-accounts-sample.md` | COA / tenant isolation |
| `docs/GL_API_STRUCTURE.md` | REST structure |

---

## 10. Quick reference (implementation map)

| Topic | Primary class |
|-------|----------------|
| Line-level Dr/Cr rules | `JournalLine` |
| Entry-level balance | `JournalEntryAggregate.create` |
| Post + idempotency | `PostJournalEntryUseCaseImpl` |
| Loan event → journals | `GlPostingService` |
| Admin Dr/Cr UI/API | `PostSimpleAccountMovementUseCaseImpl` |

---

*Last aligned with ledger-service domain model in this repository; Fineract sync details see `docs/GL_TRANSACTION_FLOW.md`.*
