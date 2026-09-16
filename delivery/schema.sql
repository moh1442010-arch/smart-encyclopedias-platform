-- Secure delivery layer. Run once against the existing smart-encyclopedias D1 database.
CREATE TABLE IF NOT EXISTS licenses (
  id TEXT PRIMARY KEY,
  order_id TEXT,
  customer_name TEXT NOT NULL,
  customer_email TEXT NOT NULL,
  object_key TEXT NOT NULL,
  token_hash TEXT NOT NULL UNIQUE,
  status TEXT NOT NULL DEFAULT 'active',
  expires_at TEXT NOT NULL,
  max_downloads INTEGER NOT NULL DEFAULT 3,
  download_count INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_download_at TEXT,
  payment_verified_at TEXT,
  approved_by TEXT,
  device_hash TEXT,
  device_bound_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_licenses_token_hash ON licenses(token_hash);
CREATE INDEX IF NOT EXISTS idx_licenses_status_expires ON licenses(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_licenses_order ON licenses(order_id);
CREATE INDEX IF NOT EXISTS idx_licenses_device ON licenses(device_hash);
