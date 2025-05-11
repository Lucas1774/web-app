UPDATE sudokus
SET state = trim(both '"' from state)
WHERE state LIKE '"%' AND state LIKE '%"';

ALTER TABLE sudokus
  ALTER COLUMN state TYPE varchar(81),
  ADD CONSTRAINT sudoku_state_unique UNIQUE (state);
