ALTER TABLE news
ADD COLUMN sentiment VARCHAR(8),
ADD COLUMN sentiment_confidence NUMERIC(15, 4);

ALTER TABLE market_data
ALTER COLUMN change_percent TYPE NUMERIC(15, 4);
