-- ============================================================================
-- V13: Seed purpose of finance data for default tenant 001
-- V8 seeded with tenant 000, this adds for tenant 001
-- ============================================================================

INSERT INTO purpose_of_finance (tenant_id, code, name_en, name_ar, sort_order)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'HOUSEHOLD', 'Household Expenses', 'مصاريف منزلية', 1),
    ('00000000-0000-0000-0000-000000000001', 'FAMILY_SUPPORT', 'Family Support', 'دعم الأسرة', 2),
    ('00000000-0000-0000-0000-000000000001', 'EDUCATION', 'Education', 'التعليم', 3),
    ('00000000-0000-0000-0000-000000000001', 'PERSONAL', 'Personal', 'شخصي', 4),
    ('00000000-0000-0000-0000-000000000001', 'MEDICAL_TREATMENT', 'Medical Treatment', 'العلاج الطبي', 5),
    ('00000000-0000-0000-0000-000000000001', 'VEHICLE_PURCHASE', 'Vehicle Purchase', 'شراء مركبة', 6),
    ('00000000-0000-0000-0000-000000000001', 'HOME_RENOVATION', 'Home Renovation', 'ترميم المنزل', 7),
    ('00000000-0000-0000-0000-000000000001', 'DEBT_CONSOLIDATION', 'Debt Consolidation', 'توحيد الديون', 8),
    ('00000000-0000-0000-0000-000000000001', 'BUSINESS', 'Business', 'أعمال تجارية', 9),
    ('00000000-0000-0000-0000-000000000001', 'OTHER', 'Other', 'أخرى', 10)
ON CONFLICT (tenant_id, code) DO NOTHING;
