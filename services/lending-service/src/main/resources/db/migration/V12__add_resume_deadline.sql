-- BRD V1.8 Step 71: 30-day comeback rule
-- If user doesn't sign contract in 24 hours, application expires but user can resume within 30 days

ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS resume_deadline DATE;

-- Index for finding resumable applications
CREATE INDEX IF NOT EXISTS idx_loan_app_resumable
    ON loan_applications(tenant_id, customer_id, status)
    WHERE status = 'EXPIRED_RESUMABLE';
