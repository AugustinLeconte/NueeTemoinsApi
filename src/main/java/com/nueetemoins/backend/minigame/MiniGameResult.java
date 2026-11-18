package com.nueetemoins.backend.minigame;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Résultat générique d'une partie terminée, quel que soit le mini-jeu.
 * C'est CETTE table qui sert aux succès ET aux classements du jour.
 * Nouveautés : durationMs + attempts (pour départager / afficher) et une partie max par jour/jeu/joueur.
 */
@Entity
@Table(name = "minigame_results",
    indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "game, played_on")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game", "played_on"}))
@Getter @Setter @NoArgsConstructor
public class MiniGameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MiniGameType game;

    @Column(name = "played_on", nullable = false)
    private LocalDate playedOn;

    @Column(nullable = false)
    private boolean won;

    /** Meilleur résultat possible du jeu (ex. Wordle du premier coup, Memory sans erreur). */
    @Column(nullable = false)
    private boolean perfect;

    @Column(nullable = false)
    private int xp;

    /** Temps de jeu "actif" en ms (null pour les anciens résultats Wordle). */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** Essais / coups utilisés (sens propre à chaque jeu). */
    private Integer attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}