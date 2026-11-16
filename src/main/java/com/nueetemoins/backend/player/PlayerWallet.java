package com.nueetemoins.backend.player;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_wallets")
@Getter @Setter @NoArgsConstructor
public class PlayerWallet {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private int coins;

    @Column(nullable = false)
    private int xp;

    @Column(name = "packs_since_rare", nullable = false)
    private int packsSinceRare;

    @Column(name = "last_daily_pack")
    private LocalDate lastDailyPack;

    @Column(name = "last_daily_coins")
    private LocalDate lastDailyCoins;

    @Column(name = "ad_rewards_date")
    private LocalDate adRewardsDate;

    @Column(name = "ad_rewards_count", nullable = false)
    private int adRewardsCount;

    @Column(name = "last_ad_reward_at")
    private Instant lastAdRewardAt;
}
