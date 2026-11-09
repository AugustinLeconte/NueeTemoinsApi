package com.nueetemoins.backend.card.dto;

import com.nueetemoins.backend.card.CardFamily;
import com.nueetemoins.backend.card.CardRarity;

public record CardResponse(
    String publicId, CardFamily family, CardRarity rarity, String imageUrl,
    String locale, String title, String subtitle, String description,
    String countryCode, Integer feastMonth, Integer feastDay
) {}