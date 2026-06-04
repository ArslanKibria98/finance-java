# Novu Email Step — Copy + Setup (all 10 workflows)

> Gmail SMTP is the **primary email** provider in Novu (integration `nodemailer`, id recreated on
> 2026-06-01). Variables come from the real Kafka payloads notification-service sends. Amounts are SAR strings.

## ⚠️ Two rules that MUST be followed (learned the hard way — 2026-06-01)

1. **Variables are FLAT — no `payload.` prefix.** This Novu Cloud engine renders the trigger payload
   flat: use `{{amount}}`, `{{currency}}`, `{{transferNumber}}`. **`{{payload.amount}}` renders BLANK.**
   (Push steps already used `{{amount}}` and worked; the email steps wrongly used `{{payload.amount}}`
   and came out empty.) Only `{{subscriber.firstName}}` keeps the `subscriber.` prefix (built-in field).
2. **Email body must be HTML.** Email steps are `customHtml` — plain-text newlines (`\n`) collapse into
   one run-on line. Use `<p>…</p>` paragraphs and `<br/>` for line breaks. SMS/push bodies stay plain text.

> The 10 workflows ARE editable via the v1 API (`PUT /v1/notification-templates/{id}`) — earlier "can't edit
> via API" note was wrong; the email just had the wrong variable prefix. You can also edit in the dashboard.

## How to add / edit the Email step (per workflow)
1. Novu dashboard → **Workflows** → open the workflow.
2. Click **+** under the existing Push/SMS step → **Email** (or open the existing Email step).
3. **Subject** → paste the subject below (flat vars).
4. **Body** → switch editor to **HTML/Custom code** → paste the HTML body.
5. For Arabic, keep one bilingual body (English block, then Arabic block) as shown.
6. **Save** → **Publish** the workflow.

> Greeting uses `{{subscriber.firstName}}` (set automatically by notification-service from
> `customer-created`/`customer-updated`). No-name fallback: `{{subscriber.firstName | default: 'Customer'}}`.

---

## 1. funds-sent-push-template  (FUNDS_SENT)
**Subject (en):** You sent {{amount}} {{currency}}
**Subject (ar):** لقد أرسلت {{amount}} {{currency}}

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>You have successfully sent <strong>{{amount}} {{currency}}</strong> to {{recipientMaskedName}}.<br/>
Reference: {{transferNumber}}<br/>
Fee: {{feeAmount}} {{currency}}</p>
<p>If you did not authorise this transfer, contact support immediately.</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">لقد قمت بإرسال <strong>{{amount}} {{currency}}</strong> إلى {{recipientMaskedName}} بنجاح.<br/>
الرقم المرجعي: {{transferNumber}}<br/>
الرسوم: {{feeAmount}} {{currency}}</p>
<p dir="rtl">إذا لم تقم بهذه العملية، يرجى التواصل مع الدعم فوراً.</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

## 2. funds-received-push-template  (FUNDS_RECEIVED)
**Subject (en):** You received {{amount}} {{currency}}
**Subject (ar):** لقد استلمت {{amount}} {{currency}}

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>You have received <strong>{{amount}} {{currency}}</strong> from {{senderMaskedName}}.<br/>
Reference: {{transferNumber}}</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">لقد استلمت <strong>{{amount}} {{currency}}</strong> من {{senderMaskedName}}.<br/>
الرقم المرجعي: {{transferNumber}}</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

## 3. customer-payment-overdue  (PAYMENT_OVERDUE)
**Subject (en):** Payment overdue — {{overdueAmount}} SAR
**Subject (ar):** دفعة متأخرة — {{overdueAmount}} ريال

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>A payment on your financing is overdue.<br/>
Overdue amount: {{overdueAmount}} SAR<br/>
Overdue installments: {{overdueInstallments}}<br/>
Days past due: {{maxDpd}}<br/>
Since: {{overdueDate}}</p>
<p>Please settle the outstanding amount to avoid additional charges.</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">لديك دفعة متأخرة على تمويلك.<br/>
المبلغ المتأخر: {{overdueAmount}} ريال<br/>
عدد الأقساط المتأخرة: {{overdueInstallments}}<br/>
عدد أيام التأخير: {{maxDpd}}<br/>
منذ: {{overdueDate}}</p>
<p dir="rtl">يرجى سداد المبلغ المستحق لتجنب أي رسوم إضافية.</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

## 4. customer-payment-due  (PAYMENT_DUE)
**Subject (en):** Upcoming payment — {{amount}} SAR due {{dueDate}}
**Subject (ar):** قسط قادم — {{amount}} ريال في {{dueDate}}

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>This is a reminder that installment #{{installmentNumber}} of <strong>{{amount}} SAR</strong>
is due on {{dueDate}} ({{daysUntilDue}} day(s) remaining).</p>
<p>Please ensure sufficient balance for the payment.</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">تذكير بأن القسط رقم {{installmentNumber}} بقيمة <strong>{{amount}} ريال</strong>
مستحق في {{dueDate}} (متبقٍ {{daysUntilDue}} يوم/أيام).</p>
<p dir="rtl">يرجى التأكد من توفر الرصيد الكافي للسداد.</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

## 5. customer-payment-completed  (PAYMENT_COMPLETED)
**Subject (en):** Payment received — {{amount}} SAR
**Subject (ar):** تم استلام دفعتك — {{amount}} ريال

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>We have received your payment of <strong>{{amount}} SAR</strong>. Thank you.<br/>
Transaction reference: {{providerTransactionId}}</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">لقد استلمنا دفعتك بقيمة <strong>{{amount}} ريال</strong>. شكراً لك.<br/>
الرقم المرجعي للعملية: {{providerTransactionId}}</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

## 6. payment-completed-sms-template  (PAYMENT_COMPLETED — SMS variant)
> ⚠️ This is the SMS duplicate of #5. Adding an email step here too may send TWO payment-completed
> emails. Recommend: add email ONLY to #5, leave this SMS-only. If you still want it, reuse #5's copy.

## 7. customer-loan-disbursed  (LOAN_DISBURSED)
**Subject (en):** Your financing has been disbursed — {{amount}} SAR
**Subject (ar):** تم صرف تمويلك — {{amount}} ريال

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>Good news — your financing of <strong>{{amount}} SAR</strong> has been disbursed on {{disbursementDate}}.
The funds are now available.</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">خبر سار — تم صرف تمويلك بقيمة <strong>{{amount}} ريال</strong> بتاريخ {{disbursementDate}}.
المبلغ متاح الآن.</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

## 8. loan-disbursed-sms-template  (LOAN_DISBURSED — SMS variant)
> ⚠️ SMS duplicate of #7. Same double-send caveat — recommend email only on #7. If needed, reuse #7's copy.

## 9. loan-approval-push-template  (LOAN_APPROVED)
**Subject (en):** Your financing is approved — {{approvedAmount}} SAR
**Subject (ar):** تمت الموافقة على تمويلك — {{approvedAmount}} ريال

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>Congratulations — your financing application has been approved for <strong>{{approvedAmount}} SAR</strong>.
You will be notified once the funds are disbursed.</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">تهانينا — تمت الموافقة على طلب تمويلك بقيمة <strong>{{approvedAmount}} ريال</strong>.
سيتم إشعارك عند صرف المبلغ.</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

## 10. user-login-push-template  (USER_LOGIN)
> notification-service unwraps the login payload to flat keys, so use `{{name}}`, `{{loginAt}}`,
> `{{loginMethod}}` (flat — no `payload.` prefix, same rule as everything else).
> ⚠️ This fires on EVERY login — an email each time can feel spammy. Consider keeping login as push-only,
> or send email only for new-device logins later.

**Subject (en):** New sign-in to your account
**Subject (ar):** تسجيل دخول جديد إلى حسابك

**Body (HTML, bilingual):**
```html
<p>Dear {{subscriber.firstName}},</p>
<p>A sign-in to your account was detected.<br/>
Time: {{loginAt}}<br/>
Method: {{loginMethod}}</p>
<p>If this wasn't you, secure your account and contact support immediately.</p>
<p>&mdash; Sullis</p>
<hr/>
<p dir="rtl">عزيزي {{subscriber.firstName}}،</p>
<p dir="rtl">تم تسجيل دخول إلى حسابك.<br/>
الوقت: {{loginAt}}<br/>
الطريقة: {{loginMethod}}</p>
<p dir="rtl">إذا لم تكن أنت، يرجى تأمين حسابك والتواصل مع الدعم فوراً.</p>
<p dir="rtl">&mdash; سُلِّس</p>
```

---

## Status
- [x] Gmail SMTP integration = primary email in Novu
- [x] **#1 funds-sent** — email step set with flat vars + HTML (done 2026-06-01)
- [ ] #2–#10 — apply flat vars + HTML body per the copy above
- [ ] Subscribers have `email` (auto from customer-created/updated; **backfill** existing customers separately)
- [ ] Test each: trigger one event and confirm email arrives from `momin.baig@xintsolutions.com` with values + spacing
- [ ] Gmail sending limit (~500/day) is enough for DEV/QA volume
```
