CREATE TABLE user_balance (
    user_id uuid NOT NULL PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    balance_cents bigint NOT NULL DEFAULT 0
);

CREATE TABLE kitty_balance (
    id int NOT NULL PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    balance_cents bigint NOT NULL DEFAULT 0 CHECK (balance_cents >= 0)
);
