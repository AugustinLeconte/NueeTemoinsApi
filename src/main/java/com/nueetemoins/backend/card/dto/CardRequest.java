package com.nueetemoins.backend.card.dto;

import java.util.List;

import com.nueetemoins.backend.card.CardFamily;
import com.nueetemoins.backend.card.CardRarity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CardRequest(
    @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") String publicId,
    @NotNull CardFamily family,
    @NotNull CardRarity rarity,
    String imageUrl,
    @NotEmpty @Valid List<TranslationRequest> translations,
    @Valid List<FeastDateRequest> feastDates
) {}

