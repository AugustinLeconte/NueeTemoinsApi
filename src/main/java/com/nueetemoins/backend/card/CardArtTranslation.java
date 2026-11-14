package com.nueetemoins.backend.card;

import java.util.UUID;

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

@Entity
@Table(name = "card_art_translations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"art_id", "locale"}))
@Getter @Setter @NoArgsConstructor
public class CardArtTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "art_id")
    private CardArt art;

    @Column(nullable = false, length = 10)
    private String locale;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
}
