PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS license_orders (
  id TEXT PRIMARY KEY,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  customer_name TEXT NOT NULL,
  contact TEXT NOT NULL,
  device_id TEXT NOT NULL,
  plan TEXT NOT NULL CHECK (plan IN ('monthly','annual','lifetime')),
  amount_sdg INTEGER NOT NULL CHECK (amount_sdg > 0),
  receipt_reference TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('pending','approved','rejected')),
  reviewed_at TEXT,
  reviewer_note TEXT,
  activation_token TEXT,
  token_expires INTEGER,
  app_version TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_license_orders_status_created
  ON license_orders(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_license_orders_contact
  ON license_orders(contact);
CREATE INDEX IF NOT EXISTS idx_license_orders_device
  ON license_orders(device_id);

CREATE TABLE IF NOT EXISTS license_audit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id TEXT NOT NULL,
  event TEXT NOT NULL,
  actor TEXT NOT NULL,
  created_at TEXT NOT NULL,
  detail TEXT,
  FOREIGN KEY(order_id) REFERENCES license_orders(id)
);
