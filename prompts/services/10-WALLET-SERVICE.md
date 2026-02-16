# 💳 Prompt 10: Wallet Service

**Objective**: Implement the Wallet Service for digital wallet and payment processing.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-09

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/master-blueprint/18_PAYMENT_PROCESSING.md`
- `/var/www/docs/islamic-financing/erd-docs/wallet-service.sql`

---

## 🎯 Implementation Requirements

### Responsibilities
- Customer wallet management
- Fund transfers (internal/external)
- Transaction history
- Balance management
- Payment method management (bank account, card)

### Database Schema
- `wallets`, `wallet_transactions`, `payment_methods`

### REST API & gRPC
Design APIs for:
- Wallet operations (create, credit, debit)
- Fund transfers
- Balance queries
- Transaction history
- Payment method management

**Note**: Design based on payment processing requirements and SAGA workflows.

### Temporal Workflows
- `TransferWorkflow` - SAGA for fund transfers
  - Debit source wallet
  - Credit destination wallet
  - Compensation on failure

### Integration
- Calls: Payment Adapter (for bank transfers)
- Publishes: `WalletCreated`, `WalletCredited`, `WalletDebited`, `TransferCompleted`

### Transaction Types
- CREDIT, DEBIT, TRANSFER, LOAN_DISBURSEMENT, LOAN_REPAYMENT

---

## ✅ Success Criteria
- [ ] Wallet CRUD working
- [ ] Atomic transfers (SAGA)
- [ ] Balance always accurate
- [ ] Transaction history queryable
- [ ] Payment method integration

---

## 🔄 Next: **Prompt 11** - Risk Service
