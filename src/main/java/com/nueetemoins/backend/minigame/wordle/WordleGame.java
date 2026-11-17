package com.nueetemoins.backend.minigame.wordle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Partie du jour d'un joueur (une seule par joueur et par jour). */
@Entity
@Table(name = "wordle_games",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_date"}))
@Getter @Setter @NoArgsConstructor
public class WordleGame {

    public enum Status { IN_PROGRESS, WON, LOST }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Verrou optimiste : empêche deux requêtes concurrentes de jouer 2 coups. */
    @Version
    private Long version;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    /** Langue figée à la 1re action du jour => même mot toute la journée. */
    @Column(nullable = false, length = 8)
    private String lang;

    /** Longueur du mot du jour (5, 6 ou 7), figée à la création de la partie. */
    @Column(name = "word_length", nullable = false)
    private int wordLength = 5;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wordle_guesses", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn(name = "idx")
    @Column(name = "word", length = 5)
    private List<String> guesses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "finished_at")
    private Instant finishedAt;
}