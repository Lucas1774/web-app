CREATE TABLE IF NOT EXISTS news_symbol (
  news_id   INTEGER NOT NULL,
  symbol_id INTEGER NOT NULL,
  PRIMARY KEY (news_id, symbol_id),
  CONSTRAINT fk_news_symbol_news
    FOREIGN KEY (news_id)   REFERENCES news(id)    ON DELETE CASCADE,
  CONSTRAINT fk_news_symbol_symbol
    FOREIGN KEY (symbol_id) REFERENCES symbol(id)  ON DELETE CASCADE
);

INSERT INTO news_symbol (news_id, symbol_id)
SELECT id AS news_id, symbol_id
  FROM news
 WHERE symbol_id IS NOT NULL
ON CONFLICT (news_id, symbol_id) DO NOTHING;

ALTER TABLE news
  DROP CONSTRAINT IF EXISTS fk_news_symbol;

ALTER TABLE news
  DROP COLUMN IF EXISTS symbol_id;
