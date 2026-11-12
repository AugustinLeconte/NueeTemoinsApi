CREATE TABLE families (
    id        UUID PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE family_translations (
    id        UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    locale    VARCHAR(255) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    UNIQUE (family_id, locale)
);

CREATE TABLE card_families (
    card_id   UUID NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    family_id UUID NOT NULL REFERENCES families(id),
    PRIMARY KEY (card_id, family_id)
);

-- Reprise des 4 familles existantes
INSERT INTO families (id, public_id) VALUES
  (gen_random_uuid(), 'fire'), (gen_random_uuid(), 'water'),
  (gen_random_uuid(), 'earth'), (gen_random_uuid(), 'air');

INSERT INTO family_translations (id, family_id, locale, name)
SELECT gen_random_uuid(), id, 'fr',
  CASE public_id WHEN 'fire' THEN 'Feu' WHEN 'water' THEN 'Eau'
                 WHEN 'earth' THEN 'Terre' WHEN 'air' THEN 'Air' END
FROM families;

-- Reprise de la famille de chaque carte
INSERT INTO card_families (card_id, family_id)
SELECT c.id, f.id FROM cards c JOIN families f ON f.public_id = LOWER(c.family);

ALTER TABLE cards DROP COLUMN family;