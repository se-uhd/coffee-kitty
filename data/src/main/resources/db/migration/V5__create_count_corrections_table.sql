CREATE TABLE count_corrections (
    id uuid NOT NULL PRIMARY KEY,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    admin_id uuid NOT NULL REFERENCES users(id),
    count_before int NOT NULL CHECK (count_before >= 0),
    count_after int NOT NULL CHECK (count_after >= 0),
    note varchar(500) NOT NULL CHECK (note <> '')
);

CREATE INDEX idx_count_corrections_user_created ON count_corrections (user_id, created_at);
CREATE INDEX idx_count_corrections_admin ON count_corrections (admin_id);
