package com.nueetemoins.backend.minigame;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Le défi du jour, identique pour tous les joueurs (donc classement équitable).
 * Figé à la première demande de la journée : ajouter une carte en cours de journée ne change rien.
 *  - GUESS_CARD : les 3 cartes à deviner (niveau 1, 2, 3)
 *  - MEMORY     : le plateau complet (chaque carte 2 fois, déjà mélangé)
 */
@Entity
@Table(name = "daily_challenges",
    uniqueConstraints = @UniqueConstraint(columnNames = {"game", "challenge_date"}))
@Getter @Setter @NoArgsConstructor
public class DailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MiniGameType game;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate day;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_challenge_cards", joinColumns = @JoinColumn(name = "challenge_id"))
    @OrderColumn(name = "idx")
    @Column(name = "card_public_id", nullable = false)
    private List<String> cardPublicIds = new ArrayList<>();
}