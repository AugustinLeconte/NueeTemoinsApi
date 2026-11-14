package com.nueetemoins.backend.card.dto;

import java.util.List;

import com.nueetemoins.backend.card.ArtSource;
import com.nueetemoins.backend.card.CardRarity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** dropWeight (défaut 100) et obtainable (défaut true) sont optionnels. */
public record CardArtRequest(
    String artistPublicId,
    @NotNull CardRarity rarity,
    @NotNull ArtSource source,
    @NotBlank String imageUrl,
    @Min(0) Integer dropWeight,
    @Min(1) Integer fragmentPrice,
    Boolean obtainable,
    @Valid List<ArtTranslationRequest> translations
) {}
