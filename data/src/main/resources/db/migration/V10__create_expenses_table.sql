CREATE TABLE expenses (
    id uuid NOT NULL PRIMARY KEY,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    buyer_user_id uuid NOT NULL REFERENCES users(id),
    expense_type varchar(16) NOT NULL CHECK (expense_type IN ('BEANS', 'OTHER')),
    bean_id uuid REFERENCES coffee_beans(id),
    weight_grams int CHECK (weight_grams > 0),
    amount_cents int NOT NULL CHECK (amount_cents > 0),
    private_amount_cents int NOT NULL CHECK (private_amount_cents >= 0),
    kitty_amount_cents int NOT NULL CHECK (kitty_amount_cents >= 0),
    note varchar(500) CHECK (note <> ''),
    CONSTRAINT ck_expenses_split CHECK (private_amount_cents + kitty_amount_cents = amount_cents),
    CONSTRAINT ck_expenses_type CHECK (
        (expense_type = 'BEANS' AND bean_id IS NOT NULL AND weight_grams IS NOT NULL)
            OR (expense_type = 'OTHER' AND bean_id IS NULL AND weight_grams IS NULL)
    )
);

CREATE INDEX idx_expenses_buyer_created ON expenses (buyer_user_id, created_at);
CREATE INDEX idx_expenses_bean ON expenses (bean_id);
