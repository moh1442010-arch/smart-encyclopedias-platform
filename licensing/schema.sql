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

CREATE TABLE IF NOT EXISTS license_order_rate_limits (
  ip_hash TEXT PRIMARY KEY,
  window_start INTEGER NOT NULL,
  attempts INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS license_audit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id TEXT NOT NULL,
  event TEXT NOT NULL,
  actor TEXT NOT NULL,
  created_at TEXT NOT NULL,
  detail TEXT,
  FOREIGN KEY(order_id) REFERENCES license_orders(id)
);

CREATE TABLE IF NOT EXISTS admin_activity (
  id TEXT PRIMARY KEY,
  event TEXT NOT NULL,
  actor TEXT NOT NULL,
  created_at TEXT NOT NULL,
  detail TEXT
);

CREATE INDEX IF NOT EXISTS idx_admin_activity_created
  ON admin_activity(created_at DESC);

CREATE TABLE IF NOT EXISTS review_submissions (
  id TEXT PRIMARY KEY,
  created_at TEXT NOT NULL,
  reviewed_at TEXT,
  product TEXT NOT NULL,
  name TEXT NOT NULL,
  role TEXT,
  platform TEXT NOT NULL,
  version TEXT,
  device TEXT,
  os TEXT,
  test_area TEXT NOT NULL,
  result TEXT NOT NULL,
  severity TEXT,
  steps TEXT,
  rating TEXT NOT NULL,
  review_text TEXT NOT NULL,
  public_consent INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected'))
);

CREATE INDEX IF NOT EXISTS idx_review_submissions_status_created
  ON review_submissions(status, created_at DESC);

CREATE TABLE IF NOT EXISTS review_rate_limits (
  ip_hash TEXT PRIMARY KEY,
  window_start INTEGER NOT NULL,
  attempts INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS analytics_daily (
  visit_date TEXT NOT NULL,
  page_path TEXT NOT NULL,
  views INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (visit_date, page_path)
);
