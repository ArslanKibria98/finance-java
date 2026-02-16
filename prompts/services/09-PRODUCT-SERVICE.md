# 📦 Prompt 09: Product Service

**Objective**: Implement the Product Service for financing product catalog management.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-08

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/erd-docs/product-service.sql`
- `/var/www/docs/islamic-financing/product-specifications/` - **ALL product specs**

---

## 🎯 Implementation Requirements

### Responsibilities
- Product catalog management (Murabaha, Ijara, Tawarruq)
- Pricing rules (profit rates, fees)
- Eligibility criteria
- Product versioning

### Database Schema
- `products`, `product_variants`, `pricing_rules`, `eligibility_criteria`

### Products to Implement
1. **Murabaha Personal Financing** - Asset purchase financing
2. **Tawarruq Cash Financing** - Liquidity financing
3. **Ijara Auto Financing** - Vehicle leasing
4. **Diminishing Musharaka Home Financing** - Home purchase

### REST API & gRPC
Design APIs for:
- Product catalog queries
- Eligibility checks
- Loan calculation (amounts, schedules, profit)
- Product management

**Note**: Design based on product specifications and business requirements.

### Integration
- Publishes: `ProductCreated`, `ProductUpdated`, `PricingChanged`

---

## ✅ Success Criteria
- [ ] All 4 product types implemented
- [ ] Eligibility checks working
- [ ] Profit rate calculation per product
- [ ] Product versioning supported

---

## 🔄 Next: **Prompt 10** - Wallet Service
