-- Run once on an existing D1 database that already has the licenses table.
-- If the database was created from the updated schema.sql from scratch, this migration is not needed.
ALTER TABLE licenses ADD COLUMN payment_verified_at TEXT;
ALTER TABLE licenses ADD COLUMN approved_by TEXT;
ALTER TABLE licenses ADD COLUMN device_hash TEXT;
ALTER TABLE licenses ADD COLUMN device_bound_at TEXT;
CREATE INDEX IF NOT EXISTS idx_licenses_device ON licenses(device_hash);
