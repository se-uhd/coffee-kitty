CREATE TABLE payments (
    id uuid NOT NULL PRIMARY KEY,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    user_id uuid REFERENCES users(id),
    amount_cents int NOT NULL CHECK (amount_cents <> 0),
    note varchar(500) CHECK (note <> ''),
    CONSTRAINT ck_payments_deposit_positive CHECK (user_id IS NULL OR amount_cents > 0)
);

CREATE INDEX idx_payments_user_created ON payments (user_id, created_at);
