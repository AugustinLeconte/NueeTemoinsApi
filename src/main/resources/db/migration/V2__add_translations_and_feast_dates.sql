-- 1. Nouvelle colonne public_id, d'abord nullable pour pouvoir remplir les cartes existantes
ALTER TABLE cards ADD COLUMN public_id VARCHAR(255);

-- 2. Génère un public_id pour les cartes existantes (slug du titre + début d'id pour garantir l'unicité)
UPDATE cards
SET public_id = trim(both '-' from regexp_replace(lower(title), '[^a-z0-9]+', '-', 'g'))
                || '-' || substr(id::text, 1, 8);

ALTER TABLE cards ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE cards ADD CONSTRAINT uk_cards_public_id UNIQUE (public_id);

-- 3. Table des traductions
CREATE TABLE card_translations (
    id          UUID PRIMARY KEY,
    card_id     UUID         NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
    locale      VARCHAR(10)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    subtitle    VARCHAR(255),
    description TEXT,
    CONSTRAINT uk_card_translations UNIQUE (card_id, locale)
);

-- 4. Reprise des textes existants en français
INSERT INTO card_translations (id, card_id, locale, title, subtitle, description)
SELECT gen_random_uuid(), id, 'fr', title, subtitle, description
FROM cards;

-- 5. Les textes quittent la table cards
ALTER TABLE cards DROP COLUMN title;
ALTER TABLE cards DROP COLUMN subtitle;
ALTER TABLE cards DROP COLUMN description;

-- 6. Table des dates de fête par pays
CREATE TABLE card_feast_dates (
    id           UUID PRIMARY KEY,
    card_id      UUID    NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    month        INTEGER NOT NULL,
    day          INTEGER NOT NULL,
    CONSTRAINT uk_card_feast_dates UNIQUE (card_id, country_code)
);