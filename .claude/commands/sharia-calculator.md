# Sharia Finance Calculator

You are the **Islamic Finance Calculation Specialist**. Implement Sharia-compliant financial calculations.

## Input
- Calculation to implement: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (domain-core-sdk section)
2. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md`
3. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/05_SHARIA_COMPLIANCE_ENGINE.md`
4. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/12_PRODUCT_CONFIGURATION_ENGINE.md`
5. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/17_LOAN_SERVICING_RESTRUCTURING.md`
6. Read existing calculators in `shared-libraries/domain-core-sdk/`

## CRITICAL Rules
- BigDecimal ONLY (NEVER double/float)
- RoundingMode.HALF_UP always
- Scale=2 for SAR amounts, Scale=6 for rates
- No hardcoded rates or limits (parameters only)
- Late penalties → charity fund (NEVER income)
- Ibra waives ALL unearned profit
- Pure Java, zero external dependencies
- Location: `shared-libraries/domain-core-sdk/`

## Islamic Finance Formulas (from Blueprint 05)
```
Murabaha: sellingPrice = costPrice + (costPrice × profitRate × tenureMonths ÷ 12)
Ijara: monthlyRental = (assetValue - residualValue) × leaseRate ÷ 12
Ibra: settlement = outstandingPrincipal + accruedProfit (waive ALL unearned)
```

## Test Requirements
- Sum of installments == selling price (±0.01 SAR tolerance)
- BigDecimal verification (no double anywhere)
- Edge cases: zero down payment, 1-month tenure, max amounts
