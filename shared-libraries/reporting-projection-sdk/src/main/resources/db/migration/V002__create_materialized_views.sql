-- Materialized view for loan portfolio overview
CREATE MATERIALIZED VIEW IF NOT EXISTS loan_portfolio_view AS
SELECT
    tenant_id,
    COUNT(*) as total_loans,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_loans,
    COUNT(CASE WHEN status = 'DISBURSED' THEN 1 END) as disbursed_loans,
    COUNT(CASE WHEN status = 'CLOSED' THEN 1 END) as closed_loans,
    COUNT(CASE WHEN status = 'WRITTEN_OFF' THEN 1 END) as written_off_loans,
    SUM(principal_amount) as total_principal,
    SUM(outstanding_principal) as total_outstanding,
    SUM(CASE WHEN days_overdue > 0 THEN outstanding_principal ELSE 0 END) as portfolio_at_risk,
    AVG(interest_rate) as avg_interest_rate,
    AVG(CASE WHEN days_overdue > 0 THEN days_overdue END) as avg_days_overdue,
    COUNT(CASE WHEN days_overdue > 30 THEN 1 END) as loans_over_30_days,
    COUNT(CASE WHEN days_overdue > 60 THEN 1 END) as loans_over_60_days,
    COUNT(CASE WHEN days_overdue > 90 THEN 1 END) as loans_over_90_days,
    MAX(updated_at) as last_update
FROM loan_summary_read_model
GROUP BY tenant_id;

CREATE UNIQUE INDEX idx_loan_portfolio_view_tenant ON loan_portfolio_view(tenant_id);

-- Materialized view for overdue loans with customer details
CREATE MATERIALIZED VIEW IF NOT EXISTS overdue_loans_view AS
SELECT
    l.tenant_id,
    l.loan_id,
    l.loan_account_number,
    l.customer_id,
    l.customer_name,
    l.national_id,
    l.outstanding_principal,
    l.days_overdue,
    l.overdue_amount,
    l.overdue_installments,
    l.last_payment_date,
    l.next_payment_date,
    l.collection_status,
    l.risk_category,
    l.branch_code,
    l.officer_name,
    c.total_loans as customer_total_loans,
    c.total_outstanding as customer_total_outstanding,
    c.risk_score as customer_risk_score
FROM loan_summary_read_model l
LEFT JOIN customer_portfolio_read_model c ON l.customer_id = c.customer_id
WHERE l.days_overdue > 0
ORDER BY l.days_overdue DESC, l.outstanding_principal DESC;

CREATE INDEX idx_overdue_loans_view_tenant ON overdue_loans_view(tenant_id);
CREATE INDEX idx_overdue_loans_view_days ON overdue_loans_view(days_overdue);
CREATE INDEX idx_overdue_loans_view_amount ON overdue_loans_view(overdue_amount);

-- Materialized view for monthly revenue analysis
CREATE MATERIALIZED VIEW IF NOT EXISTS revenue_view AS
SELECT
    tenant_id,
    DATE_TRUNC('month', payment_date) as revenue_month,
    COUNT(*) as payment_count,
    COUNT(DISTINCT customer_id) as unique_customers,
    SUM(payment_amount) as total_collected,
    SUM(principal_portion) as principal_collected,
    SUM(interest_portion) as interest_collected,
    SUM(profit_portion) as profit_collected, -- Islamic finance
    SUM(late_fee) as late_fees_collected,
    SUM(other_charges) as other_charges_collected,
    AVG(payment_amount) as avg_payment_amount,
    COUNT(CASE WHEN days_late <= 0 THEN 1 END) as on_time_payments,
    COUNT(CASE WHEN days_late > 0 THEN 1 END) as late_payments,
    AVG(CASE WHEN days_late > 0 THEN days_late END) as avg_days_late
FROM payment_history_read_model
WHERE status = 'COMPLETED'
GROUP BY tenant_id, DATE_TRUNC('month', payment_date);

CREATE INDEX idx_revenue_view_tenant ON revenue_view(tenant_id);
CREATE INDEX idx_revenue_view_month ON revenue_view(revenue_month);

-- Materialized view for customer segmentation
CREATE MATERIALIZED VIEW IF NOT EXISTS customer_segmentation_view AS
SELECT
    tenant_id,
    customer_segment,
    risk_category,
    COUNT(*) as customer_count,
    AVG(total_loans) as avg_loans_per_customer,
    AVG(active_loans) as avg_active_loans,
    SUM(total_outstanding) as segment_total_outstanding,
    AVG(total_outstanding) as avg_outstanding,
    SUM(overdue_amount) as segment_overdue_amount,
    AVG(payment_performance_score) as avg_performance_score,
    AVG(lifetime_value) as avg_lifetime_value,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY total_outstanding) as median_outstanding,
    PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY total_outstanding) as p90_outstanding
FROM customer_portfolio_read_model
GROUP BY tenant_id, customer_segment, risk_category;

CREATE INDEX idx_customer_segmentation_view_tenant ON customer_segmentation_view(tenant_id);
CREATE INDEX idx_customer_segmentation_view_segment ON customer_segmentation_view(customer_segment);

-- Materialized view for product performance
CREATE MATERIALIZED VIEW IF NOT EXISTS product_performance_view AS
SELECT
    tenant_id,
    product_type,
    COUNT(*) as loan_count,
    SUM(principal_amount) as total_disbursed,
    SUM(outstanding_principal) as total_outstanding,
    AVG(interest_rate) as avg_interest_rate,
    AVG(principal_amount) as avg_loan_size,
    COUNT(CASE WHEN days_overdue > 0 THEN 1 END) as overdue_loans,
    SUM(CASE WHEN days_overdue > 0 THEN outstanding_principal ELSE 0 END) as product_par,
    COUNT(CASE WHEN status = 'WRITTEN_OFF' THEN 1 END) as written_off_count,
    AVG(payment_performance_score) as avg_performance
FROM loan_summary_read_model
GROUP BY tenant_id, product_type;

CREATE INDEX idx_product_performance_view_tenant ON product_performance_view(tenant_id);
CREATE INDEX idx_product_performance_view_product ON product_performance_view(product_type);

-- Materialized view for collection efficiency
CREATE MATERIALIZED VIEW IF NOT EXISTS collection_efficiency_view AS
SELECT
    tenant_id,
    DATE_TRUNC('month', due_date) as collection_month,
    COUNT(*) as total_installments,
    SUM(scheduled_amount) as total_due,
    SUM(CASE WHEN payment_date IS NOT NULL THEN payment_amount ELSE 0 END) as total_collected,
    SUM(CASE WHEN payment_date <= due_date THEN payment_amount ELSE 0 END) as on_time_collection,
    SUM(CASE WHEN payment_date > due_date THEN payment_amount ELSE 0 END) as late_collection,
    COUNT(CASE WHEN payment_date IS NOT NULL THEN 1 END) as payments_received,
    COUNT(CASE WHEN payment_date <= due_date THEN 1 END) as on_time_count,
    COUNT(CASE WHEN payment_date > due_date THEN 1 END) as late_count,
    COUNT(CASE WHEN payment_date IS NULL THEN 1 END) as missed_payments,
    CASE
        WHEN SUM(scheduled_amount) > 0
        THEN (SUM(CASE WHEN payment_date IS NOT NULL THEN payment_amount ELSE 0 END) / SUM(scheduled_amount)) * 100
        ELSE 0
    END as collection_rate,
    CASE
        WHEN COUNT(*) > 0
        THEN (COUNT(CASE WHEN payment_date <= due_date THEN 1 END)::FLOAT / COUNT(*)) * 100
        ELSE 0
    END as on_time_rate
FROM payment_history_read_model
WHERE due_date IS NOT NULL
GROUP BY tenant_id, DATE_TRUNC('month', due_date);

CREATE INDEX idx_collection_efficiency_view_tenant ON collection_efficiency_view(tenant_id);
CREATE INDEX idx_collection_efficiency_view_month ON collection_efficiency_view(collection_month);

-- Function to refresh all materialized views
CREATE OR REPLACE FUNCTION refresh_all_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY loan_portfolio_view;
    REFRESH MATERIALIZED VIEW CONCURRENTLY overdue_loans_view;
    REFRESH MATERIALIZED VIEW CONCURRENTLY revenue_view;
    REFRESH MATERIALIZED VIEW CONCURRENTLY customer_segmentation_view;
    REFRESH MATERIALIZED VIEW CONCURRENTLY product_performance_view;
    REFRESH MATERIALIZED VIEW CONCURRENTLY collection_efficiency_view;
END;
$$ LANGUAGE plpgsql;

-- Create a scheduled job to refresh views (requires pg_cron extension)
-- This is commented out as pg_cron needs to be enabled separately
-- SELECT cron.schedule('refresh-materialized-views', '0 * * * *', 'SELECT refresh_all_materialized_views();');