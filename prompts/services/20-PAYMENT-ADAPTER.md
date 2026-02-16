# 💳 Prompt 20: Payment Adapter (FINAL SERVICE)

**Objective**: Implement the Payment Adapter for payment gateway integrations (SADAD, Mada, etc.).

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-19

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/master-blueprint/18_PAYMENT_PROCESSING.md` - **Primary reference**
- `/var/www/docs/islamic-financing/erd-docs/payment-adapter.sql` - Database schema

---

## 🎯 Implementation Requirements

### Responsibilities
- Payment gateway integrations
- Bank transfer processing
- Payment status reconciliation
- Refund processing

### Payment Providers

#### 1. SADAD (Bill Payment)
- Integration: SADAD API
- Use case: Customers pay via SADAD bill
- Flow: Generate bill → Customer pays → Webhook callback

#### 2. Mada (Debit Card)
- Integration: Hyperpay or PayTabs
- Use case: Card payments
- Flow: Tokenize card → Charge → Callback

#### 3. Bank Transfer
- Integration: SARIE (Saudi payment system)
- Use case: Direct bank transfers
- Flow: Initiate transfer → Poll status → Confirm

### REST API
- POST `/api/v1/payments/sadad/generate-bill` - Create SADAD bill
- POST `/api/v1/payments/mada/charge` - Charge Mada card
- POST `/api/v1/payments/bank-transfer` - Initiate transfer
- GET `/api/v1/payments/{id}/status` - Check payment status
- POST `/api/v1/payments/{id}/refund` - Process refund

### Webhooks (from providers)
- POST `/webhooks/sadad` - SADAD payment confirmation
- POST `/webhooks/mada` - Mada payment result
- POST `/webhooks/bank-transfer` - Transfer confirmation

### Database Schema
- `payment_transactions`, `payment_methods`, `refunds`

### Integration
- Called by: Wallet Service, Lending Service
- Publishes: `PaymentInitiated`, `PaymentCompleted`, `PaymentFailed`, `RefundProcessed`

---

## 🧪 Testing Requirements
- Mock SADAD API with WireMock
- Test webhook signature verification
- Test idempotency (duplicate webhooks)
- Test refund processing

---

## ✅ Success Criteria
- [ ] SADAD integration working
- [ ] Mada integration working
- [ ] Bank transfer working
- [ ] Webhook authentication secure
- [ ] Idempotency prevents duplicate processing
- [ ] Refunds working

---

## 🎉 CONGRATULATIONS!

You've completed **ALL 20 microservices**!

---

## 🔄 Next: **Infrastructure Setup**

Proceed to:
- **Prompt 21**: `/var/www/islamic-financing-platform/prompts/infrastructure/01-DOCKER-COMPOSE.md`
- **Prompt 22**: `02-KUBERNETES-ELK.md`
- **Prompt 23**: `03-KUBERNETES-MONITORING.md`
- **Prompt 24**: `04-KUBERNETES-SERVICES.md`
