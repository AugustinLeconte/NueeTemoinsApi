package com.nueetemoins.backend.family.dto;

import jakarta.validation.constraints.NotBlank;

public record FamilyTranslationRequest(@NotBlank String locale, @NotBlank String name) {}