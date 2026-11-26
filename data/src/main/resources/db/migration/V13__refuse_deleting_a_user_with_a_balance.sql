ALTER TABLE user_balance DROP CONSTRAINT user_balance_user_id_fkey;
ALTER TABLE user_balance ADD CONSTRAINT user_balance_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
