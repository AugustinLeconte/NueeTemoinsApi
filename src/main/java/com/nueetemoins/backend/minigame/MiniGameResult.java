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
 * C'est CETTE table qui sert aux succès (pas les tables propres à chaque jeu).
 */
@Entity
@Table(name = "minigame_results", indexes = @Index(columnList = "user_id"))
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

    /** Meilleur résultat possible du jeu (ex. Wordle trouvé du premier coup). */
    @Column(nullable = false)
    private boolean perfect;

    @Column(nullable = false)
    private int xp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}