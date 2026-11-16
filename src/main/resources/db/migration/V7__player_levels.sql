-- Expérience du joueur (le niveau est calculé à partir de l'XP total)
ALTER TABLE player_wallets ADD COLUMN xp INT NOT NULL DEFAULT 0;

-- Packs de niveau déjà ouverts depuis la boutique (un par niveau et par joueur)
CREATE TABLE player_pack_claims (
    id         UUID PRIMARY KEY,
    user_id    VARCHAR(128) NOT NULL,
    level      INT NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, level)
);
