CREATE TABLE IF NOT EXISTS recommendation (
  id                   BIGSERIAL PRIMARY KEY,
  symbol_id            BIGINT    NOT NULL
                                 REFERENCES symbol(id)
                                 ON UPDATE CASCADE
                                 ON DELETE RESTRICT,
  action               VARCHAR(4)    NOT NULL,
  confidence           NUMERIC(15,4) NOT NULL,
  rationale            VARCHAR(512)  NOT NULL,
  recommendation_date  DATE          NOT NULL,
  CONSTRAINT uq_rec_symbol_date UNIQUE (symbol_id, recommendation_date)
);
