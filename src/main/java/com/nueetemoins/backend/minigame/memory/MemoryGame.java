package com.nueetemoins.backend.minigame.memory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Partie du jour d'un joueur. Le plateau est celui du DailyChallenge ; on ne stocke que la progression. */
@Entity
@Table(name = "memory_games",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "played_on"}))
@Getter @Setter @NoArgsConstructor
public class MemoryGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Verrou optimiste : empêche deux requêtes concurrentes de jouer 2 coups. */
    @Version
    private Long version;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "played_on", nullable = false)
    private LocalDate playedOn;

    /** Index des cases déjà appariées. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_game_matched", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "tile_index")
    private Set<Integer> matched = new HashSet<>();

    /** Première carte retournée d'une paire en cours (null si aucune). */
    @Column(name = "pending_index")
    private Integer pendingIndex;

    /** 1 coup = 2 cartes retournées. */
    @Column(nullable = false)
    private int moves;

    @Column(name = "active_ms", nullable = false)
    private long activeMs;

    @Column(name = "last_action_at", nullable = false)
    private Instant lastActionAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    public boolean isFinished() { return finishedAt != null; }
}