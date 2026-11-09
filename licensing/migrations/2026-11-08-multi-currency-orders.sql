-- Apply once to the existing Al-Mizan licensing D1 database before deploying the multi-currency Worker.
-- Existing orders remain SDG; no historical amount is converted or relabelled.
ALTER TABLE license_orders ADD COLUMN currency_code TEXT NOT NULL DEFAULT 'SDG';
ALTER TABLE license_orders ADD COLUMN amount_minor INTEGER NOT NULL DEFAULT 0;
UPDATE license_orders SET amount_minor = amount_sdg WHERE amount_minor = 0;
CREATE INDEX IF NOT EXISTS idx_license_orders_currency_created ON license_orders(currency_code, created_at DESC);
