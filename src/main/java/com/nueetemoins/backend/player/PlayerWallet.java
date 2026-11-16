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

    /** Pièces : gagnées dans les packs, dépensées en boutique. */
    @Column(nullable = false)
    private int coins;

    /** Expérience totale du joueur (le niveau en est déduit, cf. GameRules). */
    @Column(nullable = false)
    private int xp;

    /** Pity : nombre de packs consécutifs sans illustration RARE+. */
    @Column(name = "packs_since_rare", nullable = false)
    private int packsSinceRare;

    @Column(name = "last_daily_pack")
    private LocalDate lastDailyPack;
}
