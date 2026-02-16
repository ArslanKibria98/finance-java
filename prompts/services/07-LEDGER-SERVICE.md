# 📒 Prompt 07: Ledger Service

**Objective**: Implement the Ledger Service for double-entry bookkeeping and financial accounting.

**Prerequisites**: ✅ All SDK prompts (01-08) + Service template + Services 01-06 complete

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md` - Ledger Service definition
- `/var/www/docs/islamic-financing/master-blueprint/07_CORE_BANKING_ADAPTER.md` - GL integration with Fineract
- `/var/www/docs/islamic-financing/erd-docs/ledger-service.sql` - Schema

---

## 🎯 Implementation Requirements

### Responsibilities
- **Double-Entry Bookkeeping**: All transactions balanced
- **Chart of Accounts**: KSA standard COA
- **Journal Entries**: Record all financial transactions
- **Trial Balance**: Real-time balance calculation
- **Financial Reports**: P&L, Balance Sheet

### Database Schema
- `chart_of_accounts`, `journal_entries`, `ledger_balances`

### REST API
- POST `/api/v1/ledger/journal-entries` - Record entry
- GET `/api/v1/ledger/trial-balance` - Get balances
- GET `/api/v1/ledger/accounts/{accountCode}/balance`

### Integration
- Consumes: `LoanDisbursed`, `RepaymentReceived` (create journal entries)
- Publishes: `JournalEntryCreated`

### Accounting Rules
- Debit = Credit (always balanced)
- Asset accounts: Debit increases
- Liability accounts: Credit increases
- Revenue accounts: Credit increases
- Expense accounts: Debit increases

---

## ✅ Success Criteria
- [ ] All journal entries balanced
- [ ] COA follows KSA standards
- [ ] Trial balance calculates correctly
- [ ] Integration with LMS Adapter for GL sync

---

## 🔄 Next: **Prompt 08** - Customer Service
