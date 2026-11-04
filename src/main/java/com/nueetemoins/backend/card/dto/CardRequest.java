package com.nueetemoins.backend.card.dto;

import com.nueetemoins.backend.card.CardFamily;
import com.nueetemoins.backend.card.CardRarity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardRequest(
    @NotNull CardFamily family,
    @NotNull CardRarity rarity,
    @NotBlank String title,
    String subtitle,
    String description,
    String imageUrl
) {}

