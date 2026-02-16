# 🔑 Prompt 04: Identity Federation Service

**Objective**: Implement the Identity Federation service for multi-tenant authentication and authorization.

**Prerequisites**:
- ✅ All SDK prompts (01-08) complete
- ✅ Service template (00) complete

---

## 📚 Reference Documents

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md`
  - Section: Identity & Access Management

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
  - Section: Identity Federation definition

### ERD Documentation
- `/var/www/docs/islamic-financing/erd-docs/identity-service.sql`

---

## 🎯 Implementation Requirements

### Service Responsibilities
1. **User Management**: Create, update users across tenants
2. **Role Management**: RBAC (Role-Based Access Control)
3. **Keycloak Integration**: Federate to Keycloak realms
4. **SSO**: Single Sign-On across subsidiaries
5. **MFA**: Multi-Factor Authentication

### Database Schema
- `users` - User accounts
- `roles` - Role definitions
- `user_roles` - User-role assignments
- `permissions` - Fine-grained permissions
- `role_permissions` - Role-permission mapping

### Keycloak Integration
- **Realm per tenant**: Each tenant has Keycloak realm
- **User federation**: Sync users to Keycloak
- **Custom claims**: Add tenant_id, customer_id to JWT

### REST API & gRPC
Design APIs for:
- User management (create, read, update)
- Role and permission management
- Role assignment to users
- Keycloak realm synchronization

**Note**: Design based on RBAC requirements and multi-tenancy needs.

### Standard Roles
- `CUSTOMER` - End customer
- `LOAN_OFFICER` - Create/review loans
- `UNDERWRITER` - Approve/reject loans
- `ADMIN` - System administrator
- `AUDITOR` - Read-only access

### Integration
- Consumes: `CustomerCreated` (create user)
- Publishes: `UserCreated`, `RoleAssigned`, `LoginAttempt`

---

## 🧪 Testing Requirements
- Test multi-tenant isolation (users can't see other tenants)
- Test RBAC enforcement
- Test JWT generation with custom claims
- Test Keycloak synchronization

---

## ✅ Success Criteria
- [ ] Users created in both service DB and Keycloak
- [ ] RBAC working with method-level security
- [ ] JWT contains tenant_id, customer_id, roles
- [ ] Multi-tenant isolation enforced
- [ ] SSO working across services
- [ ] All tests pass

---

## 🔄 Next Step
After completing this service, proceed to:
- **Prompt 05**: Lending Service
