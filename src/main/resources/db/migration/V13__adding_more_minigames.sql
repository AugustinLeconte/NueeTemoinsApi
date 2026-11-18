-- PostgreSQL. À adapter au format de ta migration (Flyway/Liquibase) si besoin.
-- Si tu es en spring.jpa.hibernate.ddl-auto=update, Hibernate crée les nouvelles tables et colonnes
-- tout seul ; seules les vérifications/contraintes de la section 1 sont à faire à la main.

-- 1) minigame_results : nouvelles colonnes + 1 résultat max / joueur / jeu / jour -------------
-- Vérifie d'abord qu'il n'y a aucun doublon (doit renvoyer 0 ligne) :
--   SELECT user_id, game, played_on, count(*) FROM minigame_results GROUP BY 1,2,3 HAVING count(*) > 1;
ALTER TABLE minigame_results ADD COLUMN IF NOT EXISTS duration_ms bigint;
ALTER TABLE minigame_results ADD COLUMN IF NOT EXISTS attempts integer;
ALTER TABLE minigame_results
  ADD CONSTRAINT uk_minigame_results_user_game_day UNIQUE (user_id, game, played_on);
CREATE INDEX IF NOT EXISTS idx_minigame_results_game_day ON minigame_results (game, played_on);

-- 2) Défi du jour (identique pour tous les joueurs) ------------------------------------------
CREATE TABLE daily_challenges (
  id             uuid PRIMARY KEY,
  game           varchar(255) NOT NULL,
  challenge_date date         NOT NULL,
  CONSTRAINT uk_daily_challenges_game_date UNIQUE (game, challenge_date)
);
CREATE TABLE daily_challenge_cards (
  challenge_id   uuid         NOT NULL REFERENCES daily_challenges (id) ON DELETE CASCADE,
  idx            integer      NOT NULL,
  card_public_id varchar(255) NOT NULL,
  PRIMARY KEY (challenge_id, idx)
);

-- 3) Devine la carte -------------------------------------------------------------------------
CREATE TABLE guess_card_games (
  id             uuid PRIMARY KEY,
  version        bigint,
  user_id        varchar(255) NOT NULL,
  played_on      date         NOT NULL,
  active_ms      bigint       NOT NULL DEFAULT 0,
  last_action_at timestamp(6) with time zone NOT NULL,
  finished_at    timestamp(6) with time zone,
  CONSTRAINT uk_guess_card_games_user_day UNIQUE (user_id, played_on)
);
CREATE TABLE guess_card_attempts (
  game_id        uuid         NOT NULL REFERENCES guess_card_games (id) ON DELETE CASCADE,
  idx            integer      NOT NULL,
  level          integer      NOT NULL,
  card_public_id varchar(255),
  correct        boolean      NOT NULL,
  PRIMARY KEY (game_id, idx)
);

-- 4) Memory ----------------------------------------------------------------------------------
CREATE TABLE memory_games (
  id             uuid PRIMARY KEY,
  version        bigint,
  user_id        varchar(255) NOT NULL,
  played_on      date         NOT NULL,
  pending_index  integer,
  moves          integer      NOT NULL DEFAULT 0,
  active_ms      bigint       NOT NULL DEFAULT 0,
  last_action_at timestamp(6) with time zone NOT NULL,
  finished_at    timestamp(6) with time zone,
  xp_awarded     integer      NOT NULL DEFAULT 0,
  CONSTRAINT uk_memory_games_user_day UNIQUE (user_id, played_on)
);
CREATE TABLE memory_game_matched (
  game_id    uuid    NOT NULL REFERENCES memory_games (id) ON DELETE CASCADE,
  tile_index integer NOT NULL,
  PRIMARY KEY (game_id, tile_index)
);