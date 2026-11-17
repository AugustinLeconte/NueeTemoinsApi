-- Migration PostgreSQL (Flyway : renomme en V<n>__minigames_wordle.sql)
-- Pour MySQL/MariaDB : remplacer uuid par char(36)/binary(16) et "timestamp with time zone" par datetime(6).

CREATE TABLE minigame_results (
    id          uuid                     PRIMARY KEY,
    user_id     varchar(255)             NOT NULL,
    game        varchar(255)             NOT NULL,
    played_on   date                     NOT NULL,
    won         boolean                  NOT NULL,
    perfect     boolean                  NOT NULL,
    xp          integer                  NOT NULL,
    created_at  timestamp with time zone NOT NULL
);
CREATE INDEX idx_minigame_results_user ON minigame_results (user_id);

CREATE TABLE wordle_games (
    id          uuid                     PRIMARY KEY,
    version     bigint,
    user_id     varchar(255)             NOT NULL,
    game_date   date                     NOT NULL,
    lang        varchar(8)               NOT NULL,
    word_length integer                  NOT NULL DEFAULT 5,
    status      varchar(255)             NOT NULL,
    xp_awarded  integer                  NOT NULL,
    finished_at timestamp with time zone,
    CONSTRAINT uk_wordle_user_date UNIQUE (user_id, game_date)
);
-- Classement du jour : filtre date + langue, tri par XP puis heure de fin
CREATE INDEX idx_wordle_leaderboard ON wordle_games (game_date, lang, xp_awarded DESC, finished_at);

CREATE TABLE wordle_guesses (
    game_id uuid        NOT NULL REFERENCES wordle_games (id) ON DELETE CASCADE,
    idx     integer     NOT NULL,
    word    varchar(12),
    PRIMARY KEY (game_id, idx)
);