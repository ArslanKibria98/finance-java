# Chart of Accounts (CoA) - GL Accounts Sample Entries

## Standard GL Accounts for Islamic Financing Platform

| Account Code | Account Name (English) | Account Name (اردو) | Account Type | Parent Account | Is Header | Status |
|---|---|---|---|---|---|---|
| **1000** | **ASSETS** | **اثاثیات** | Header | NULL | Yes | Active |
| 1100 | Current Assets | موجودہ اثاثیات | Header | 1000 | Yes | Active |
| 1110 | Cash & Bank Balances | نقد اور بینک میں رقم | Asset | 1100 | No | Active |
| 1111 | Bank Account - SAR | بینک اکاؤنٹ - ریال | Asset | 1110 | No | Active |
| 1120 | Investment Securities | سرمایہ کاری کے اوراق | Asset | 1100 | No | Active |
| 1200 | Non-Current Assets | غیر موجودہ اثاثیات | Header | 1000 | Yes | Active |
| 1210 | Property, Plant & Equipment | سازوسامان اور عمارت | Asset | 1200 | No | Active |
| 1220 | Intangible Assets | غیر مرئی اثاثیات | Asset | 1200 | No | Active |
| **2000** | **LIABILITIES** | **ذمہ داریاں** | Header | NULL | Yes | Active |
| 2100 | Current Liabilities | موجودہ ذمہ داریاں | Header | 2000 | Yes | Active |
| 2110 | Customer Deposits | گاہک کی جمع رقم | Liability | 2100 | No | Active |
| 2120 | Accounts Payable | واجب الادا رقم | Liability | 2100 | No | Active |
| 2200 | Non-Current Liabilities | غیر موجودہ ذمہ داریاں | Header | 2000 | Yes | Active |
| 2210 | Long-term Financing | طویل مدتی فنڈنگ | Liability | 2200 | No | Active |
| **3000** | **EQUITY** | **سرمایہ** | Header | NULL | Yes | Active |
| 3100 | Share Capital | حصص کی رقم | Equity | 3000 | No | Active |
| 3200 | Retained Earnings | برقرار کمائی | Equity | 3000 | No | Active |
| 3300 | Zakat Fund | زکوۃ فنڈ | Equity | 3000 | No | Active |
| **4000** | **INCOME** | **آمدنی** | Header | NULL | Yes | Active |
| 4100 | Murabaha Income | مرابحہ کی آمدنی | Income | 4000 | No | Active |
| 4110 | Murabaha Profit | مرابحہ منافع | Income | 4100 | No | Active |
| 4200 | Ijara Income | اجارہ کی آمدنی | Income | 4000 | No | Active |
| 4300 | Service Fees | خدمات کی فیس | Income | 4000 | No | Active |
| 4400 | Interest/Riba (Non-Compliant) | سود (غیر شرعی) | Income | 4000 | No | Active |
| **5000** | **EXPENSES** | **اخراجات** | Header | NULL | Yes | Active |
| 5100 | Salaries & Benefits | تنخواہیں اور فوائد | Expense | 5000 | No | Active |
| 5200 | Administrative Expenses | انتظامی اخراجات | Expense | 5000 | No | Active |
| 5300 | Provision for Bad Debts | مشکوک ڈیبٹ کے لیے ذخیرہ | Expense | 5000 | No | Active |
| 5400 | Depreciation | قیمت میں کمی | Expense | 5000 | No | Active |

## Account Type Reference

| Type | Description |
|---|---|
| **Header** | Parent/summary account for grouping (non-posting) |
| **Asset** | Resources owned (cash, securities, property) |
| **Liability** | Obligations owed (deposits, loans, payables) |
| **Equity** | Owner's stake (capital, retained earnings, funds) |
| **Income** | Revenue sources (Murabaha, Ijara, fees) |
| **Expense** | Costs (salaries, administration, provisions) |

## Notes
- **Zakat Fund**: Separate charity account per Islamic financing rules
- **Murabaha Income**: Cost-plus-profit financing revenue
- **Ijara Income**: Islamic leasing revenue  
- **Riba (Interest)**: Non-compliant account for tracking—marked for compliance monitoring
- Parent hierarchy supports multi-level GL structure
- All accounts are tenant-isolated (via ledger-service multi-tenancy)

## How to Create These Accounts via API

**POST** `/api/v1/accounts`

```json
{
  "accountCode": "1000",
  "accountName": "ASSETS",
  "accountNameAr": "اثاثیات",
  "accountType": "HEADER",
  "parentAccountCode": null,
  "isHeader": true
}
```

**Create child account under parent:**

```json
{
  "accountCode": "1110",
  "accountName": "Cash & Bank Balances",
  "accountNameAr": "نقد اور بینک میں رقم",
  "accountType": "ASSET",
  "parentAccountCode": "1100",
  "isHeader": false
}
```

---

**Created**: 2026-04-17  
**Service**: ledger-service  
**Endpoint Base**: `/api/v1/accounts`  
**Authentication**: Bearer JWT token (tenant_id required in claims)
