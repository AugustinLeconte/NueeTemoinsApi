CREATE TABLE cards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family      VARCHAR(50)  NOT NULL,
    rarity      VARCHAR(50)  NOT NULL,
    title       VARCHAR(150) NOT NULL,
    subtitle    VARCHAR(255),
    description TEXT,
    image_url   VARCHAR(500)
);

CREATE INDEX idx_cards_family ON cards(family);
CREATE INDEX idx_cards_rarity ON cards(rarity);