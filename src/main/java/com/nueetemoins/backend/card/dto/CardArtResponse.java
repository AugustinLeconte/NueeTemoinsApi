package com.nueetemoins.backend.card.dto;

import com.nueetemoins.backend.card.ArtSource;
import com.nueetemoins.backend.card.CardRarity;

public record CardArtResponse(
    String publicId,
    String cardPublicId,
    String cardTitle,
    ArtistResponse artist,
    CardRarity rarity,
    ArtSource source,
    String imageUrl,
    Integer fragmentPrice,
    String title,
    String description
) {}
