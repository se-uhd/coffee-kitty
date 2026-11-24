ALTER TABLE coffee_times ADD COLUMN price_cents int CHECK (price_cents >= 0);
