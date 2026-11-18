package com.nueetemoins.backend.minigame.guesscard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Partie du jour d'un joueur (3 niveaux). L'état de chaque niveau se déduit des tentatives. */
@Entity
@Table(name = "guess_card_games",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "played_on"}))
@Getter @Setter @NoArgsConstructor
public class GuessCardGame {

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "guess_card_attempts", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn(name = "idx")
    private List<Attempt> attempts = new ArrayList<>();

    /** Temps de jeu actif cumulé (les pauses longues sont plafonnées, cf. service). */
    @Column(name = "active_ms", nullable = false)
    private long activeMs;

    @Column(name = "last_action_at", nullable = false)
    private Instant lastActionAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Embeddable
    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class Attempt {
        private int level;
        @Column(name = "card_public_id")
        private String cardPublicId;
        private boolean correct;
    }
}