-- PostgreSQL : chrono du Wordle
ALTER TABLE wordle_games ADD COLUMN IF NOT EXISTS active_ms bigint NOT NULL DEFAULT 0;
ALTER TABLE wordle_games ADD COLUMN IF NOT EXISTS last_action_at timestamp(6) with time zone;
UPDATE wordle_games SET last_action_at = COALESCE(finished_at, now()) WHERE last_action_at IS NULL;
ALTER TABLE wordle_games ALTER COLUMN last_action_at SET NOT NULL;

-- Les mots peuvent faire 5, 6 ou 7 lettres : vérifie que cette colonne n'est pas en varchar(5)
--   \d wordle_guesses
-- ALTER TABLE wordle_guesses ALTER COLUMN word TYPE varchar(7);