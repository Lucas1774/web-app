ALTER TABLE recommendation
  ADD COLUMN IF NOT EXISTS model VARCHAR(50) NOT NULL
    DEFAULT 'N/A';

ALTER TABLE recommendation
  ALTER COLUMN model DROP DEFAULT;
