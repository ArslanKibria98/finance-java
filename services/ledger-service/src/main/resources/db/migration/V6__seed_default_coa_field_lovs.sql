-- Seed default COA field LOV entries for standard product mapping screens.
-- Tenant aligned with existing local test token tenant_id.

INSERT INTO coa_field_lovs (
    tenant_id,
    field_key,
    field_label_en,
    field_label_ar,
    category,
    is_mandatory_default,
    display_order,
    status
)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'COLLECTION_ACCOUNT', 'Collection Account', 'حساب التحصيل', 'COLLECTIONS', TRUE, 10, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'PURCHASE_ACCOUNT', 'Purchase Account', 'حساب الشراء', 'PROCUREMENT', FALSE, 20, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'SUPPLIER_ACCOUNT', 'Supplier Account', 'حساب المورد', 'PROCUREMENT', FALSE, 30, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'OPERATION_ACCOUNT', 'Operation Account', 'حساب التشغيل', 'OPERATIONS', TRUE, 40, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'VAT_ACCOUNT', 'VAT Account', 'حساب ضريبة القيمة المضافة', 'TAX', FALSE, 50, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'ACCRUED_ACCOUNT', 'Accrued Account', 'حساب المستحقات', 'ACCRUALS', FALSE, 60, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'OUTPUT_VAT_ACCOUNT', 'Output VAT Account', 'حساب ضريبة المخرجات', 'TAX', FALSE, 70, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'REVENUE_ACCOUNT', 'Revenue Account', 'حساب الإيرادات', 'REVENUE', TRUE, 80, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'RETURNS_ACCOUNT', 'Returns Account', 'حساب المرتجعات', 'REVENUE', FALSE, 90, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'PAYABLE_ACCOUNT', 'Payable Account', 'حساب الدائنين', 'LIABILITIES', TRUE, 100, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'EARLY_SETTLEMENT_PRINCIPAL', 'Early Settlement Principal', 'أصل التسوية المبكرة', 'SETTLEMENT', TRUE, 110, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'EARLY_SETTLEMENT_PROFIT', 'Early Settlement Profit', 'ربح التسوية المبكرة', 'SETTLEMENT', TRUE, 120, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'LATE_PAYMENT_PRINCIPAL', 'Late Payment Principal', 'أصل السداد المتأخر', 'LATE_PAYMENT', FALSE, 130, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'LATE_PAYMENT_PROFIT', 'Late Payment Profit', 'ربح السداد المتأخر', 'LATE_PAYMENT', FALSE, 140, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'CASH_IN_ACCOUNT', 'Cash In Account', 'حساب التدفق النقدي الداخل', 'CASHFLOW', FALSE, 150, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'CASH_OUT_ACCOUNT', 'Cash Out Account', 'حساب التدفق النقدي الخارج', 'CASHFLOW', FALSE, 160, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'WALLET_ACCOUNT', 'Wallet Account', 'حساب المحفظة', 'WALLET', FALSE, 170, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'COST_ACCOUNT', 'Cost Account', 'حساب التكلفة', 'COSTING', FALSE, 180, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'INVENTORY_ACCOUNT', 'Inventory Account', 'حساب المخزون', 'INVENTORY', FALSE, 190, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'INVESTMENT_ACCOUNT', 'Investment Account', 'حساب الاستثمار', 'INVESTMENT', FALSE, 200, 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000001', 'FEES_ACCOUNT', 'Fees Account', 'حساب الرسوم', 'FEES', FALSE, 210, 'ACTIVE')
ON CONFLICT (tenant_id, field_key) DO NOTHING;
