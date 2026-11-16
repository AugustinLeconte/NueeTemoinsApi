package com.nueetemoins.backend.player;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pack de niveau déjà ouvert par un joueur. */
@Entity
@Table(name = "player_pack_claims",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "level"}))
@Getter @Setter @NoArgsConstructor
public class PlayerPackClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private int level;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt = Instant.now();
}
