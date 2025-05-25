ALTER TABLE portfolio
  ADD COLUMN IF NOT EXISTS average_commission NUMERIC(15,4) NOT NULL
    DEFAULT 0;

ALTER TABLE portfolio_mock
  ADD COLUMN IF NOT EXISTS average_commission NUMERIC(15,4) NOT NULL
    DEFAULT 0;

ALTER TABLE portfolio
  ALTER COLUMN average_commission DROP DEFAULT;

ALTER TABLE portfolio_mock
  ALTER COLUMN average_commission DROP DEFAULT;
