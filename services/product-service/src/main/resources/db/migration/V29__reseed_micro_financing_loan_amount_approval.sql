-- V29__reseed_micro_financing_loan_amount_approval.sql
-- Reseed MICRO-001 approval workflows using loan_amount threshold (<=10000 AUTO, >10000 MANUAL).
-- Replaces the earlier risk_score-based rules with simple amount-based routing.

DO $$
DECLARE
    v_product_id UUID := '4210f18e-8c35-4b12-9b7f-c96de95767c7';
    v_tenant_id UUID;
    v_auto_id UUID;
    v_manual_id UUID;
BEGIN
    SELECT tenant_id INTO v_tenant_id FROM products WHERE id = v_product_id;
    IF v_tenant_id IS NULL THEN
        RAISE NOTICE 'Micro-Financing product % not found — skipping seed', v_product_id;
        RETURN;
    END IF;

    -- Deactivate all existing MICRO-001 approval workflows (preserve history)
    UPDATE product_approval_workflows
    SET is_active = FALSE, updated_at = NOW()
    WHERE product_id = v_product_id;

    -- Ensure approval_duration_days = 2 for faster testing (override if needed)
    UPDATE product_duration_settings
    SET approval_duration_days = 2, updated_at = NOW()
    WHERE product_id = v_product_id;

    -- ── AUTO_APPROVAL: loan_amount <= 10000 ──
    v_auto_id := gen_random_uuid();
    INSERT INTO product_approval_workflows
        (id, tenant_id, product_id, workflow_type, name_en, name_ar, description,
         is_active, priority, created_at, updated_at, version)
    VALUES
        (v_auto_id, v_tenant_id, v_product_id, 'AUTO_APPROVAL',
         'Auto Approval - Micro Amount',
         'الموافقة التلقائية - مبلغ صغير',
         'Auto-approve micro financing when loan_amount <= 10000 SAR',
         TRUE, 1, NOW(), NOW(), 1);

    INSERT INTO product_approval_conditions
        (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
    VALUES
        (v_tenant_id, v_auto_id, 'loan_amount', 'LTE', '"10000"'::jsonb, 1, NOW(), NOW());

    INSERT INTO product_approval_actions
        (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
    VALUES
        (v_tenant_id, v_auto_id, 'APPROVE',
         '{"notify_customer": true}'::jsonb, 1, NOW(), NOW());

    -- ── MANUAL_APPROVAL: loan_amount > 10000 ──
    v_manual_id := gen_random_uuid();
    INSERT INTO product_approval_workflows
        (id, tenant_id, product_id, workflow_type, name_en, name_ar, description,
         is_active, priority, created_at, updated_at, version)
    VALUES
        (v_manual_id, v_tenant_id, v_product_id, 'MANUAL_APPROVAL',
         'Manual Review - Above Threshold',
         'مراجعة يدوية - فوق الحد',
         'Route to underwriter for manual review when loan_amount > 10000 SAR',
         TRUE, 2, NOW(), NOW(), 1);

    INSERT INTO product_approval_conditions
        (tenant_id, workflow_id, field, operator, value, sort_order, created_at, updated_at)
    VALUES
        (v_tenant_id, v_manual_id, 'loan_amount', 'GT', '"10000"'::jsonb, 1, NOW(), NOW());

    INSERT INTO product_approval_actions
        (tenant_id, workflow_id, action_type, configuration, sort_order, created_at, updated_at)
    VALUES
        (v_tenant_id, v_manual_id, 'ASSIGN_REVIEWER',
         '{"reviewer_role": "underwriter"}'::jsonb, 1, NOW(), NOW()),
        (v_tenant_id, v_manual_id, 'NOTIFY',
         '{"notify_email": "underwriters@kfs.com"}'::jsonb, 2, NOW(), NOW());

    RAISE NOTICE 'Seeded MICRO-001 approval rules: AUTO=%, MANUAL=%', v_auto_id, v_manual_id;
END $$;
