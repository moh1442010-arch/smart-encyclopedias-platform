CREATE TABLE IF NOT EXISTS tirbal_devices (
  device_id TEXT PRIMARY KEY,
  first_seen INTEGER NOT NULL,
  trial_expires INTEGER NOT NULL,
  plan TEXT,
  license_expires INTEGER,
  activation_token TEXT,
  updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS tirbal_orders (
  id TEXT PRIMARY KEY,
  created_at INTEGER NOT NULL,
  customer_name TEXT NOT NULL,
  contact TEXT NOT NULL,
  device_id TEXT NOT NULL,
  plan TEXT NOT NULL CHECK(plan IN ('monthly','annual','lifetime')),
  amount_sdg INTEGER NOT NULL CHECK(amount_sdg > 0),
  receipt_reference TEXT NOT NULL,
  status TEXT NOT NULL CHECK(status IN ('pending','approved','rejected')),
  reviewed_at INTEGER,
  reviewer_note TEXT,
  activation_token TEXT,
  token_expires INTEGER,
  app_version TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tirbal_receipt_active
ON tirbal_orders(receipt_reference) WHERE status IN ('pending','approved');
CREATE INDEX IF NOT EXISTS idx_tirbal_orders_status
ON tirbal_orders(status,created_at DESC);
CREATE TABLE IF NOT EXISTS tirbal_audit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id TEXT NOT NULL,
  event TEXT NOT NULL,
  actor TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  detail TEXT
);
\nCREATE TABLE IF NOT EXISTS tirbal_admin_attempts (\n  ip_hash TEXT PRIMARY KEY,\n  window_start INTEGER NOT NULL,\n  attempts INTEGER NOT NULL\n);\n
CREATE TABLE IF NOT EXISTS tirbal_order_attempts (
  ip_hash TEXT PRIMARY KEY,
  window_start INTEGER NOT NULL,
  attempts INTEGER NOT NULL
);
