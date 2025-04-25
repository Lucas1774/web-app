CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS users (
  id       SERIAL       PRIMARY KEY,
  username VARCHAR(50)  NOT NULL UNIQUE,
  password VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
  id             SERIAL      PRIMARY KEY,
  name           VARCHAR(50) NOT NULL UNIQUE,
  category_order INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
  id            SERIAL      PRIMARY KEY,
  name          VARCHAR(50) NOT NULL UNIQUE,
  is_rare       BOOLEAN     NOT NULL DEFAULT FALSE,
  category_id   INTEGER     REFERENCES categories(id),
  product_order INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS my_table (
  id        SERIAL       PRIMARY KEY,
  ans       VARCHAR(50),
  text      VARCHAR(255),
  text_mode BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS shopping (
  id         SERIAL    PRIMARY KEY,
  user_id    INTEGER   NOT NULL REFERENCES users(id),
  product_id INTEGER   NOT NULL REFERENCES products(id),
  quantity   INTEGER   NOT NULL,
  UNIQUE(user_id, product_id)
);

CREATE TABLE IF NOT EXISTS sudokus (
  id    SERIAL       PRIMARY KEY,
  state VARCHAR(255) NOT NULL
);

INSERT INTO users(username, password) VALUES
  ('admin',   'admin'),
  ('default', 'default')
ON CONFLICT DO NOTHING;

INSERT INTO my_table(id, ans, text) VALUES (1, NULL, NULL)
ON CONFLICT (id) DO NOTHING;
