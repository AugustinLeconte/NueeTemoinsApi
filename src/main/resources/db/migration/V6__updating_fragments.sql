-- Les fragments deviennent propres à chaque saint
ALTER TABLE player_cards   ADD COLUMN fragments INT NOT NULL DEFAULT 0;
-- Le wallet ne contient plus que des pièces (l'ancien solde est conservé)
ALTER TABLE player_wallets RENAME COLUMN fragments TO coins;
-- Le prix en boutique est en pièces
ALTER TABLE card_arts      RENAME COLUMN fragment_price TO coin_price;

ALTER TABLE card_arts ALTER COLUMN image_url TYPE VARCHAR(500);