-- Database-backed owner-approved license price catalog. Apply once to D1 before deploying this Worker.
CREATE TABLE IF NOT EXISTS license_price_catalog (
  currency_code TEXT NOT NULL CHECK (currency_code IN ('SDG','SAR','AED','USD')),
  plan_code TEXT NOT NULL CHECK (plan_code IN ('monthly','annual','lifetime')),
  amount_minor INTEGER NOT NULL CHECK (amount_minor > 0),
  active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1)),
  updated_at TEXT NOT NULL,
  PRIMARY KEY(currency_code, plan_code)
);
INSERT OR IGNORE INTO license_price_catalog(currency_code,plan_code,amount_minor,active,updated_at) VALUES
 ('SDG','monthly',55000,1,'2026-11-09T00:00:00.000Z'),
 ('SDG','annual',550000,1,'2026-11-09T00:00:00.000Z'),
 ('SDG','lifetime',1050000,1,'2026-11-09T00:00:00.000Z'),
 ('SAR','monthly',29,1,'2026-11-09T00:00:00.000Z'),
 ('SAR','annual',290,1,'2026-11-09T00:00:00.000Z'),
 ('SAR','lifetime',725,1,'2026-11-09T00:00:00.000Z'),
 ('AED','monthly',29,1,'2026-11-09T00:00:00.000Z'),
 ('AED','annual',290,1,'2026-11-09T00:00:00.000Z'),
 ('AED','lifetime',725,1,'2026-11-09T00:00:00.000Z'),
 ('USD','monthly',8,1,'2026-11-09T00:00:00.000Z'),
 ('USD','annual',80,1,'2026-11-09T00:00:00.000Z'),
 ('USD','lifetime',199,1,'2026-11-09T00:00:00.000Z');
