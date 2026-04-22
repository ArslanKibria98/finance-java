# FINERACT REPORTS - What Gets Generated & How to Display

> **Purpose**: Identify all reports that Fineract generates from GL transactions and how to display them in dashboard
> **Data Source**: Fineract GL journal entries, accounts, loans
> **Alignment**: GL transaction flow → Reporting → Dashboard

---

## 📊 **REPORTS GENERATED FROM FINERACT (As Per Flow)**

### **Based on Current GL Entries Being Posted:**

```
GL ENTRIES POSTED TO FINERACT:
├── Disbursement Entries (Dr. Loans Receivable, Cr. Bank)
├── Repayment Entries (Dr. Bank, Cr. Loans Receivable, Cr. Profit)
├── Early Settlement Entries (Dr. Bank, Cr. LR, Cr. Profit, + Waiver)
└── Restructuring Write-off Entries (Dr. Provision, Cr. LR, + Profit Waiver)

FROM THESE GL ENTRIES, FINERACT CAN GENERATE:
```

---

## 🎯 **REPORTS THAT CAN BE GENERATED**

### **1️⃣ TRIAL BALANCE REPORT**

```yaml
Name: Trial Balance
Frequency: Daily / Monthly / Ad-hoc
Source: GL accounts table in Fineract

What it shows:
  - All GL accounts with balances
  - Total debits = Total credits (should balance)
  - Opening balance, transactions, closing balance
  
Example Output:
  Account #    Account Name              Debit           Credit
  1010         Bank Account              1,200,000.00    500,000.00
  1200         Loans Receivable          3,000,000.00    4,500,000.00
  1300         Profit Receivable         500,000.00      200,000.00
  2200         Unearned Profit           150,000.00      0.00
  
  TOTAL:       4,850,000.00              4,850,000.00  ✓ BALANCED

Use Case: Daily end-of-day reconciliation
Dashboard: Show as "GL Trial Balance" card
GET API: /api/v1/reports/trial-balance?date=2026-03-30
```

---

### **2️⃣ LOAN PORTFOLIO SUMMARY**

```yaml
Name: Loan Portfolio Report
Frequency: Daily / Monthly
Source: Loans table linked to GL Loans Receivable account

What it shows:
  - Total loans disbursed
  - Outstanding principal balance
  - Collections received
  - Default/delinquent loans
  - Portfolio quality (DPD buckets)

Example Output:
  Total Loans:                    150
  Total Disbursed:                15,000,000.00 SAR
  Outstanding Principal:          14,500,000.00 SAR
  Collections (This Month):       500,000.00 SAR
  
  Active Loans:                   145
  Delinquent (DPD 1-30):         3
  Delinquent (DPD 31-60):        1
  Defaulted (DPD > 90):          1
  
  Portfolio Health:               97.33% performing

Use Case: Portfolio health monitoring
Dashboard: Multiple cards (Total Disbursed, Outstanding, Delinquent)
GET API: /api/v1/reports/portfolio-summary?from=2026-01-01&to=2026-03-30
```

---

### **3️⃣ DPD BUCKET REPORT (Delinquency Analysis)**

```yaml
Name: Days Past Due (DPD) Bucket Report
Frequency: Daily
Source: Loans + GL Receivable aging analysis

What it shows:
  - Count of loans by DPD category
  - Amount outstanding by DPD
  - Trend over time

Example Output:
  Current (DPD 0):               140 loans   | 14,000,000 SAR
  Grace Period (DPD 1-30):       7 loans     | 500,000 SAR
  Mild Delinquent (DPD 31-60):   2 loans     | 250,000 SAR
  High Delinquent (DPD 61-90):   1 loan      | 150,000 SAR
  Defaulted (DPD > 90):          0 loans     | 0 SAR
  
  Total:                         150 loans   | 14,900,000 SAR
  
  Trend:
    Yesterday: 2 in grace, 6 in mild, 1 in high
    Today:     7 in grace, 2 in mild, 1 in high
    Movement:  +5 entered grace period, -4 moved out

Use Case: Collections management, early intervention
Dashboard: DPD buckets chart + table
GET API: /api/v1/reports/dpd-buckets?date=2026-03-30
```

---

### **4️⃣ COLLECTIONS & REPAYMENT REPORT**

```yaml
Name: Collections & Repayment Report
Frequency: Daily / Weekly / Monthly
Source: GL Bank Account (Dr. entries) + GL Loan Receivable (Cr. entries)

What it shows:
  - Total payments received today/week/month
  - Average payment size
  - Payment methods breakdown
  - On-time vs late payments
  - Collection rate

Example Output:
  Period: March 2026
  
  Total Collections:              2,500,000.00 SAR
  Number of Payments:             500
  Average Payment Size:           5,000.00 SAR
  
  By Payment Method:
    SADAD:                        1,500,000.00 (60%)
    Bank Transfer:                800,000.00 (32%)
    Card:                         200,000.00 (8%)
  
  Collection Performance:
    Expected Collections:         2,800,000.00
    Actual Collections:           2,500,000.00
    Collection Rate:              89.3%
  
  On-Time vs Late:
    On-Time Payments:             450 (90%)
    Late Payments:                50 (10%)

Use Case: Revenue monitoring, payment analysis
Dashboard: Collections card + method breakdown chart
GET API: /api/v1/reports/collections?from=2026-03-01&to=2026-03-31
```

---

### **5️⃣ PROFIT & REVENUE REPORT**

```yaml
Name: Profit/Interest Income Report
Frequency: Monthly
Source: GL Profit Receivable account + GL Unearned Profit account

What it shows:
  - Profit earned (accrued)
  - Profit collected (received)
  - Unearned profit (deferred)
  - Profit by product type

Example Output:
  Murabaha Portfolio:
    Total Profit Scheduled:       1,200,000.00 SAR
    Profit Accrued to Date:       600,000.00 SAR (50%)
    Profit Collected:             400,000.00 SAR
    Profit Receivable:            200,000.00 SAR
    Unearned Profit:              600,000.00 SAR
  
  Ijara Portfolio:
    Total Profit Scheduled:       400,000.00 SAR
    Profit Accrued to Date:       150,000.00 SAR (37.5%)
    Profit Collected:             100,000.00 SAR
    Profit Receivable:            50,000.00 SAR
    Unearned Profit:              250,000.00 SAR
  
  Combined Metrics:
    Total Monthly Profit Income:  750,000.00 SAR
    Profit Realization Rate:      66.7%

Use Case: Revenue forecasting, Islamic finance metrics
Dashboard: Profit revenue card + trend chart
GET API: /api/v1/reports/profit-revenue?month=2026-03
```

---

### **6️⃣ WRITE-OFF & PROVISIONS REPORT**

```yaml
Name: Write-off & Bad Debt Provisions Report
Frequency: Monthly
Source: GL Provision account + GL Loans Receivable write-offs

What it shows:
  - Amount provided for bad debts
  - Amount written off
  - Loans restructured with write-off
  - Coverage ratio

Example Output:
  Bad Debt Provisions:
    Opening Balance:              500,000.00 SAR
    New Provisions (This Month):  50,000.00 SAR
    Write-offs Applied:           (30,000.00) SAR
    Closing Balance:              520,000.00 SAR
  
  Coverage Analysis:
    Defaulted Loans (>90 DPD):    100,000.00 SAR
    Provision Made:               520,000.00 SAR
    Coverage Ratio:               520% (over-provided)
  
  Restructured Loans (With Write-off):
    Count:                        8 loans
    Total Write-off Amount:       200,000.00 SAR
    Profit Waivers:               50,000.00 SAR
    
  Reconciliation:
    GL Provision Account:         520,000.00 SAR ✓

Use Case: Risk management, regulatory reporting
Dashboard: Provisions card + coverage ratio
GET API: /api/v1/reports/write-offs-provisions?month=2026-03
```

---

### **7️⃣ CASH FLOW REPORT**

```yaml
Name: Cash Flow / Bank Account Report
Frequency: Daily
Source: GL Bank Account (account 1010)

What it shows:
  - Daily cash inflows (collections)
  - Daily cash outflows (disbursements)
  - Net cash position
  - Available liquidity

Example Output:
  Date: 2026-03-30
  
  Opening Balance (Start of Day):  1,000,000.00 SAR
  
  Inflows:
    Loan Repayments:              500,000.00 SAR
    Settlement Payments:          50,000.00 SAR
    Other:                        20,000.00 SAR
    Total Inflows:                570,000.00 SAR
  
  Outflows:
    Loan Disbursements:           (300,000.00) SAR
    Refunds/Reversals:            (10,000.00) SAR
    Total Outflows:               (310,000.00) SAR
  
  Closing Balance (End of Day):    1,260,000.00 SAR
  
  Available Liquidity:            1,260,000.00 SAR
  Recommended Reserve:            500,000.00 SAR
  Buffer Above Reserve:           760,000.00 SAR ✓

Use Case: Liquidity management, cash forecasting
Dashboard: Cash position card + daily inflow/outflow
GET API: /api/v1/reports/cash-flow?date=2026-03-30
```

---

### **8️⃣ INVESTOR/PORTFOLIO REPORT**

```yaml
Name: Investor Portfolio & ROI Report
Frequency: Monthly
Source: GL accounts + loan disbursement allocation

What it shows:
  - Assets Under Management (AUM)
  - Investor share allocation
  - Profit earned per investor
  - ROI calculation
  - Default impact

Example Output:
  Investor: Pension Fund A (40% allocation)
  
  Allocated Amount:               6,000,000.00 SAR
  Current Outstanding:            5,800,000.00 SAR
  
  Profit Earned (This Month):     150,000.00 SAR
  Profit Earned (YTD):            600,000.00 SAR
  
  Collections Received:           200,000.00 SAR
  Defaults Written-off:           0.00 SAR
  
  Net Yield (YTD):                10.0%
  Annualized ROI:                 12.5%
  
  Performance Metrics:
    Portfolio Status:             97% performing
    Default Rate:                 0%
    Collection Efficiency:        96%

Use Case: Investor reporting, partnership management
Dashboard: Investor portfolio card + ROI chart
GET API: /api/v1/reports/investor-portfolio?investor=INV001&month=2026-03
```

---

### **9️⃣ RECONCILIATION REPORT**

```yaml
Name: Fineract ↔ Our System Reconciliation
Frequency: Daily
Source: GL entries + our transaction logs

What it shows:
  - GL entries posted to Fineract
  - Our records vs Fineract records
  - Discrepancies (if any)
  - Pending sync items

Example Output:
  Report Date: 2026-03-30
  
  GL Entries Status:
    Total Created in Our System:  100
    Successfully Posted:          95
    Pending Posting:              3
    Failed (Retry):               2
  
  Account Reconciliation:
    Account 1010 (Bank):
      Our GL Balance:             1,260,000.00 SAR
      Fineract GL Balance:        1,260,000.00 SAR
      Match:                      ✓ YES
  
    Account 1200 (Loans Receivable):
      Our GL Balance:             14,500,000.00 SAR
      Fineract GL Balance:        14,500,000.00 SAR
      Match:                      ✓ YES
  
    Account 1300 (Profit Receivable):
      Our GL Balance:             200,000.00 SAR
      Fineract GL Balance:        200,000.00 SAR
      Match:                      ✓ YES
  
  Overall Status:                 ✓ RECONCILED
  
  Pending Items:
    Entry ID: uuid-123
    Reference: DISB-2026-00050
    Status: SUBMITTED (awaiting Fineract response)
    Since: 2 hours ago
  
  Failed Items (Auto-Retry):
    Entry ID: uuid-456
    Reference: PAY-2026-00051
    Failure: Connection timeout
    Next Retry: 2026-03-30 20:00:00

Use Case: Data integrity, daily validation
Dashboard: Reconciliation status card + pending items
GET API: /api/v1/reports/reconciliation?date=2026-03-30
```

---

### **🔟 SAMA REGULATORY REPORT (Monthly)**

```yaml
Name: SAMA Compliance & Reporting
Frequency: Monthly
Source: All GL data + loan portfolio data

What it shows:
  - Capital adequacy ratio
  - Loan delinquency metrics
  - Profit distribution compliance
  - Risk concentration
  - Sharia compliance status

Example Output:
  Month: March 2026
  
  Regulatory Metrics:
    Total Assets:                 20,000,000.00 SAR
    Total Loans:                  15,000,000.00 SAR (75%)
    Reserves & Provisions:        520,000.00 SAR
    
  Delinquency:
    Portfolio Delinquency Rate:   2.67% (4 of 150 loans)
    Target (SAMA):                < 5%
    Status:                       ✓ COMPLIANT
  
  Capital Metrics:
    Minimum Capital Requirement:  2,000,000.00 SAR
    Current Capital:              3,000,000.00 SAR
    Excess Capital:               1,000,000.00 SAR
    Status:                       ✓ ABOVE MINIMUM
  
  Profit Distribution:
    Profit Earned (This Month):   750,000.00 SAR
    Zakat (2.5%):                 18,750.00 SAR
    Retained Profit:              731,250.00 SAR
    Status:                       ✓ SHARIA COMPLIANT
  
  Sector Concentration:
    Real Estate:                  40% (LIMIT: 50%)
    Trade:                        30% (LIMIT: 40%)
    SME:                          30% (LIMIT: 40%)
    Status:                        ✓ WITHIN LIMITS
  
  Overall Rating:                 ✓ COMPLIANT (A Rating)

Use Case: Regulatory submission, compliance monitoring
Dashboard: SAMA metrics card + compliance badges
Endpoint: /api/v1/reports/sama-regulatory?month=2026-03
```

---

## 📍 **REPORT MAPPING TO DASHBOARD**

```yaml
DASHBOARD CARDS:

┌─────────────────────────────────────────┐
│ GL OPERATIONS SECTION                   │
├─────────────────────────────────────────┤
│ Card 1: Trial Balance Status             │ → Trial Balance Report
│ Card 2: GL Entry Queue                   │ → GL Transaction Status
│ Card 3: Reconciliation Status            │ → Reconciliation Report
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ PORTFOLIO HEALTH SECTION                 │
├─────────────────────────────────────────┤
│ Card 1: Loan Portfolio Summary           │ → Portfolio Report
│ Card 2: DPD Buckets (Delinquency)        │ → DPD Bucket Report
│ Card 3: Collections YTD                  │ → Collections Report
│ Card 4: Provisions & Write-offs          │ → Write-off Report
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ REVENUE & PROFITABILITY SECTION          │
├─────────────────────────────────────────┤
│ Card 1: Profit Earned (This Month)       │ → Profit & Revenue Report
│ Card 2: Cash Position                    │ → Cash Flow Report
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ INVESTOR MANAGEMENT SECTION              │
├─────────────────────────────────────────┤
│ Card 1: Portfolio AUM                    │ → Investor Portfolio Report
│ Card 2: Investor ROI                     │ → Investor Portfolio Report
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ REGULATORY COMPLIANCE SECTION            │
├─────────────────────────────────────────┤
│ Card 1: SAMA Compliance Status           │ → SAMA Regulatory Report
│ Card 2: Capital Adequacy Ratio           │ → SAMA Regulatory Report
│ Card 3: Sharia Compliance Badge          │ → SAMA Regulatory Report
└─────────────────────────────────────────┘
```

---

## 🔗 **REPORT GET APIs TO IMPLEMENT**

```
1. GET /api/v1/reports/trial-balance
   Query: ?date=2026-03-30&format=json/csv
   
2. GET /api/v1/reports/portfolio-summary
   Query: ?from=2026-01-01&to=2026-03-30
   
3. GET /api/v1/reports/dpd-buckets
   Query: ?date=2026-03-30&trend=true
   
4. GET /api/v1/reports/collections
   Query: ?from=2026-01-01&to=2026-03-31&groupBy=method
   
5. GET /api/v1/reports/profit-revenue
   Query: ?month=2026-03&byProduct=true
   
6. GET /api/v1/reports/write-offs-provisions
   Query: ?month=2026-03
   
7. GET /api/v1/reports/cash-flow
   Query: ?date=2026-03-30&trend=true
   
8. GET /api/v1/reports/investor-portfolio
   Query: ?investor=INV001&month=2026-03
   
9. GET /api/v1/reports/reconciliation
   Query: ?date=2026-03-30
   
10. GET /api/v1/reports/sama-regulatory
    Query: ?month=2026-03
```

---

## 🏗️ **IMPLEMENTATION PATH**

```
Phase 1: Core Reports (Essential for daily ops)
├─ Trial Balance Report
├─ GL Entry Queue Status
├─ DPD Bucket Report
├─ Collections Report
└─ Cash Flow Report

Phase 2: Analysis Reports (For decision-making)
├─ Portfolio Summary
├─ Profit & Revenue Report
├─ Write-off & Provisions Report
└─ Reconciliation Report

Phase 3: External Reporting (Investor + Regulatory)
├─ Investor Portfolio Report
└─ SAMA Regulatory Report
```

---

**These 10 reports can all be generated from the GL entries being posted to Fineract! 🎯**

Kaunsa report pehlay implement karte hain? 👇
