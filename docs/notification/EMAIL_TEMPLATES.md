# Novu Email Step — Copy + Setup (all 10 workflows)

> Gmail SMTP is already the **primary email** provider in Novu (integration `nodemailer`, id `6a1d2a4ff457cb565fe70473`).
> Each workflow below still needs an **Email step** added in the Novu dashboard (the 10 workflows are
> `novu-cloud-v1` origin → can't be edited reliably via API). Variables come from the real Kafka payloads
> notification-service sends. Amounts are SAR strings.

## How to add the Email step (per workflow) — do this once per workflow
1. Novu dashboard → **Workflows** → open the workflow.
2. Click **+** under the existing Push/SMS step → **Email**.
3. Drag the Email step so it runs in parallel/after the push step (order doesn't matter for delivery).
4. **Subject** → paste the subject below.
5. **Body** → switch the editor to **HTML/Custom code** (or use the visual editor) → paste the body.
6. For Arabic, either add a second Email step gated on `{{subscriber.locale}} == 'ar'` (advanced) OR keep one
   bilingual body (English first, Arabic below). Simplest: use the **bilingual** body provided.
7. **Save** → **Publish** the workflow.

> Greeting uses `{{subscriber.firstName}}` (set automatically by notification-service from `customer-created`/`customer-updated`).
> If a subscriber has no name yet, set a Novu default: `{{subscriber.firstName | default: 'Customer'}}`.

---

## 1. funds-sent-push-template  (FUNDS_SENT)
**Subject (en):** You sent {{payload.amount}} {{payload.currency}}
**Subject (ar):** لقد أرسلت {{payload.amount}} {{payload.currency}}

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

You have successfully sent {{payload.amount}} {{payload.currency}} to {{payload.recipientMaskedName}}.
Reference: {{payload.transferNumber}}
Fee: {{payload.feeAmount}} {{payload.currency}}

If you did not authorise this transfer, contact support immediately.

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

لقد قمت بإرسال {{payload.amount}} {{payload.currency}} إلى {{payload.recipientMaskedName}} بنجاح.
الرقم المرجعي: {{payload.transferNumber}}
الرسوم: {{payload.feeAmount}} {{payload.currency}}

إذا لم تقم بهذه العملية، يرجى التواصل مع الدعم فوراً.

— سُلِّس
```

## 2. funds-received-push-template  (FUNDS_RECEIVED)
**Subject (en):** You received {{payload.amount}} {{payload.currency}}
**Subject (ar):** لقد استلمت {{payload.amount}} {{payload.currency}}

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

You have received {{payload.amount}} {{payload.currency}} from {{payload.senderMaskedName}}.
Reference: {{payload.transferNumber}}

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

لقد استلمت {{payload.amount}} {{payload.currency}} من {{payload.senderMaskedName}}.
الرقم المرجعي: {{payload.transferNumber}}

— سُلِّس
```

## 3. customer-payment-overdue  (PAYMENT_OVERDUE)
**Subject (en):** Payment overdue — {{payload.overdueAmount}} SAR
**Subject (ar):** دفعة متأخرة — {{payload.overdueAmount}} ريال

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

A payment on your financing is overdue.
Overdue amount: {{payload.overdueAmount}} SAR
Overdue installments: {{payload.overdueInstallments}}
Days past due: {{payload.maxDpd}}
Since: {{payload.overdueDate}}

Please settle the outstanding amount to avoid additional charges.

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

لديك دفعة متأخرة على تمويلك.
المبلغ المتأخر: {{payload.overdueAmount}} ريال
عدد الأقساط المتأخرة: {{payload.overdueInstallments}}
عدد أيام التأخير: {{payload.maxDpd}}
منذ: {{payload.overdueDate}}

يرجى سداد المبلغ المستحق لتجنب أي رسوم إضافية.

— سُلِّس
```

## 4. customer-payment-due  (PAYMENT_DUE)
**Subject (en):** Upcoming payment — {{payload.amount}} SAR due {{payload.dueDate}}
**Subject (ar):** قسط قادم — {{payload.amount}} ريال في {{payload.dueDate}}

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

This is a reminder that installment #{{payload.installmentNumber}} of {{payload.amount}} SAR
is due on {{payload.dueDate}} ({{payload.daysUntilDue}} day(s) remaining).

Please ensure sufficient balance for the payment.

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

تذكير بأن القسط رقم {{payload.installmentNumber}} بقيمة {{payload.amount}} ريال
مستحق في {{payload.dueDate}} (متبقٍ {{payload.daysUntilDue}} يوم/أيام).

يرجى التأكد من توفر الرصيد الكافي للسداد.

— سُلِّس
```

## 5. customer-payment-completed  (PAYMENT_COMPLETED)
**Subject (en):** Payment received — {{payload.amount}} SAR
**Subject (ar):** تم استلام دفعتك — {{payload.amount}} ريال

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

We have received your payment of {{payload.amount}} SAR. Thank you.
Transaction reference: {{payload.providerTransactionId}}

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

لقد استلمنا دفعتك بقيمة {{payload.amount}} ريال. شكراً لك.
الرقم المرجعي للعملية: {{payload.providerTransactionId}}

— سُلِّس
```

## 6. payment-completed-sms-template  (PAYMENT_COMPLETED — SMS variant)
> ⚠️ This is the SMS duplicate of #5. Adding an email step here too may send TWO payment-completed
> emails. Recommend: add email ONLY to #5, leave this SMS-only. If you still want it, reuse #5's copy.

## 7. customer-loan-disbursed  (LOAN_DISBURSED)
**Subject (en):** Your financing has been disbursed — {{payload.amount}} SAR
**Subject (ar):** تم صرف تمويلك — {{payload.amount}} ريال

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

Good news — your financing of {{payload.amount}} SAR has been disbursed on {{payload.disbursementDate}}.
The funds are now available.

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

خبر سار — تم صرف تمويلك بقيمة {{payload.amount}} ريال بتاريخ {{payload.disbursementDate}}.
المبلغ متاح الآن.

— سُلِّس
```

## 8. loan-disbursed-sms-template  (LOAN_DISBURSED — SMS variant)
> ⚠️ SMS duplicate of #7. Same double-send caveat — recommend email only on #7. If needed, reuse #7's copy.

## 9. loan-approval-push-template  (LOAN_APPROVED)
**Subject (en):** Your financing is approved — {{payload.approvedAmount}} SAR
**Subject (ar):** تمت الموافقة على تمويلك — {{payload.approvedAmount}} ريال

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

Congratulations — your financing application has been approved for {{payload.approvedAmount}} SAR.
You will be notified once the funds are disbursed.

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

تهانينا — تمت الموافقة على طلب تمويلك بقيمة {{payload.approvedAmount}} ريال.
سيتم إشعارك عند صرف المبلغ.

— سُلِّس
```

## 10. user-login-push-template  (USER_LOGIN)
> Variables here are NOT under `payload.` nesting — notification-service unwraps the login payload, so use
> `{{payload.name}}`, `{{payload.loginAt}}`, `{{payload.loginMethod}}` directly.
> ⚠️ This fires on EVERY login — an email each time can feel spammy. Consider keeping login as push-only,
> or send email only for new-device logins later.

**Subject (en):** New sign-in to your account
**Subject (ar):** تسجيل دخول جديد إلى حسابك

**Body (bilingual):**
```
Dear {{subscriber.firstName}},

A sign-in to your account was detected.
Time: {{payload.loginAt}}
Method: {{payload.loginMethod}}

If this wasn't you, secure your account and contact support immediately.

— Sullis
______________________________________

عزيزي {{subscriber.firstName}}،

تم تسجيل دخول إلى حسابك.
الوقت: {{payload.loginAt}}
الطريقة: {{payload.loginMethod}}

إذا لم تكن أنت، يرجى تأمين حسابك والتواصل مع الدعم فوراً.

— سُلِّس
```

---

## Pre-flight checklist before emails actually deliver
- [x] Gmail SMTP integration = primary email in Novu
- [ ] Email step added + published on each workflow above
- [ ] Subscribers have `email` (auto from customer-created/updated; **backfill** existing customers separately)
- [ ] Test: trigger one event (e.g. loan disbursed) and confirm email arrives from `momin.baig@xintsolutions.com`
- [ ] Gmail sending limit (~500/day) is enough for DEV/QA volume
