CREATE TABLE artists (
    id        UUID PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    name      VARCHAR(255) NOT NULL,
    bio       TEXT,
    url       VARCHAR(255)
);

CREATE TABLE card_arts (
    id             UUID PRIMARY KEY,
    public_id      VARCHAR(255) NOT NULL UNIQUE,
    card_id        UUID NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    artist_id      UUID REFERENCES artists(id),
    rarity         VARCHAR(255) NOT NULL,
    source         VARCHAR(255) NOT NULL,
    image_url      VARCHAR(255) NOT NULL,
    drop_weight    INT NOT NULL DEFAULT 100,
    fragment_price INT,
    obtainable     BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_card_arts_card ON card_arts(card_id);
-- une seule illustration par défaut par saint
CREATE UNIQUE INDEX uq_card_arts_default ON card_arts(card_id) WHERE source = 'DEFAULT';

CREATE TABLE card_art_translations (
    id          UUID PRIMARY KEY,
    art_id      UUID NOT NULL REFERENCES card_arts(id) ON DELETE CASCADE,
    locale      VARCHAR(10) NOT NULL,
    title       VARCHAR(255),
    description TEXT,
    UNIQUE (art_id, locale)
);

-- Côté joueur (user_id = même type que profiles.user_id)
CREATE TABLE player_wallets (
    user_id          VARCHAR(128) PRIMARY KEY,
    fragments        INT NOT NULL DEFAULT 0,
    packs_since_rare INT NOT NULL DEFAULT 0,
    last_daily_pack  DATE
);

CREATE TABLE player_arts (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(128) NOT NULL,
    art_id      UUID NOT NULL REFERENCES card_arts(id),
    obtained_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, art_id)
);

CREATE TABLE player_cards (
    id              UUID PRIMARY KEY,
    user_id         VARCHAR(128) NOT NULL,
    card_id         UUID NOT NULL REFERENCES cards(id),
    level           INT NOT NULL DEFAULT 1,
    equipped_art_id UUID NOT NULL REFERENCES card_arts(id),
    UNIQUE (user_id, card_id)
);
CREATE INDEX idx_player_cards_user ON player_cards(user_id);

-- Migration des données existantes : l'ancienne image devient l'illustration par défaut
INSERT INTO card_arts (id, public_id, card_id, rarity, source, image_url)
SELECT gen_random_uuid(), substr(md5(random()::text || id::text), 1, 6), id, 'COMMON', 'DEFAULT', image_url
FROM cards WHERE image_url IS NOT NULL;

ALTER TABLE cards DROP COLUMN rarity;
ALTER TABLE cards DROP COLUMN image_url;