package com.nueetemoins.backend.card;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByFamily(CardFamily family);
    List<Card> findByRarity(CardRarity rarity);
}