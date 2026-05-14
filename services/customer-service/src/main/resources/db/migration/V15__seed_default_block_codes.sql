-- V15: Seed default block codes
INSERT INTO block_codes (id, tenant_id, code, description, type, category, is_active, created_at, updated_at)
VALUES 
    ('1563f38c-6c69-4639-826c-578ca27399fe', '00000000-0000-0000-0000-000000000001', 'AML001', 'Money Laundering Indicator - Transaction Pattern', 'HARD_BLOCK', 'AML', TRUE, NOW(), NOW()),
    ('51d2b49c-2806-4881-9ddf-bdea12551a50', '00000000-0000-0000-0000-000000000001', 'COMP001', 'Compliance Review Required - Document Mismatch', 'HARD_BLOCK', 'COMPLIANCE', TRUE, NOW(), NOW()),
    ('243d3255-4f02-4e6b-ba2a-c01337bf0659', '00000000-0000-0000-0000-000000000001', 'SANCT001', 'Sanctions Match - Block Transaction', 'HARD_BLOCK', 'SANCTION', TRUE, NOW(), NOW()),
    ('75db145b-54d8-45a3-81b3-bf570c69650d', '00000000-0000-0000-0000-000000000001', 'FRAUD001', 'High Probability of Identity Theft', 'HARD_BLOCK', 'FRAUD', TRUE, NOW(), NOW()),
    ('350473d2-73be-4b0a-8cec-1f5901d40648', '00000000-0000-0000-0000-000000000001', 'SANC002', 'TESTING', 'HARD_BLOCK', 'SANCTION', TRUE, NOW(), NOW()),
    ('98a8edc1-ed1a-4049-8bc3-0f8efcbf16a8', '00000000-0000-0000-0000-000000000001', 'COMP002', 'High Risk INd', 'HARD_BLOCK', 'COMPLIANCE', TRUE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET 
    code = EXCLUDED.code,
    description = EXCLUDED.description,
    type = EXCLUDED.type,
    category = EXCLUDED.category;
