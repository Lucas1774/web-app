ALTER TABLE recommendation
  ADD COLUMN IF NOT EXISTS market_data_id BIGINT;

UPDATE recommendation r
SET market_data_id = md.id
FROM market_data md
WHERE md.symbol_id = r.symbol_id
  AND md.trade_date = r.recommendation_date
  AND r.market_data_id IS NULL;

ALTER TABLE recommendation
  ADD CONSTRAINT fk_recommendation_market_data
    FOREIGN KEY (market_data_id)
    REFERENCES market_data(id)
    ON DELETE SET NULL;
