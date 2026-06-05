-- Display currency per customer, synced from customer-service profile events.
-- Used to stamp the correct currency (e.g. CAD for Canada/Foreign flows) onto outbound
-- notification payloads that carry monetary amounts.
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3);
