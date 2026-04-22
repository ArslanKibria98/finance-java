-- Add invoice_id denormalization to installments for faster lookup
-- Links installments directly to invoices (invoiceId format: INV-xxxxx)

ALTER TABLE installments
ADD COLUMN invoice_id VARCHAR(100);

-- Create index for fast lookup by invoice_id
CREATE INDEX idx_installments_invoice_id ON installments(tenant_id, invoice_id);

-- Update existing installments with invoice_id from repayment_schedules if needed
-- For new installments, invoice_id will be set when installment is created
