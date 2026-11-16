ALTER TABLE player_wallets
  ADD COLUMN last_daily_coins DATE,
  ADD COLUMN ad_rewards_date DATE,
  ADD COLUMN ad_rewards_count INT NOT NULL DEFAULT 0,
  ADD COLUMN last_ad_reward_at TIMESTAMP;