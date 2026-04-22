-- Convert installments.status (and referencing columns) from the native
-- `installment_status` enum to VARCHAR(30) so Hibernate can UPDATE without
-- explicit casts. Matches payments.status shape and prevents
-- "column status is of type installment_status but expression is of type character varying".

ALTER TABLE installments
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN status TYPE VARCHAR(30) USING status::text,
    ALTER COLUMN status SET DEFAULT 'SCHEDULED';

ALTER TABLE installment_status_history
    ALTER COLUMN from_status TYPE VARCHAR(30) USING from_status::text,
    ALTER COLUMN to_status TYPE VARCHAR(30) USING to_status::text;

DROP TYPE IF EXISTS installment_status;
