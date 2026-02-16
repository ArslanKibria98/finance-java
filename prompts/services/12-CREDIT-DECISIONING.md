# 🎯 Prompt 12: Credit Decisioning Service

**Objective**: Implement the Credit Decisioning Service for automated loan approval decisions.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-11

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/10_KYC_CREDIT_DECISIONING.md` - **Primary reference**
- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/erd-docs/credit-service.sql` - Database schema

---

## 🎯 Implementation Requirements

### Responsibilities
- Automated decisioning (rule-based)
- Manual decisioning (workflow routing)
- Decision matrix (amount × risk score)
- Approval limits per role
- Escalation rules

### Decision Matrix (from references)
| Amount | Risk Score | Decision |
|--------|-----------|----------|
| < 50K SAR | > 700 | Auto-Approve |
| < 50K SAR | 600-700 | Manual Review |
| < 50K SAR | < 600 | Auto-Reject |
| 50K-200K SAR | > 750 | Manual Review |
| > 200K SAR | Any | Committee Review |

### Temporal Workflows
- `DecisioningWorkflow` - Route based on decision matrix
  - Auto decisions: Immediate
  - Manual decisions: Route to underwriter
  - Committee decisions: Route to credit committee

### REST API
- POST `/api/v1/decisions/evaluate` - Evaluate loan application
- GET `/api/v1/decisions/{id}` - Get decision
- POST `/api/v1/decisions/{id}/override` - Manual override

### Integration
- Calls: Risk Service (get credit score)
- Publishes: `DecisionMade`, `DecisionOverridden`

---

## ✅ Success Criteria
- [ ] Decision matrix implemented
- [ ] Auto-approve/reject working
- [ ] Manual routing to underwriters
- [ ] Escalation to committee
- [ ] Approval limits enforced

---

## 🔄 Next: **Prompt 13** - Collections Service
