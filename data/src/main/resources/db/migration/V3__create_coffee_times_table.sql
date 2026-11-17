CREATE TABLE coffee_times (
    consumption_id uuid NOT NULL REFERENCES coffee_consumptions(id) ON DELETE CASCADE,
    position int NOT NULL CHECK (position >= 0),
    added_at timestamp NOT NULL,
    PRIMARY KEY (consumption_id, position)
);
