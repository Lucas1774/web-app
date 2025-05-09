CREATE TABLE IF NOT EXISTS symbol (
  id   SERIAL PRIMARY KEY,
  name VARCHAR(10) NOT NULL UNIQUE
);

INSERT INTO symbol(name)
  SELECT DISTINCT symbol FROM news
  UNION
  SELECT DISTINCT symbol FROM market_data
ON CONFLICT (name) DO NOTHING;

ALTER TABLE news
  ADD COLUMN symbol_id INTEGER;
ALTER TABLE market_data
  ADD COLUMN symbol_id INTEGER;

UPDATE news n
  SET symbol_id = s.id
  FROM symbol s
  WHERE n.symbol = s.name;

UPDATE market_data m
  SET symbol_id = s.id
  FROM symbol s
  WHERE m.symbol = s.name;

ALTER TABLE news
  ALTER COLUMN symbol_id SET NOT NULL;
ALTER TABLE market_data
  ALTER COLUMN symbol_id SET NOT NULL;

ALTER TABLE news
  ADD CONSTRAINT fk_news_symbol FOREIGN KEY(symbol_id) REFERENCES symbol(id);

ALTER TABLE market_data
  ADD CONSTRAINT fk_market_data_symbol FOREIGN KEY(symbol_id) REFERENCES symbol(id);

ALTER TABLE market_data
  DROP CONSTRAINT IF EXISTS uk_market_data_symbol_date;
ALTER TABLE market_data
  ADD CONSTRAINT uk_market_data_symbolid_date UNIQUE(symbol_id, trade_date);

ALTER TABLE news
  DROP COLUMN symbol;

ALTER TABLE market_data
  DROP COLUMN symbol;

COMMIT;
