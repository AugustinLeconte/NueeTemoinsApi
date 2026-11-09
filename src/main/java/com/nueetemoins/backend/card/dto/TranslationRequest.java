package com.nueetemoins.backend.card.dto;

import jakarta.validation.constraints.NotBlank;

public record TranslationRequest(
    @NotBlank String locale,
    @NotBlank String title,
    String subtitle,
    String description
) {}