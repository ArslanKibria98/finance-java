-- V38: Casbin policies for simplified admin COA movements (ledger-service)
-- Maps @SecuredEndpoint(obj = "ledger.admin-movements", act = "create")

INSERT INTO casbin_rule (ptype, v0, v1, v2)
VALUES
    ('p', 'super_admin', 'ledger.admin-movements', '*'),
    ('p', 'admin', 'ledger.admin-movements', 'create'),
    ('p', 'head_of_accounts', 'ledger.admin-movements', 'create')
ON CONFLICT DO NOTHING;
