CREATE TABLE IF NOT EXISTS portfolio (
  id                 BIGSERIAL     PRIMARY KEY,
  symbol_id          BIGINT        NOT NULL
                         REFERENCES symbol(id)
                         ON UPDATE CASCADE ON DELETE RESTRICT,
  quantity           NUMERIC(15,4) NOT NULL DEFAULT 0,
  average_cost           NUMERIC(15,4) NOT NULL DEFAULT 0,
  effective_timestamp TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS portfolio_mock (
  id                 BIGSERIAL     PRIMARY KEY,
  symbol_id          BIGINT        NOT NULL
                         REFERENCES symbol(id)
                         ON UPDATE CASCADE ON DELETE RESTRICT,
  quantity           NUMERIC(15,4) NOT NULL DEFAULT 0,
  average_cost           NUMERIC(15,4) NOT NULL DEFAULT 0,
  effective_timestamp TIMESTAMP    NOT NULL DEFAULT NOW()
);
