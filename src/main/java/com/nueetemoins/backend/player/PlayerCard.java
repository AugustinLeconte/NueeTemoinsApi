package com.nueetemoins.backend.player;

import java.util.UUID;

import com.nueetemoins.backend.card.Card;
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

/** Un saint possédé par un joueur : niveau + illustration affichée. */
@Entity
@Table(name = "player_cards",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"}))
@Getter @Setter @NoArgsConstructor
public class PlayerCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false)
    private int level = 1;

    /** Fragments de CE saint (obtenus via les doublons de ses illustrations). */
    @Column(nullable = false)
    private int fragments = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipped_art_id")
    private CardArt equippedArt;
}
