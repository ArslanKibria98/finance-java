# ⚠️ Prompt 11: Risk Service

**Objective**: Implement the Risk Service for credit scoring and risk assessment.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-10

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/master-blueprint/10_KYC_CREDIT_DECISIONING.md` - **Credit decisioning algorithms**
- `/var/www/docs/islamic-financing/erd-docs/risk-service.sql`

---

## 🎯 Implementation Requirements

### Responsibilities
- Credit scoring (internal + Simah)
- Affordability assessment (DBR calculation)
- Fraud detection
- Portfolio risk metrics (PAR, NPL)
- Concentration risk

### Database Schema
- `credit_scores`, `risk_assessments`, `fraud_alerts`, `risk_models`

### REST API & gRPC
Design APIs for:
- Credit scoring
- DBR (Debt Burden Ratio) calculation
- Portfolio risk metrics
- Fraud detection

**Note**: Design based on SAMA compliance requirements and risk models.

### Credit Scoring Model (from references)
- Simah score (weight: 40%)
- Salary level (weight: 25%)
- Employment stability (weight: 20%)
- Existing debt (weight: 15%)

### DBR Calculation
- **Formula**: (Total Monthly Obligations / Monthly Income) × 100
- **SAMA Limit**: Max 55% DBR for consumer loans

### Integration
- Calls: Simah API (credit bureau)
- Publishes: `CreditScoreCalculated`, `FraudDetected`, `RiskAssessmentCompleted`

---

## ✅ Success Criteria
- [ ] Credit scoring algorithm implemented
- [ ] DBR calculation correct (SAMA compliant)
- [ ] Simah integration working
- [ ] Fraud rules detecting anomalies
- [ ] Portfolio risk metrics accurate

---

## 🔄 Next: **Prompt 12** - Credit Decisioning Service
