CREATE TABLE player_achievements (
    id          UUID                     PRIMARY KEY,
    user_id     VARCHAR(255)             NOT NULL,
    code        VARCHAR(255)             NOT NULL,
    unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_player_achievements_user_code UNIQUE (user_id, code)
);