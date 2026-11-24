CREATE TABLE coffee_beans (
    id uuid NOT NULL PRIMARY KEY,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    name varchar(200) NOT NULL CHECK (name <> ''),
    active boolean NOT NULL DEFAULT true,
    merged_into_id uuid REFERENCES coffee_beans(id),
    CONSTRAINT ck_coffee_beans_active CHECK (active = (merged_into_id IS NULL)),
    CONSTRAINT ck_coffee_beans_merged_into_other CHECK (merged_into_id <> id)
);

CREATE UNIQUE INDEX uq_coffee_beans_active_name ON coffee_beans (lower(name)) WHERE merged_into_id IS NULL;
CREATE INDEX idx_coffee_beans_merged_into ON coffee_beans (merged_into_id);
