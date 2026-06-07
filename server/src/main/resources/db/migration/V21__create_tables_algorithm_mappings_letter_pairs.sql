CREATE TABLE algorithm_mappings
(
    id               SERIAL PRIMARY KEY,
    first_sticker    INTEGER NOT NULL,
    second_sticker   INTEGER NOT NULL,
    edge_algorithm   VARCHAR(100),
    corner_algorithm VARCHAR(100),
    parity_algorithm VARCHAR(100),
    edge_type        VARCHAR(10),
    edge_technique   VARCHAR(10),
    corner_type      VARCHAR(10),
    corner_technique VARCHAR(10),
    parity_type      VARCHAR(10),
    parity_technique VARCHAR(10),
    CONSTRAINT uk_algorithm_mappings_first_second UNIQUE (first_sticker, second_sticker)
);

CREATE INDEX idx_algorithm_mappings_first_second
    ON algorithm_mappings (first_sticker, second_sticker);

CREATE TABLE letter_pairs
(
    id          SERIAL PRIMARY KEY,
    letter_pair VARCHAR(100) NOT NULL UNIQUE,
    person      VARCHAR(100),
    action      VARCHAR(100),
    object      VARCHAR(10)
);

CREATE INDEX idx_letter_pairs_letter_pair
    ON letter_pairs (letter_pair);
