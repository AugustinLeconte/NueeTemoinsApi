package com.nueetemoins.backend.card.dto;

import jakarta.validation.constraints.NotBlank;

public record ArtTranslationRequest(@NotBlank String locale, String title, String description) {}
