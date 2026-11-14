package com.nueetemoins.backend.card;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

import com.nueetemoins.backend.family.Family;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards")
@Getter @Setter @NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId;

    @ManyToMany
    @JoinTable(name = "card_families",
        joinColumns = @JoinColumn(name = "card_id"),
        inverseJoinColumns = @JoinColumn(name = "family_id"))
    @BatchSize(size = 50)
    private Set<Family> families = new HashSet<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<CardTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<CardFeastDate> feastDates = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<CardArt> arts = new ArrayList<>();

    public Optional<CardTranslation> findTranslation(String locale) {
        return translations.stream().filter(t -> t.getLocale().equals(locale)).findFirst();
    }

    public Optional<CardFeastDate> findFeastDate(String countryCode) {
        return feastDates.stream().filter(f -> f.getCountryCode().equals(countryCode)).findFirst();
    }

    public Optional<CardArt> findDefaultArt() {
        return arts.stream().filter(CardArt::isDefault).findFirst();
    }
}
