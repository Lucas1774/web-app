TRUNCATE TABLE recommendation_news, recommendation, portfolio, portfolio_mock, market_snapshot, news_symbol, news, market_data, symbol, shopping, products, categories, sudokus, my_table, users RESTART IDENTITY CASCADE;

INSERT INTO users(username, password) VALUES ('admin','admin'), ('default','default');
INSERT INTO my_table(id, ans, text, text_mode) VALUES (1, NULL, NULL, FALSE) ON CONFLICT (id) DO NOTHING;
