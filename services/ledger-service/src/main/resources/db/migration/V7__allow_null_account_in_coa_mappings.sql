-- Step-1 field selection and step-2 account assignment require nullable account_id.
ALTER TABLE coa_configuration_mappings
    ALTER COLUMN account_id DROP NOT NULL;
