# 📧 Prompt 16: Notification Service

**Objective**: Implement the Notification Service for multi-channel notifications (SMS, Email, Push).

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-15

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/master-blueprint/19_NOTIFICATION_COMMUNICATION.md`

---

## 🎯 Implementation Requirements

### Responsibilities
- Send notifications (SMS, Email, Push, In-app)
- Template management (Arabic/English)
- Delivery tracking
- Preference management (opt-in/opt-out)
- Rate limiting

### Database Schema
- `notification_templates`, `notifications`, `delivery_logs`, `preferences`

### Channels
- SMS - STC, Mobily, Zain integrations
- Email - AWS SES
- Push - FCM (Firebase Cloud Messaging)
- In-app - WebSocket

### REST API & gRPC
Design APIs for:
- Multi-channel notification sending
- Delivery status tracking
- Template management (Arabic/English)
- User preference management
- Rate limiting configuration

**Note**: Design based on notification channels and template requirements.

### Templates
- `LOAN_APPROVED` - "Your loan has been approved"
- `PAYMENT_DUE` - "Payment due in 3 days"
- `PAYMENT_OVERDUE` - "Payment overdue"
- `DISBURSEMENT_SUCCESS` - "Funds transferred"

Support Arabic and English.

### Integration
- Consumes: ALL domain events (sends notifications)
- Publishes: `NotificationSent`, `NotificationFailed`

---

## ✅ Success Criteria
- [ ] SMS sending working (STC integration)
- [ ] Email sending working (AWS SES)
- [ ] Templates support Arabic/English
- [ ] Delivery tracking working
- [ ] Rate limiting enforced
- [ ] Preferences respected (no spam)

---

## 🔄 Next: **Prompt 17** - Audit Service
