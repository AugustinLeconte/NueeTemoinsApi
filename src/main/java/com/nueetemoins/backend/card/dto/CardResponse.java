package com.nueetemoins.backend.card.dto;

import java.util.UUID;

import com.nueetemoins.backend.card.Card;
import com.nueetemoins.backend.card.CardFamily;
import com.nueetemoins.backend.card.CardRarity;

public record CardResponse(
    UUID id, CardFamily family, CardRarity rarity,
    String title, String subtitle, String description, String imageUrl
) {
    public static CardResponse from(Card c) {
        return new CardResponse(c.getId(), c.getFamily(), c.getRarity(),
            c.getTitle(), c.getSubtitle(), c.getDescription(), c.getImageUrl());
    }
}