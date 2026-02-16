# 👤 Prompt 08: Customer Service

**Objective**: Implement the Customer Service for customer profile and relationship management.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-07

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md` - Customer Service
- `/var/www/docs/islamic-financing/erd-docs/customer-service.sql` - Complete schema
- `/var/www/docs/islamic-financing/user-journeys/01_INDIVIDUAL_BORROWER.md` - Customer flows

---

## 🎯 Implementation Requirements

### Responsibilities
- Customer profile management (non-PII data)
- Employment information
- Financial profile (income, expenses)
- Relationship management
- Customer segmentation

### Database Schema
- `customers`, `employment_info`, `financial_profiles`, `customer_segments`

### REST API & gRPC
Design APIs for:
- Customer profile management
- Employment information updates
- Financial profile queries
- Customer segmentation

**Note**: Design based on user journeys and customer lifecycle requirements.

### Integration
- Calls: PII Vault (for encrypted data), Global Profile Index
- Publishes: `CustomerCreated`, `CustomerUpdated`, `EmploymentVerified`

### Customer Types
- INDIVIDUAL, BUSINESS_OWNER, SALARIED_EMPLOYEE, GOVERNMENT_EMPLOYEE

---

## ✅ Success Criteria
- [ ] Customer CRUD working
- [ ] PII stored in PII Vault (tokenized)
- [ ] Global Profile Index updated
- [ ] Employment verification flow working

---

## 🔄 Next: **Prompt 09** - Product Service
