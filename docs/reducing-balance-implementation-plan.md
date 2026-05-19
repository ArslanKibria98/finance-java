# Reducing Balance Implementation Plan

> Source spec: [reducing-balance-load-documentation.md](reducing-balance-load-documentation.md)
> Status: PLAN ONLY — no code change
> Owner: lending-team + collections-team
> Last updated: 2026-05-18

---

## 1. Goal

Unify loan EMI calculation across the platform on the **reducing balance (amortized)** methodology defined in `reducing-balance-load-documentation.md`, replacing the current divergent flat-rate logic in:
- `lending-service` (offer side)
- `collections-service` (schedule side)

Result:
- One single source of truth for EMI math
- Offer EMI = Schedule EMI = Fineract EMI (no drift)
- Monthly profit decreases, principal increases (per doc)
- Audit-ready installment rows with opening/closing balance

---

## 2. Current State — Gap Analysis

### 2.1 Two Different Flat-Rate Formulas (mismatch)

| Concern | Offer side | Schedule side |
|---|---|---|
| Caller | `CreditCheckActivityImpl.calculateOffer` | `LoanEventListener.onLoanCreated` |
| Engine | `FinanceCalculationService.calculate` | `AmortizationScheduleGenerator.generateFlatSchedule` |
| Profit formula | `P × r × (n/12)` | `P × r` (no /12, no tenure factor) |
| VAT | Yes (4-case branch) | No |
| Fee handling | Folded into profit (case 2B) or excluded (case 2A) | Separate fee column distributed evenly |
| Equal when | `n == 12` only | — |

Concrete divergence (P=100,000, r=5% annual, n=24):
- Offer EMI ≈ 4,583 (profit 10,000)
- Schedule EMI ≈ 4,375 (profit 5,000)

### 2.2 Reducing Balance Code Exists But Unused

`AmortizationScheduleGenerator.generateDecliningBalanceSchedule` already implements the doc formula (EMI = P × r(1+r)^n / ((1+r)^n − 1)) — but **no production code path calls it**.

### 2.3 Persistence Lacks Audit Fields

`InstallmentJpaEntity` has `principal_amount`, `profit_amount`, `fee_amount`, `paid_*`. Missing for reducing balance audit per doc §11, §15:
- `opening_balance` (outstanding at start of installment)
- `closing_balance` (outstanding after installment)
- `monthly_profit_rate` (for reproducibility)

---

## 3. Target State

Single canonical engine in `domain-core-sdk` implementing reducing balance:

```
r            = annualRate / 12
EMI          = P × [r(1+r)^n] / [(1+r)^n − 1]
profit[i]    = openingBalance[i] × r
principal[i] = EMI − profit[i]
closing[i]   = opening[i] − principal[i]
```

Edge cases:
- `r = 0` → EMI = P/n, profit[i] = 0, principal[i] = P/n
- Last installment → absorbs rounding remainder so `closing[n] = 0` exactly
- Fee (admin / processing) → handled OUTSIDE EMI base. Either:
  - deducted at disbursement (inclusive), OR
  - added as separate per-installment fee column (non-inclusive)
- VAT → applied on **profit portion only** (per doc §12), separate column

---

## 4. Scope of Changes

| Layer | Component | Action |
|---|---|---|
| SDK | `domain-core-sdk` — `AmortizationScheduleGenerator` | Add canonical reducing-balance method that matches doc exactly (rename/fix existing `generateDecliningBalanceSchedule`) |
| SDK | `domain-core-sdk` — `InstallmentLine` record | Add `monthlyRate`, ensure `openingPrincipal` + `closingBalance` are populated |
| SDK | `domain-core-sdk` — `MurabahaCalculator`, `TawarruqCalculator` | Switch default `calculate(...)` to delegate to new reducing-balance engine; deprecate flat variant or keep behind explicit flag |
| lending-service | `FinanceCalculationService` | Replace internal flat math with delegation to `domain-core-sdk` reducing-balance engine |
| lending-service | `CreditCheckActivityImpl.calculateOffer` | No signature change — picks up new math via `FinanceCalculationService` |
| lending-service | `RescheduleActivityImpl` (currently labelled "Flat Rate") | Switch to reducing-balance for recalculated schedules |
| lending-service | `CalculateFinanceUseCaseImpl`, `CheckEligibilityUseCaseImpl`, `SuggestProductsUseCaseImpl`, `PreQualificationData`, `LoanApplicationController` (3 call sites) | Inherit new math via `FinanceCalculationService` — verify no caller bypasses it |
| collections-service | `LoanEventListener.onLoanCreated` | Replace `generateFlatSchedule(...)` with reducing-balance engine; ingest opening/closing balance |
| collections-service | DB — new Flyway `V13__add_balance_columns_to_installments.sql` | Add `opening_balance`, `closing_balance`, `monthly_rate` to `installments` |
| collections-service | `InstallmentJpaEntity` + persistence mapper | Map new columns |
| collections-service | `ManageRepaymentScheduleUseCase.CreateScheduleCommand.InstallmentEntry` | Add opening/closing fields |
| ledger-service | Fineract loan-product config | Verify Fineract loan product uses "Declining Balance — Equal Installments". Reconciliation must compare same schedule shape. |
| Postman / OpenAPI | Sample responses | Update example payloads showing new EMI breakdown |

---

## 5. Phased Rollout

### Phase 0 — Design lockdown (1–2 days)
- Sign-off this plan with Sharia board + product
- Confirm Sharia stance: declining balance is *permissible* under AAOIFI when rate is fixed and EMI is fixed (no compounding on unpaid profit)
- Pick: replace flat globally **OR** add `repayment_method` per product (`FLAT` / `REDUCING`) and migrate per-product
- Recommendation: **per-product flag** (safer rollout, existing loans untouched)

### Phase 1 — Engine in SDK (1 day)
- Add `generateReducingBalanceSchedule(...)` in `AmortizationScheduleGenerator` matching doc §3–§7 exactly
- Add `MurabahaCalculator.calculateReducingBalance(...)` wrapper
- Unit tests: doc example (P=100k, r=12%, n=12 → EMI≈8,884) + zero-rate + 24m + last-installment rounding
- No caller wiring yet

### Phase 2 — Lending-service offer (1 day)
- Branch `FinanceCalculationService.calculate(...)` on `repaymentMethod` param
- Default param value = `FLAT` so existing callers unchanged
- New `calculateReducing(...)` overload calls SDK engine
- Add `repaymentMethod` to `ProfitCalculationInput` so workflow can pass product setting
- Unit + integration tests for both modes

### Phase 3 — Collections-service schedule (1 day)
- Flyway V13 migration (nullable columns first → backfill → not-null later)
- Extend `InstallmentEntry` DTO with opening/closing
- Branch `LoanEventListener` on a flag on the `loan-created` event (`repaymentMethod` field added by lending-service)
- Persist opening/closing balance for new loans

### Phase 4 — Reschedule + early settlement (1 day)
- `RescheduleActivityImpl`: regenerate schedule using same method as original loan
- Early settlement (`reducing-balance-load-documentation.md` §16): outstanding = `closing_balance` of last paid installment; no future profit charged — already aligns with reducing balance semantics

### Phase 5 — Fineract alignment (1 day)
- Confirm Fineract loan product set to `Declining Balance — Equal Installments`
- `RunReconciliationUseCaseImpl`: ensure tolerance handles HALF_UP rounding diff between Java BigDecimal (scale 6/2) and Fineract internal scale
- Run reconciliation in dry-run for sample loans

### Phase 6 — Migration of in-flight loans (optional)
- Decision required: leave existing `FLAT` loans as-is, or rebuild their schedules
- Default recommendation: **leave as-is** — Sharia contract is signed at flat math; changing mid-contract requires customer consent and is rarely worth it
- New product variants opt-in to `REDUCING` going forward

### Phase 7 — Cleanup
- Mark `generateFlatSchedule(...)` deprecated once 100% of new originations use reducing balance
- Remove flat math path from `FinanceCalculationService` after sunset window (e.g. 6 months no-flat originations)

---

## 6. File-by-File Change List (no code yet — just intent)

### Shared SDK
- `shared-libraries/domain-core-sdk/src/main/java/com/ksa/financing/domain/sharia/AmortizationScheduleGenerator.java`
  - Add `generateReducingBalanceSchedule(principal, annualRate, tenure, startDate, feeAmount)` matching doc
  - Fix existing `generateDecliningBalanceSchedule` to use `RoundingMode.HALF_UP` consistently and last-installment remainder absorption
- `shared-libraries/domain-core-sdk/src/main/java/com/ksa/financing/domain/sharia/InstallmentLine.java`
  - Already has `openingPrincipal`, `closingPrincipal` — ensure both consistently populated by new method
- `shared-libraries/domain-core-sdk/src/main/java/com/ksa/financing/domain/sharia/MurabahaCalculator.java`
  - Add `calculateReducingBalance(...)` returning new `MurabahaCalculation` with reducing schedule
- `shared-libraries/domain-core-sdk/src/main/java/com/ksa/financing/domain/sharia/TawarruqCalculator.java`
  - Same as Murabaha
- `shared-libraries/domain-core-sdk/src/test/java/.../MurabahaCalculatorTest.java`, `AmortizationScheduleGeneratorTest.java`
  - Add reducing-balance assertion suite (use doc §6 example as golden)

### lending-service
- `services/lending-service/src/main/java/com/ksa/financing/lending/domain/service/FinanceCalculationService.java`
  - Add enum `RepaymentMethod { FLAT, REDUCING_BALANCE }`
  - Overload `calculate(...)` with `RepaymentMethod` param
  - REDUCING path delegates to SDK engine; FLAT path stays for backward compat
- `services/lending-service/src/main/java/com/ksa/financing/lending/adapter/temporal/activity/CreditCheckActivityImpl.java` (line 232)
  - Pass `input.repaymentMethod()` through
- `shared-libraries/lending-activity-api/src/main/java/com/ksa/islamic/orchestration/activity/lending/CreditCheckActivity.java`
  - Add `repaymentMethod` to `ProfitCalculationInput`
- `services/lending-service/src/main/java/com/ksa/financing/lending/adapter/temporal/workflow/LoanApplicationWorkflowImpl.java` (lines 990, 1066)
  - Read `repaymentMethod` from product config; pass to activity
- `services/lending-service/src/main/java/com/ksa/financing/lending/adapter/temporal/activity/RescheduleActivityImpl.java` (lines 275, 608)
  - Drop "Flat Rate" comment; use same method as original loan (lookup from loan record)
- `services/lending-service/src/main/java/com/ksa/financing/lending/adapter/rest/controller/LoanApplicationController.java` (lines 296, 495, 1928)
  - Plumb `repaymentMethod` from product
- `services/lending-service/src/main/java/com/ksa/financing/lending/application/usecase/CalculateFinanceUseCaseImpl.java`, `CheckEligibilityUseCaseImpl.java`, `SuggestProductsUseCaseImpl.java`, `adapter/rest/response/PreQualificationData.java`
  - Same plumbing — read from product
- `services/lending-service/src/main/resources/db/migration/V{n}__add_repayment_method_to_loans.sql`
  - Store the method on the loan record so reschedule/settlement use same math
- Domain event `LoanCreated` payload
  - Add `repaymentMethod` field (Kafka contract change — coordinate with collections)

### product-service
- Add `repayment_method` column to product table (defaults to `FLAT` for existing, `REDUCING_BALANCE` for new Murabaha/Ijara products as configured)

### collections-service
- `services/collections-service/src/main/resources/db/migration/V13__add_balance_columns_to_installments.sql`
  - `ALTER TABLE installments ADD COLUMN opening_balance NUMERIC(20,6)`
  - `ALTER TABLE installments ADD COLUMN closing_balance NUMERIC(20,6)`
  - `ALTER TABLE installments ADD COLUMN monthly_rate NUMERIC(10,8)`
- `services/collections-service/src/main/java/com/ksa/financing/collections/infrastructure/persistence/entity/InstallmentJpaEntity.java`
  - Map new columns
- `services/collections-service/src/main/java/com/ksa/financing/collections/infrastructure/messaging/LoanEventListener.java`
  - Read `repaymentMethod` from event
  - Branch: `generateReducingBalanceSchedule(...)` vs current `generateFlatSchedule(...)`
  - Persist opening/closing/monthlyRate
- `ManageRepaymentScheduleUseCase.CreateScheduleCommand.InstallmentEntry`
  - Add `openingBalance`, `closingBalance`, `monthlyRate`

### ledger-service / Fineract
- `services/ledger-service/src/main/java/.../FineractGateway.java`
  - Ensure loan product creation uses `interestType=DECLINING_BALANCE`, `amortizationType=EQUAL_INSTALLMENTS`
- `RunReconciliationUseCaseImpl`
  - Comparison tolerance ≤ 0.01 SAR per installment (rounding noise)

### Tests
- SDK: golden tests using doc §7 example table (months 1–4)
- Lending: assert offer EMI equals schedule EMI for tenures {6, 12, 24, 36, 60}
- Collections: assert sum(principal) = P, sum(profit) = total profit, `closing[n] = 0`
- Integration: end-to-end loan origination → schedule generated → Fineract account matches

### Docs / OpenAPI / Postman
- `docs/postman/KSA-Islamic-Financing-Platform.postman_collection.json` — refresh example responses for `/calculate-finance`, `/loans/{id}/schedule`
- `infrastructure/swagger/openapi.yml` — update example payloads, add `repaymentMethod` enum
- ADR — write `docs/standards/adr/ADR-XXX-reducing-balance-repayment.md` capturing the decision

---

## 7. Database Migration Notes

- All new columns initially `NULLABLE`
- Backfill job (one-off SQL or Spring `ApplicationRunner`): for legacy FLAT loans, set `opening_balance = principal_amount`, `closing_balance = 0` per installment (best-effort placeholder — they aren't true reducing-balance loans)
- Flip to `NOT NULL` only after backfill verified
- No rewrite of existing schedules — preserves Sharia contract integrity

---

## 8. Sharia / Compliance Checklist

- [ ] AAOIFI FAS 2 compliance review for Murabaha with reducing-balance EMI
- [ ] Confirm: EMI fixed throughout, profit pre-disclosed at contract → still Sharia-valid
- [ ] No compounding (profit-on-profit) — verified by formula: profit = openingBalance × monthlyRate (not previous-profit-included balance)
- [ ] Late payment must NOT increase total profit — penalty goes to charity (`CharityPenaltyCalculator` already handles this; verify still wired)
- [ ] Early settlement: customer pays `closing_balance` only, no future profit (per doc §16) — implement Ibra rebate path if product offers it
- [ ] Sharia board sign-off captured as artifact

---

## 9. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Existing in-flight loans break | Low | High | Use per-product flag; never mutate existing loan schedules |
| Fineract drift (rounding) | Medium | Medium | Tolerance ≤ 0.01 SAR; reconciliation alerts |
| Sharia board rejects reducing balance | Low | High | Pre-clear in Phase 0; fallback = keep flat with bug fix only |
| Customer-facing EMI changes for products mid-launch | Medium | High | Freeze offer + schedule deploy together |
| Kafka contract break (`repaymentMethod` field) | Medium | Medium | Field is optional, defaults to `FLAT` on consumer |
| Backfill of legacy installments wrong | Low | Low | Backfill is cosmetic only; not used for collections math |

---

## 10. Acceptance Criteria

1. For a sample loan `P=100,000, r=12%, n=12`, offer response and persisted schedule both show EMI = `8,884.88 ± 0.02 SAR`
2. Sum of principal portions across all installments = exact principal (no rounding leakage)
3. Final installment `closing_balance = 0.00` exactly
4. Profit per installment decreases monotonically, principal per installment increases monotonically
5. `repaymentMethod` propagates: product → loan → event → schedule (audit log shows same value in all three)
6. Reconciliation job vs Fineract: 0 discrepancies on a 100-loan sample
7. ArchUnit + `/review-arch` pass
8. `/compliance-check lending-service` and `/compliance-check collections-service` pass
9. Postman regression collection green on all loan flows

---

## 11. Open Questions

- [ ] Product team: does every Murabaha product migrate to reducing balance, or stays flat?
- [ ] Sharia board: explicit fatwa on declining-balance EMI for Murabaha (some boards prefer flat)
- [ ] Finance ops: any reporting (P&L, accruals) that assumes flat profit distribution?
- [ ] Existing Fineract loans — currently configured as flat or declining? (verify)
- [ ] Tax/ZATCA: does VAT timing change if profit is recognised on declining-balance schedule? (consult tax)

---

## 12. Out of Scope

- Penalty / late-fee calculation (lives in `CharityPenaltyCalculator`, untouched)
- Bullet / balloon repayment structures
- Variable rate products
- Ijara-MBT specific residual-value handling
- Conversion of historical FLAT loans to REDUCING (cosmetic backfill only)

---

## 13. References

- Spec: [reducing-balance-load-documentation.md](reducing-balance-load-documentation.md)
- Current flat engine: [FinanceCalculationService.java](../services/lending-service/src/main/java/com/ksa/financing/lending/domain/service/FinanceCalculationService.java)
- Schedule generator: [AmortizationScheduleGenerator.java](../shared-libraries/domain-core-sdk/src/main/java/com/ksa/financing/domain/sharia/AmortizationScheduleGenerator.java)
- Consumer: [LoanEventListener.java](../services/collections-service/src/main/java/com/ksa/financing/collections/infrastructure/messaging/LoanEventListener.java)
- Standards: [docs/standards/ENVIRONMENT_CONFIG.md](standards/ENVIRONMENT_CONFIG.md), [docs/standards/NAMING_CONVENTIONS.md](standards/NAMING_CONVENTIONS.md)
