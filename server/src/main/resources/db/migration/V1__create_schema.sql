CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
  id       SERIAL       PRIMARY KEY,
  username VARCHAR(50)  NOT NULL UNIQUE,
  password VARCHAR(50)  NOT NULL
);

CREATE TABLE categories (
  id             SERIAL      PRIMARY KEY,
  name           VARCHAR(50) NOT NULL UNIQUE,
  category_order INTEGER     NOT NULL
);

CREATE TABLE products (
  id            SERIAL      PRIMARY KEY,
  name          VARCHAR(50) NOT NULL UNIQUE,
  is_rare       BOOLEAN     NOT NULL DEFAULT FALSE,
  category_id   INTEGER     REFERENCES categories(id),
  product_order INTEGER     NOT NULL
);

CREATE TABLE my_table (
  id        SERIAL       PRIMARY KEY,
  ans       VARCHAR(50),
  text      VARCHAR(255),
  text_mode BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE shopping (
  id         SERIAL    PRIMARY KEY,
  user_id    INTEGER   NOT NULL REFERENCES users(id),
  product_id INTEGER   NOT NULL REFERENCES products(id),
  quantity   INTEGER   NOT NULL,
  UNIQUE(user_id, product_id)
);

CREATE TABLE sudokus (
  id    SERIAL       PRIMARY KEY,
  state VARCHAR(255) NOT NULL
);

CREATE TABLE market_data (
  id               BIGSERIAL PRIMARY KEY,
  symbol           VARCHAR(10)     NOT NULL,
  open             NUMERIC(15,4),
  high             NUMERIC(15,4),
  low              NUMERIC(15,4),
  price            NUMERIC(15,4)   NOT NULL,
  volume           BIGINT,
  trade_date       DATE            NOT NULL,
  previous_close   NUMERIC(15,4),
  change           NUMERIC(15,4),
  change_percent   VARCHAR(8),
  CONSTRAINT uk_market_data_symbol_date UNIQUE(symbol, trade_date)
);

CREATE TABLE news (
  id               BIGSERIAL PRIMARY KEY,
  external_id      BIGINT         NOT NULL UNIQUE,
  symbol           VARCHAR(10)     NOT NULL,
  publication_date TIMESTAMP       NOT NULL,
  headline         VARCHAR(512)    NOT NULL,
  summary          TEXT,
  url              VARCHAR(512)    NOT NULL,
  source           VARCHAR(100),
  category         VARCHAR(50),
  image_url        VARCHAR(512),
  embedding        VECTOR(3072)
);

INSERT INTO users(username, password) VALUES
  ('admin',   'admin'),
  ('default', 'default')
ON CONFLICT DO NOTHING;

INSERT INTO my_table(id, ans, text) VALUES (1, NULL, NULL)
ON CONFLICT (id) DO NOTHING;
