CREATE TABLE reschedule_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    reschedule_type VARCHAR(50) NOT NULL,
    label_en VARCHAR(255) NOT NULL,
    label_ar VARCHAR(255) NOT NULL,
    description_en TEXT,
    description_ar TEXT,
    fields_config JSONB,
    rules_config JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    requires_approval BOOLEAN DEFAULT FALSE,
    approver_role VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, reschedule_type)
);

-- Seed Initial Configs for Tenant 00000000-0000-0000-0000-000000000001
INSERT INTO reschedule_configs (id, tenant_id, reschedule_type, label_en, label_ar, description_en, description_ar, fields_config, rules_config, requires_approval, approver_role)
VALUES 
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'SKIP_PAYMENT', 'Skip Payment', 'تأجيل قسط', 
 'Move one installment to end of tenure. No extra profit charged (Sharia-compliant).', 
 'تأجيل قسط واحد إلى نهاية مدة التمويل دون أي رسوم إضافية.',
 '[{"name": "skipmonth", "type": "DATE", "label": "Month to Skip", "labelAr": "الشهر المراد تأجيله", "required": true, "hint": "Select the upcoming installment month you want to skip.", "validation": {"format": "YYYY-MM-01", "note": "Must be a future installment month. Use first day of month."}}, {"name": "justification", "type": "TEXT", "label": "Reason for Skip (Optional)", "labelAr": "سبب التأجيل (اختياري)", "placeholder": "Briefly mention why you want to skip this installment...", "required": false, "hint": "Optional — helps us serve you better.", "validation": {"minLength": 0, "maxLength": 300}}]',
 '{"max_skips_total": 2, "min_months_between_skips": 6, "min_loan_age_months": 3}', false, NULL),

(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'TENURE_EXTENSION', 'Tenure Extension', 'تمديد مدة التمويل',
 'Extend your loan tenure to reduce your monthly installment.',
 'تمديد مدة التمويل لتقليل قيمة القسط الشهري.',
 '[{"name": "extensionMonths", "type": "INTEGER", "label": "Extension Duration", "labelAr": "مدة التمديد", "placeholder": "Enter months", "unit": "months", "required": true, "hint": "Extending tenure reduces monthly installment but increases total profit paid.", "validation": {"min": 1, "max": 12, "step": 1, "maxTotalTenureMonths": 72}}, {"name": "justification", "type": "TEXT", "label": "Reason for Extension", "labelAr": "سبب طلب التمديد", "placeholder": "Describe your financial hardship...", "required": true, "hint": "Reviewed by the operations team.", "validation": {"minLength": 20, "maxLength": 500}}, {"name": "attachment", "type": "ATTACHMENT", "label": "Support Document", "labelAr": "المستند الداعم", "required": false}, {"name": "hardshipDeclaration", "type": "BOOLEAN", "label": "I declare that I am experiencing financial hardship", "labelAr": "أقر بأنني أمر بظروف مالية صعبة", "required": true}]',
 '{"max_total_tenure_months": 72, "max_extension_months": 12, "min_loan_age_months": 6, "max_dpd_allowed": 30}', true, 'OPERATIONS_HEAD'),

(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'PAYMENT_HOLIDAY', 'Payment Holiday', 'إجازة سداد',
 'Pause payments for up to 3 months. No extra profit accrues (Sharia-compliant).',
 'أوقف أقساطك مؤقتاً لمدة تصل إلى 3 أشهر دون احتساب أرباح إضافية.',
 '[{"name": "holidayMonths", "type": "INTEGER", "label": "Holiday Duration", "labelAr": "مدة الإجازة", "placeholder": "Enter months (1–3)", "unit": "months", "required": true, "hint": "Tenure will be extended by the number of holiday months.", "validation": {"min": 1, "max": 3, "step": 1}}, {"name": "skipmonth", "type": "DATE", "label": "Holiday Start Month", "labelAr": "بداية إجازة السداد", "required": true}, {"name": "justification", "type": "TEXT", "label": "Justification", "labelAr": "المبرر", "placeholder": "Explain why you need a payment holiday...", "required": true, "validation": {"minLength": 20, "maxLength": 500}}, {"name": "attachment", "type": "ATTACHMENT", "label": "Support Document", "labelAr": "المستند الداعم", "required": true}]',
 '{"max_holiday_months": 3, "min_loan_age_months": 3}', true, 'OPERATIONS_HEAD'),

(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'RESTRUCTURING', 'Full Restructuring', 'إعادة هيكلة كاملة',
 'Complete loan restructure for financial distress.',
 'إعادة هيكلة شاملة للتمويل في حالات الضائقة المالية.',
 '[{"name": "restructuringOption", "type": "SELECT", "label": "Restructuring Option", "labelAr": "خيار إعادة الهيكلة", "required": true, "options": [{"value": "REDUCE_PRINCIPAL", "label": "Principal Reduction", "labelAr": "تخفيض الأصل"}, {"value": "REDUCE_PROFIT_RATE", "label": "Profit Rate Reduction", "labelAr": "تخفيض معدل الربح"}, {"value": "EXTEND_TENURE", "label": "Tenure Extension", "labelAr": "تمديد مدة التمويل"}, {"value": "COMBINATION", "label": "Combination", "labelAr": "مزيج"}]}, {"name": "extensionMonths", "type": "INTEGER", "label": "Extension Months", "labelAr": "عدد أشهر التمديد", "required": false, "conditional": true, "showWhen": "restructuringOption IN [EXTEND_TENURE, COMBINATION]"}, {"name": "newProfitRate", "type": "DECIMAL", "label": "Requested New Profit Rate (%)", "labelAr": "معدل الربح المقترح (%)", "required": false, "conditional": true, "showWhen": "restructuringOption IN [REDUCE_PROFIT_RATE, COMBINATION]"}, {"name": "justification", "type": "TEXT", "label": "Financial Distress Explanation", "labelAr": "شرح الوضع المالي", "required": true, "validation": {"minLength": 50, "maxLength": 1000}}, {"name": "hardshipDeclaration", "type": "BOOLEAN", "label": "I declare I am under documented financial distress", "labelAr": "أقر بأنني في ضائقة مالية موثقة", "required": true}, {"name": "attachmentUrl", "type": "ATTACHMENT", "label": "Financial Hardship Evidence", "labelAr": "إثبات الضائقة المالية", "required": true}]',
 '{"requires_financial_distress": true, "approver": "CREDIT_COMMITTEE"}', true, 'CREDIT_COMMITTEE');
