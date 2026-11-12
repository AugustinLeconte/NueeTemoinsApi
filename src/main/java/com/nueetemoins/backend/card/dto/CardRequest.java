package com.nueetemoins.backend.card.dto;

import java.util.List;

import com.nueetemoins.backend.card.CardRarity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CardRequest(
    @NotEmpty List<String> families,
    @NotNull CardRarity rarity,
    String imageUrl,
    @NotEmpty @Valid List<TranslationRequest> translations,
    @Valid List<FeastDateRequest> feastDates
) {}

