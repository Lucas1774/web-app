INSERT INTO users(username, password) VALUES
  ('admin',   'admin'),
  ('default', 'default')
ON CONFLICT DO NOTHING;

INSERT INTO my_table(id, ans, text, text_mode) VALUES (1, 0, 'hello', false)
ON CONFLICT (id) DO NOTHING;
