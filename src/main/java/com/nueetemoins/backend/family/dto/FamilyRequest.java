package com.nueetemoins.backend.family.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record FamilyRequest(
    @NotEmpty @Valid List<FamilyTranslationRequest> translations
) {}