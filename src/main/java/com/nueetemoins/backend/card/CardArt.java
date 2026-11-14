package com.nueetemoins.backend.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "card_arts")
@Getter @Setter @NoArgsConstructor
public class CardArt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardRarity rarity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtSource source = ArtSource.PACK;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /** Poids dans le tirage au sein d'une même rareté. */
    @Column(name = "drop_weight", nullable = false)
    private int dropWeight = 100;

    /** Prix en fragments (uniquement pour source = SHOP). */
    @Column(name = "fragment_price")
    private Integer fragmentPrice;

    /** Permet de retirer une illustration des packs/boutique sans la supprimer. */
    @Column(nullable = false)
    private boolean obtainable = true;

    @OneToMany(mappedBy = "art", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<CardArtTranslation> translations = new ArrayList<>();

    public boolean isDefault() { return source == ArtSource.DEFAULT; }

    public Optional<CardArtTranslation> findTranslation(String locale) {
        return translations.stream().filter(t -> t.getLocale().equals(locale)).findFirst();
    }
}
