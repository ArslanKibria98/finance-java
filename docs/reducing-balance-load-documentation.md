# Reducing Balance Loan Repayment Engine Documentation

## Overview

This document explains the architecture, business logic, accounting flow, and repayment calculations for a production-grade reducing balance (amortized) loan repayment engine.

The purpose of this repayment model is to:

* Calculate loan installments using reducing balance methodology
* Generate amortized repayment schedules
* Split each installment into:

  * Profit/Interest Portion
  * Principal Portion
* Reduce outstanding principal after every installment
* Support banking and fintech repayment workflows
* Produce accurate repayment schedules similar to real banking systems

---

# 1. Loan Repayment Models

## 1.1 Flat Rate Model

In a flat-rate model:

* Profit/interest is calculated on the original loan amount for the entire tenure.
* Installments are usually calculated as:

```text
Total Payable / Total Months
```

### Characteristics

* Same installment amount
* Same profit distribution
* Same principal distribution
* Profit does not decrease over time
* Not suitable for real banking repayment engines

### Problem with Flat Model

The borrower continues paying profit on the full original principal even after partial repayment.

This is not how most modern banks or fintech systems operate.

---

## 1.2 Reducing Balance Model

In reducing balance loans:

* Profit/interest is calculated on the remaining outstanding principal.
* Outstanding balance decreases after every installment.
* Profit decreases gradually.
* Principal repayment increases gradually.

### Characteristics

* EMI may remain fixed
* Profit portion changes monthly
* Principal portion changes monthly
* Realistic banking behavior
* Industry-standard repayment mechanism

---

# 2. Core Business Logic

## Banking Repayment Flow

### Step 1 — Loan Disbursement

Customer receives loan amount.

Example:

| Item        | Amount  |
| ----------- | ------- |
| Loan Amount | 100,000 |

---

### Step 2 — Monthly Profit Calculation

Every month:

* Bank calculates profit on remaining principal.
* Since principal keeps reducing, profit also reduces.

Formula:

```text
Monthly Profit = Outstanding Principal × Monthly Profit Rate
```

---

### Step 3 — Installment Allocation

Every EMI/installment contains:

| Component         | Description           |
| ----------------- | --------------------- |
| Profit Portion    | Bank earnings         |
| Principal Portion | Actual loan repayment |

Formula:

```text
Principal Portion = EMI - Profit Portion
```

---

### Step 4 — Outstanding Reduction

After principal repayment:

```text
New Outstanding = Previous Outstanding - Principal Portion
```

---

### Step 5 — Repeat Until Loan Closure

The process repeats monthly until:

```text
Outstanding Principal = 0
```

---

# 3. EMI (Equal Monthly Installment) Formula

## Monthly Rate Formula

```text
r = AnnualRate / (12 × 100)
```

Where:

| Variable   | Meaning                      |
| ---------- | ---------------------------- |
| r          | Monthly profit/interest rate |
| AnnualRate | Yearly profit percentage     |

---

## EMI Formula

```text
EMI = P × [ r(1+r)^n ] / [ (1+r)^n - 1 ]
```

Where:

| Variable | Meaning             |
| -------- | ------------------- |
| P        | Loan Amount         |
| r        | Monthly Profit Rate |
| n        | Total Months        |

---

# 4. Why EMI Stays Fixed

In reducing balance loans:

* EMI amount often remains fixed.
* Internal composition changes monthly.

Meaning:

| Month        | Profit | Principal |
| ------------ | ------ | --------- |
| Early Months | High   | Low       |
| Later Months | Low    | High      |

This creates smooth repayment behavior.

---

# 5. Monthly Amortization Logic

## Step 1 — Calculate Profit Portion

```text
Profit Portion = Outstanding Principal × Monthly Rate
```

---

## Step 2 — Calculate Principal Portion

```text
Principal Portion = EMI - Profit Portion
```

---

## Step 3 — Reduce Outstanding Principal

```text
Outstanding Principal = Outstanding Principal - Principal Portion
```

---

## Step 4 — Continue Monthly Loop

Repeat until tenure completes.

---

# 6. Real-World Example

## Loan Information

| Item               | Value     |
| ------------------ | --------- |
| Loan Amount        | 100,000   |
| Annual Profit Rate | 12%       |
| Tenure             | 12 Months |

---

## Monthly Rate

```text
12 / (12 × 100)
= 0.01
= 1%
```

---

## EMI Calculation

Using EMI formula:

```text
EMI ≈ 8,884
```

---

# 7. Amortization Schedule Example

| Month | Opening Balance | Profit | Principal | EMI   | Closing Balance |
| ----- | --------------- | ------ | --------- | ----- | --------------- |
| 1     | 100,000         | 1,000  | 7,884     | 8,884 | 92,116          |
| 2     | 92,116          | 921    | 7,963     | 8,884 | 84,153          |
| 3     | 84,153          | 842    | 8,042     | 8,884 | 76,111          |
| 4     | 76,111          | 761    | 8,123     | 8,884 | 67,988          |

---

# 8. Why Profit Decreases Over Time

Because:

```text
Profit = Outstanding Principal × Rate
```

Outstanding principal reduces monthly.

Therefore:

* Profit automatically reduces.
* Principal repayment increases.

This creates realistic repayment behavior.

---

# 9. Why Flat Installment Formula Is Incorrect

Incorrect Formula:

```text
equalInstallment = totalPayable / months
```

Problems:

| Issue                         | Explanation                        |
| ----------------------------- | ---------------------------------- |
| No reducing balance           | Outstanding never affects profit   |
| Static profit                 | Profit remains effectively fixed   |
| No amortization               | Principal/profit split not dynamic |
| Unrealistic banking behavior  | Does not mimic actual bank systems |
| Incorrect repayment structure | Customer pays profit uniformly     |

---

# 10. Correct Banking Behavior

Real banking systems:

* Track outstanding balance
* Calculate monthly profit dynamically
* Reduce principal monthly
* Maintain amortized schedules
* Produce audit-ready repayment records

---

# 11. Accounting Perspective

## Bank Perspective

### Assets

Loan outstanding is treated as:

```text
Loan Receivable
```

---

### Income

Monthly profit becomes:

```text
Profit/Interest Income
```

---

### Principal Recovery

Principal repayment reduces:

```text
Loan Receivable
```

---

## Customer Perspective

Customer pays:

* Fixed EMI
* Gradually reducing profit
* Increasing principal contribution

---

# 12. VAT Handling

If VAT applies:

VAT is usually charged on:

```text
Profit/Service Charges
```

Not on:

```text
Principal Amount
```

---

## VAT Formula

```text
VAT = Profit × VAT Percentage
```

---

# 13. Fees Handling

## Common Fees

| Fee            | Purpose                   |
| -------------- | ------------------------- |
| Admin Fee      | Administrative processing |
| Processing Fee | Loan processing charges   |
| Service Fee    | Platform/service cost     |

---

## Fee Inclusion Logic

### Inclusive Disbursement

Fees are deducted before customer receives money.

Example:

| Item                     | Amount  |
| ------------------------ | ------- |
| Approved Loan            | 100,000 |
| Fees                     | 5,000   |
| Amount Given to Customer | 95,000  |

---

### Non-Inclusive Disbursement

Customer receives full amount.

Fees are added separately to repayment schedule.

---

# 14. System Design Recommendations

## Recommended Components

| Component              | Purpose                     |
| ---------------------- | --------------------------- |
| Loan Engine            | Core repayment calculations |
| Amortization Generator | Schedule generation         |
| Installment Service    | EMI management              |
| Accounting Module      | Ledger postings             |
| VAT Engine             | Tax calculations            |
| Fee Engine             | Service fee management      |

---

# 15. Recommended Database Fields

## Loan Table

| Field                | Description         |
| -------------------- | ------------------- |
| LoanAmount           | Original loan       |
| OutstandingPrincipal | Remaining balance   |
| AnnualRate           | Profit rate         |
| TenureMonths         | Loan duration       |
| EMI                  | Monthly installment |
| Status               | Active/Closed       |

---

## Installment Table

| Field           | Description          |
| --------------- | -------------------- |
| InstallmentNo   | Installment sequence |
| OpeningBalance  | Previous outstanding |
| ProfitAmount    | Monthly profit       |
| PrincipalAmount | Principal repayment  |
| EMI             | Monthly payment      |
| ClosingBalance  | Remaining balance    |
| DueDate         | Installment due date |

---

# 16. Edge Cases

## Zero Profit Rate

If:

```text
AnnualRate = 0
```

Then:

* EMI becomes principal-only repayment.
* No profit calculation required.

---

## Early Settlement

If customer closes loan early:

* Future profit should not be charged.
* Outstanding principal should be settled.

---

## Late Payment

Possible additions:

* Penalty charges
* Additional profit
* Grace period logic

---

# 17. Production Considerations

## Precision Handling

Use decimal types.

Avoid float/double for financial calculations.

---

## Rounding Strategy

Define:

* installment rounding
* final installment adjustment
* currency precision

---

## Auditability

Every installment should maintain:

* opening balance
* profit calculation
* principal calculation
* closing balance

for regulatory compliance.

---

# 18. Final Summary

A reducing balance loan engine:

* Calculates profit on remaining outstanding balance
* Produces amortized repayment schedules
* Dynamically adjusts profit and principal portions
* Mimics real banking systems
* Provides accurate financial behavior
* Supports production-grade fintech operations

Compared to flat-rate logic:

```text
equalInstallment = totalPayable / months
```

reducing balance methodology provides:

* realistic repayments
* fair customer charging
* accurate accounting
* compliant banking behavior
* scalable loan processing architecture
