ALTER TABLE market_data
  ALTER COLUMN change_percent TYPE numeric(6,4)
    USING (
        regexp_replace(
          change_percent,
          '%$',
          ''
        )::numeric
    );
