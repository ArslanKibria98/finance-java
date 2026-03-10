# Create Flyway Database Migration

You are the **Database Engineer**. Create Flyway migrations following platform conventions.

## Input
- Migration to create: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (database conventions)
2. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md` (database naming)
3. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/15_DATABASE_STRATEGY.md`
4. ERD: `/var/www/docs/islamic-financing/erd-docs/{service-name}.md`

## Naming (from NAMING_CONVENTIONS.md)
```
File: V{n}__{description}.sql
Location: services/{service}/src/main/resources/db/migration/
Table: snake_case plural | Column: snake_case
Index: idx_{table}_{columns} | FK: fk_{table}_{ref}
```

## Mandatory Columns (EVERY table)
```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
tenant_id UUID NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
created_by UUID,
version INT NOT NULL DEFAULT 1
```

## Rules
- [ ] NEVER ddl-auto: create/update (Flyway only)
- [ ] Read ERD doc before designing schema
- [ ] tenant_id on every table
- [ ] Audit columns on every table
- [ ] UUID primary keys
- [ ] snake_case naming
- [ ] Proper indexes (tenant_id + status minimum)
- [ ] Migrations are backward-compatible
- [ ] Include outbox_events table if service publishes events (Blueprint 15)
