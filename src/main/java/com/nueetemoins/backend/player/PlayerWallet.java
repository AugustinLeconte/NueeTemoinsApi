package com.nueetemoins.backend.player;

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

    private int fragments;

    /** Pity : nombre de packs consécutifs sans illustration RARE+. */
    @Column(name = "packs_since_rare", nullable = false)
    private int packsSinceRare;

    @Column(name = "last_daily_pack")
    private LocalDate lastDailyPack;
}
