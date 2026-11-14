package com.nueetemoins.backend.player;

import java.time.Instant;
import java.util.UUID;

import com.nueetemoins.backend.card.CardArt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une illustration possédée par un joueur. */
@Entity
@Table(name = "player_arts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "art_id"}))
@Getter @Setter @NoArgsConstructor
public class PlayerArt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "art_id")
    private CardArt art;

    @Column(name = "obtained_at", nullable = false)
    private Instant obtainedAt = Instant.now();
}
