ALTER TABLE market_data
    ADD COLUMN average_gain NUMERIC(15, 4),
    ADD COLUMN average_loss NUMERIC(15, 4),
    ADD COLUMN atr NUMERIC(15, 4),
    ADD COLUMN previous_average_gain NUMERIC(15, 4),
    ADD COLUMN previous_average_loss NUMERIC(15, 4),
    ADD COLUMN previous_atr NUMERIC(15, 4);
