-- Add installment_id and invoice_id tracking to payments table
-- Required for proper payment-to-installment linkage and invoice audit trail

-- Add columns
ALTER TABLE payments
ADD COLUMN installment_id UUID REFERENCES installments(id),
ADD COLUMN invoice_id VARCHAR(100);

-- Create unique constraint for invoice_id per tenant
ALTER TABLE payments
ADD CONSTRAINT uk_payments_tenant_invoice_id UNIQUE (tenant_id, invoice_id);

-- Create index for faster lookups
CREATE INDEX idx_payments_installment ON payments(tenant_id, installment_id);
CREATE INDEX idx_payments_invoice_id ON payments(tenant_id, invoice_id);

-- Update NOT NULL constraints (existing payments can have NULL values)
-- For future payments, these will be required at the application level via validation
